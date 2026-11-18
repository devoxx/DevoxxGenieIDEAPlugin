package com.devoxx.genie.service.websearch;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.service.agent.tool.WebSearchToolExecutor;
import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.strategy.WebSearchStrategyAccess;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.service.prompt.websearch.WebSearchPromptExecutionService;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.settings.websearch.WebSearchTestSearchAccess;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TASK-277.5 end-to-end: the real Parallel REST engine, through {@link WebSearchService}, against a local stub
 * server, for the agent web_search tool, /search (real strategy and execution service) and Settings Test search.
 */
class ParallelE2ETest {

    private MockedStatic<DevoxxGenieStateService> stateStatic;
    private DevoxxGenieStateService state;
    private ParallelStubServer parallel;
    private KeenableStubServer keenable;

    @BeforeEach
    void setUp() throws Exception {
        state = new DevoxxGenieStateService();
        state.setIsWebSearchEnabled(true);
        state.setMaxSearchResults(3);
        state.setSelectedWebSearchProvider(WebSearchProvider.PARALLEL);
        state.setCredential(CredentialKey.PARALLEL_SEARCH_KEY, "par-e2e-key");
        stateStatic = mockStatic(DevoxxGenieStateService.class);
        stateStatic.when(DevoxxGenieStateService::getInstance).thenReturn(state);
        keenable = new KeenableStubServer(); // proves another provider stays untouched
        parallel = new ParallelStubServer();
    }

    @AfterEach
    void tearDown() throws Exception {
        parallel.close();
        keenable.close();
        WebSearchServiceTestSupport.reset();
        stateStatic.close();
    }

    private static ToolExecutionRequest webSearch(String query) {
        return ToolExecutionRequest.builder().name("web_search").arguments("{\"query\":\"" + query + "\"}").build();
    }

    @Nested
    class AgentTool {

        @Test
        void sendsTheRestRequest_withKey_andFormatsTheResult() throws Exception {
            parallel.respondWithHit("Build number ranges", "https://plugins.jetbrains.com/docs/b", "Use since-build.");

            String result = new WebSearchToolExecutor().execute(webSearch("intellij since-build"), null);

            assertThat(result).contains("Build number ranges").contains("URL: https://plugins.jetbrains.com/docs/b")
                    .contains("Use since-build.");
            RecordedRequest request = parallel.takeRequest();
            assertThat(request.getPath()).isEqualTo("/v1/search");
            assertThat(request.getHeader("x-api-key")).isEqualTo("par-e2e-key");
            assertThat(request.getHeader("User-Agent")).isEqualTo("DevoxxGenie");
            assertThat(request.getBody().readUtf8()).contains("\"search_queries\":[\"intellij since-build\"]")
                    .contains("\"mode\":\"fast\"").contains("\"max_results\":3");
            assertThat(keenable.requestCount()).isZero();
        }

        @Test
        void emptyResults_areReportedToTheAgent() {
            parallel.respondWithNoResults();

            assertThat(new WebSearchToolExecutor().execute(webSearch("nothing"), null))
                    .isEqualTo("No results found for query: nothing");
        }

        @Test
        void rateLimit_isASpecificMessage() {
            parallel.respondWithStatus(429);

            assertThat(new WebSearchToolExecutor().execute(webSearch("q"), null))
                    .startsWith("Error:").contains("rate limit").contains("minute");
        }

        @Test
        void rejectedKey_isASpecificMessage_withoutTheKey() {
            parallel.respondWithStatus(401);

            assertThat(new WebSearchToolExecutor().execute(webSearch("q"), null))
                    .startsWith("Error:").contains("Parallel rejected the API key").doesNotContain("par-e2e-key");
        }

        @Test
        void outOfCredit_isASpecificMessage() {
            parallel.respondWithStatus(402);

            assertThat(new WebSearchToolExecutor().execute(webSearch("q"), null)).contains("out of credit");
        }

        @Test
        void missingKey_sendsNothing_andNamesTheKey() {
            state.setCredential(CredentialKey.PARALLEL_SEARCH_KEY, "");

            assertThat(new WebSearchToolExecutor().execute(webSearch("q"), null))
                    .startsWith("Error:").contains("Parallel is missing its API key");
            assertThat(parallel.requestCount()).isZero();
        }

        @Test
        void parallelIsNeverContacted_whenAnotherProviderIsSelected() {
            state.setSelectedWebSearchProvider(WebSearchProvider.KEENABLE);
            keenable.respondWithHit("Keenable hit", "https://example.com/k", "k");

            new WebSearchToolExecutor().execute(webSearch("q"), null);

            assertThat(parallel.requestCount()).isZero();
            assertThat(keenable.requestCount()).isEqualTo(1);
        }
    }

    @Nested
    class SlashSearch {

