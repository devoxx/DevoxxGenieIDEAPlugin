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
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSearchPromptStrategyTest {

    @Mock private Project mockProject;
    @Mock private ChatMemoryManager mockChatMemoryManager;
    @Mock private ThreadPoolManager mockThreadPoolManager;
    @Mock private MessageCreationService mockMessageCreationService;
    @Mock private ChatMessageContext mockContext;
    @Mock private PromptOutputPanel mockPanel;
    @Mock private PromptTask<PromptResult> mockResultTask;
    @Mock private WebSearchPromptExecutionService mockWebSearchService;
    @Mock private ExecutorService mockExecutor;
    @Mock private LanguageModel mockLanguageModel;

    private MockedStatic<ApplicationManager> applicationManagerMock;
    private MockedStatic<ChatMemoryManager> chatMemoryManagerMock;
    private MockedStatic<ThreadPoolManager> threadPoolManagerMock;
    private MockedStatic<MessageCreationService> messageCreationServiceMock;
    private MockedStatic<WebSearchPromptExecutionService> webSearchServiceMock;
    private MockedStatic<PromptErrorHandler> promptErrorHandlerMock;

    private WebSearchPromptStrategy strategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        applicationManagerMock = mockStatic(ApplicationManager.class);
        chatMemoryManagerMock = mockStatic(ChatMemoryManager.class);
        threadPoolManagerMock = mockStatic(ThreadPoolManager.class);
        messageCreationServiceMock = mockStatic(MessageCreationService.class);
        webSearchServiceMock = mockStatic(WebSearchPromptExecutionService.class);
        promptErrorHandlerMock = mockStatic(PromptErrorHandler.class);

        Application mockApplication = mock(Application.class);
        applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(mockApplication);
        when(mockApplication.getService(ChatMemoryManager.class)).thenReturn(mockChatMemoryManager);
        when(mockApplication.getService(ThreadPoolManager.class)).thenReturn(mockThreadPoolManager);
        when(mockApplication.getService(MessageCreationService.class)).thenReturn(mockMessageCreationService);

        chatMemoryManagerMock.when(ChatMemoryManager::getInstance).thenReturn(mockChatMemoryManager);
        threadPoolManagerMock.when(ThreadPoolManager::getInstance).thenReturn(mockThreadPoolManager);
        messageCreationServiceMock.when(MessageCreationService::getInstance).thenReturn(mockMessageCreationService);
        webSearchServiceMock.when(WebSearchPromptExecutionService::getInstance).thenReturn(mockWebSearchService);

        when(mockThreadPoolManager.getPromptExecutionPool()).thenReturn(mockExecutor);
        when(mockContext.getProject()).thenReturn(mockProject);
        when(mockContext.getUserPrompt()).thenReturn("test query");
        when(mockContext.getCreatedOn()).thenReturn(java.time.LocalDateTime.now());
        when(mockLanguageModel.getProvider()).thenReturn(ModelProvider.OpenAI);
        when(mockLanguageModel.getModelName()).thenReturn("gpt-4");
        when(mockContext.getLanguageModel()).thenReturn(mockLanguageModel);

        // Execute runnables inline for testing
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mockExecutor).execute(any(Runnable.class));

        strategy = new WebSearchPromptStrategy(mockProject);
    }

    @AfterEach
    void tearDown() {
        if (applicationManagerMock != null) applicationManagerMock.close();
        if (chatMemoryManagerMock != null) chatMemoryManagerMock.close();
        if (threadPoolManagerMock != null) threadPoolManagerMock.close();
        if (messageCreationServiceMock != null) messageCreationServiceMock.close();
        if (webSearchServiceMock != null) webSearchServiceMock.close();
        if (promptErrorHandlerMock != null) promptErrorHandlerMock.close();
    }

    @Test
    void getStrategyName_returnsWebSearchPrompt() {
        assertThat(strategy.getStrategyName()).isEqualTo("web search prompt");
    }

    @Test
    void cancel_isNoOp() {
        // cancel() should not throw any exception
        strategy.cancel();
    }

    @Test
    void executeStrategySpecific_withSuccessfulSearch_completesSuccessfully() {
        AiMessage aiMessage = AiMessage.from("Search result text");
        when(mockWebSearchService.searchWeb(mockContext)).thenReturn(Optional.of(aiMessage));

        strategy.executeStrategySpecific(mockContext, mockPanel, mockResultTask);

        verify(mockWebSearchService).searchWeb(mockContext);
        verify(mockPanel).addChatResponse(mockContext);
        verify(mockResultTask).complete(any(PromptResult.class));
    }

    @Test
    void executeStrategySpecific_withNoResults_completesWithFailure() {
        when(mockWebSearchService.searchWeb(mockContext)).thenReturn(Optional.empty());

        strategy.executeStrategySpecific(mockContext, mockPanel, mockResultTask);

        verify(mockWebSearchService).searchWeb(mockContext);
        verify(mockPanel, never()).addChatResponse(any());
        verify(mockResultTask).complete(argThat(result -> {
            assertThat(result.getError()).isNotNull();
            return true;
        }));
    }

    @Test
    void executeStrategySpecific_withException_handlesError() {
        when(mockWebSearchService.searchWeb(mockContext)).thenThrow(new RuntimeException("Search failed"));

        strategy.executeStrategySpecific(mockContext, mockPanel, mockResultTask);

        verify(mockResultTask).complete(argThat(result -> {
            assertThat(result.getError()).isNotNull();
            return true;
        }));
    }

    @Test
    void executeStrategySpecific_setsExecutionTime() {
        AiMessage aiMessage = AiMessage.from("Result");
        when(mockWebSearchService.searchWeb(mockContext)).thenReturn(Optional.of(aiMessage));

        strategy.executeStrategySpecific(mockContext, mockPanel, mockResultTask);

        verify(mockContext).setAiMessage(aiMessage);
        verify(mockContext).setExecutionTimeMs(anyLong());
    }
}
