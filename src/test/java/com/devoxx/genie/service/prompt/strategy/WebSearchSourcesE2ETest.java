package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.service.prompt.websearch.WebSearchPromptExecutionService;
import com.devoxx.genie.service.websearch.TavilyStubServer;
import com.devoxx.genie.service.websearch.WebSearchProvider;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TASK-277.2 end-to-end: /search through the real strategy, execution service and Tavily engine (against a
 * local stub server). Asserts the source links reach the message handed to the chat panel for display.
 */
class WebSearchSourcesE2ETest {

    private MockedStatic<ApplicationManager> applicationManagerMock;
    private MockedStatic<ChatMemoryManager> chatMemoryManagerMock;
    private MockedStatic<ThreadPoolManager> threadPoolManagerMock;
    private MockedStatic<MessageCreationService> messageCreationServiceMock;
    private MockedStatic<WebSearchPromptExecutionService> executionServiceMock;
    private MockedStatic<PromptErrorHandler> promptErrorHandlerMock;
    private MockedStatic<DevoxxGenieStateService> stateMock;

    private final Project project = mock(Project.class);
    private final PromptOutputPanel panel = mock(PromptOutputPanel.class);
    @SuppressWarnings("unchecked")
    private final PromptTask<PromptResult> resultTask = mock(PromptTask.class);
    private final AtomicInteger llmCalls = new AtomicInteger();

    private DevoxxGenieStateService state;
    private TavilyStubServer tavily;
    private WebSearchPromptStrategy strategy;

    @BeforeEach
    void setUp() throws Exception {
        applicationManagerMock = mockStatic(ApplicationManager.class);
        chatMemoryManagerMock = mockStatic(ChatMemoryManager.class);
        threadPoolManagerMock = mockStatic(ThreadPoolManager.class);
        messageCreationServiceMock = mockStatic(MessageCreationService.class);
        executionServiceMock = mockStatic(WebSearchPromptExecutionService.class);
        promptErrorHandlerMock = mockStatic(PromptErrorHandler.class);
        stateMock = mockStatic(DevoxxGenieStateService.class);

        Application application = mock(Application.class);
        applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(application);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        ThreadPoolManager threadPoolManager = mock(ThreadPoolManager.class);
        MessageCreationService messageCreationService = mock(MessageCreationService.class);
        chatMemoryManagerMock.when(ChatMemoryManager::getInstance).thenReturn(chatMemoryManager);
        threadPoolManagerMock.when(ThreadPoolManager::getInstance).thenReturn(threadPoolManager);
        messageCreationServiceMock.when(MessageCreationService::getInstance).thenReturn(messageCreationService);

        // Run the strategy's background work inline.
        ExecutorService inline = mock(ExecutorService.class);
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(inline).execute(any(Runnable.class));
        when(threadPoolManager.getPromptExecutionPool()).thenReturn(inline);

        state = new DevoxxGenieStateService();
        state.setIsWebSearchEnabled(true);
        state.setMaxSearchResults(3);
        state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
        state.setTavilySearchKey("tvly-e2e-key");
        stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(state);

        // The context carries a chat model, so the service never needs ChatModelProvider here.
        WebSearchPromptExecutionService realService = new WebSearchPromptExecutionService();
        executionServiceMock.when(WebSearchPromptExecutionService::getInstance).thenReturn(realService);

        tavily = new TavilyStubServer();
        strategy = new WebSearchPromptStrategy(project);
    }

    @AfterEach
    void tearDown() throws Exception {
        tavily.close();
        stateMock.close();
        promptErrorHandlerMock.close();
        executionServiceMock.close();
        messageCreationServiceMock.close();
        threadPoolManagerMock.close();
        chatMemoryManagerMock.close();
        applicationManagerMock.close();
    }

    private ChatMessageContext searchContext() {
        ChatModel chatModel = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                llmCalls.incrementAndGet();
                return ChatResponse.builder().aiMessage(AiMessage.from("Use since-build in plugin.xml.")).build();
            }
        };
        return ChatMessageContext.builder()
                .project(project)
                .userPrompt("How do I declare IntelliJ plugin compatibility?")
                .chatModel(chatModel)
                .languageModel(LanguageModel.builder().provider(ModelProvider.OpenAI).modelName("gpt-test").build())
                .build();
    }

    private String messageShownInChat() {
        failOnReportedError();
        ArgumentCaptor<ChatMessageContext> shown = ArgumentCaptor.forClass(ChatMessageContext.class);
        verify(panel).addChatResponse(shown.capture());
        return shown.getValue().getAiMessage().text();
    }

    /** The strategy swallows exceptions into PromptErrorHandler; surface the real cause in the failure. */
    private void failOnReportedError() {
        ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
        promptErrorHandlerMock.verify(() -> PromptErrorHandler.handleException(any(), error.capture(), any()),
                atLeast(0));
        if (!error.getAllValues().isEmpty()) {
            Throwable reported = error.getValue();
            throw new AssertionError("search reported an error instead of an answer: " + reported
                    + (reported.getCause() == null ? "" : " caused by " + reported.getCause()), reported);
        }
    }

    @Test
    void searchAnswer_shownInChat_endsWithClickableSourceLinks() {
        tavily.respondWithHit("Build number ranges",
                "https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html",
                "since-build declares the earliest compatible IDE build.");

        strategy.executeStrategySpecific(searchContext(), panel, resultTask);

        assertThat(messageShownInChat())
                .startsWith("Use since-build in plugin.xml.")
                .contains("**Sources**")
                .contains("1. [Build number ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html)");
        assertThat(llmCalls).hasValue(1);
        verify(resultTask).complete(argThat(result -> result.getError() == null));
    }

    // TASK-277.4: a failing (soon discontinued) Google API reaches the user as the deprecation message.
    @Test
    void failingGoogle_reachesTheUserAsTheDeprecationMessage() {
        state.setSelectedWebSearchProvider(WebSearchProvider.GOOGLE);
        state.setGoogleSearchKey("g-key");
        state.setGoogleCSIKey("cse-id");
        com.devoxx.genie.service.websearch.WebSearchServiceTestSupport.replaceEngine(WebSearchProvider.GOOGLE, request -> {
            throw new IllegalStateException("410 Gone");
        });

        strategy.executeStrategySpecific(searchContext(), panel, resultTask);

        verify(panel, never()).addChatResponse(any());
        ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
        promptErrorHandlerMock.verify(() -> PromptErrorHandler.handleException(any(), error.capture(), any()));
        assertThat(error.getValue().getMessage()).contains("Google Custom Search").contains("January 1, 2027")
                .doesNotContain("410 Gone");
    }

    @Test
    void noResults_shownInChatAsAClearMessage_notAnErrorCard() {
        tavily.respondWithNoResults();

        strategy.executeStrategySpecific(searchContext(), panel, resultTask);

        assertThat(messageShownInChat())
                .contains("No web results found for \"How do I declare IntelliJ plugin compatibility?\"")
                .doesNotContain("**Sources**");
        assertThat(llmCalls).as("no LLM call without results").hasValue(0);
        promptErrorHandlerMock.verifyNoInteractions();
        verify(resultTask).complete(argThat(result -> result.getError() == null));
    }
}