        private MockedStatic<ApplicationManager> applicationManagerMock;
        private MockedStatic<ChatMemoryManager> chatMemoryManagerMock;
        private MockedStatic<ThreadPoolManager> threadPoolManagerMock;
        private MockedStatic<MessageCreationService> messageCreationServiceMock;
        private MockedStatic<WebSearchPromptExecutionService> executionServiceMock;
        private MockedStatic<PromptErrorHandler> promptErrorHandlerMock;
        private final PromptOutputPanel panel = mock(PromptOutputPanel.class);
        @SuppressWarnings("unchecked")
        private final PromptTask<PromptResult> resultTask = mock(PromptTask.class);

        @BeforeEach
        void setUpStrategy() {
            applicationManagerMock = mockStatic(ApplicationManager.class);
            chatMemoryManagerMock = mockStatic(ChatMemoryManager.class);
            threadPoolManagerMock = mockStatic(ThreadPoolManager.class);
            messageCreationServiceMock = mockStatic(MessageCreationService.class);
            executionServiceMock = mockStatic(WebSearchPromptExecutionService.class);
            promptErrorHandlerMock = mockStatic(PromptErrorHandler.class);
            applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(mock(Application.class));
            chatMemoryManagerMock.when(ChatMemoryManager::getInstance).thenReturn(mock(ChatMemoryManager.class));
            messageCreationServiceMock.when(MessageCreationService::getInstance).thenReturn(mock(MessageCreationService.class));
            ThreadPoolManager threadPoolManager = mock(ThreadPoolManager.class);
            threadPoolManagerMock.when(ThreadPoolManager::getInstance).thenReturn(threadPoolManager);
            ExecutorService inline = mock(ExecutorService.class);
            doAnswer(inv -> {
                ((Runnable) inv.getArgument(0)).run();
                return null;
            }).when(inline).execute(any(Runnable.class));
            when(threadPoolManager.getPromptExecutionPool()).thenReturn(inline);
            executionServiceMock.when(WebSearchPromptExecutionService::getInstance)
                    .thenReturn(new WebSearchPromptExecutionService());
        }

        @AfterEach
        void tearDownStrategy() {
            promptErrorHandlerMock.close();
            executionServiceMock.close();
            messageCreationServiceMock.close();
            threadPoolManagerMock.close();
            chatMemoryManagerMock.close();
            applicationManagerMock.close();
        }

        private void runSearch() {
            ChatModel chatModel = new ChatModel() {
                @Override
                public ChatResponse doChat(ChatRequest request) {
                    return ChatResponse.builder().aiMessage(AiMessage.from("Parallel answer.")).build();
                }
            };
            WebSearchStrategyAccess.run(ChatMessageContext.builder()
                    .project(mock(Project.class))
                    .userPrompt("intellij plugin compatibility")
                    .chatModel(chatModel)
                    .languageModel(LanguageModel.builder().provider(ModelProvider.OpenAI).modelName("m").build())
                    .build(), panel, resultTask);
        }

        @Test
        void answerShownInChat_hasParallelSourceLinks() {
            parallel.respondWithHit("Build number ranges", "https://plugins.jetbrains.com/docs/b", "since-build");

            runSearch();

            ArgumentCaptor<ChatMessageContext> shown = ArgumentCaptor.forClass(ChatMessageContext.class);
            verify(panel).addChatResponse(shown.capture());
            assertThat(shown.getValue().getAiMessage().text()).startsWith("Parallel answer.")
                    .contains("1. [Build number ranges](https://plugins.jetbrains.com/docs/b)");
        }

        @Test
        void serverError_reachesTheUserAsTheSpecificMessage() {
            parallel.respondWithStatus(503);

            runSearch();

            verify(panel, never()).addChatResponse(any());
            ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
            promptErrorHandlerMock.verify(() -> PromptErrorHandler.handleException(any(), error.capture(), any()));
            assertThat(error.getValue().getMessage()).contains("Parallel search failed (HTTP 503)")
                    .contains("Try again later");
        }
    }

    @Nested
    class TestSearch {

        @Test
        void testSearchShowsResults() {
            parallel.respondWithHit("Stub", "https://example.com/stub", "excerpt");

            String output = WebSearchTestSearchAccess.run(new WebSearchConfig(WebSearchProvider.PARALLEL,
                    Map.of(CredentialKey.PARALLEL_SEARCH_KEY, "par-key"), 3), "q");

            assertThat(output).contains("Stub").contains("https://example.com/stub").contains("excerpt");
        }

        @Test
        void forbidden_showsTheSpecificMessage() {
            parallel.respondWithStatus(403);

            String output = WebSearchTestSearchAccess.run(new WebSearchConfig(WebSearchProvider.PARALLEL,
                    Map.of(CredentialKey.PARALLEL_SEARCH_KEY, "par-bad"), 3), "q");

            assertThat(output).contains("Parallel rejected the API key").contains("HTTP 403").doesNotContain("par-bad");
        }
    }
}
