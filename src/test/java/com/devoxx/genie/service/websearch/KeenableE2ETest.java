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
 * TASK-277.3 end-to-end: the real Keenable engine, through {@link WebSearchService}, against a local stub
 * server, for the agent web_search tool, /search (real strategy and execution service) and the settings
 * Test search. Covers keyless and keyed endpoints, identifying headers, and 429/401/403 messages.
 */
class KeenableE2ETest {

    private MockedStatic<DevoxxGenieStateService> stateStatic;
    private DevoxxGenieStateService state;
    private KeenableStubServer keenable;
    private TavilyStubServer tavily;

    @BeforeEach
    void setUp() throws Exception {
        state = new DevoxxGenieStateService();
        state.setIsWebSearchEnabled(true);
        state.setMaxSearchResults(3);
        state.setSelectedWebSearchProvider(WebSearchProvider.KEENABLE);
        stateStatic = mockStatic(DevoxxGenieStateService.class);
        stateStatic.when(DevoxxGenieStateService::getInstance).thenReturn(state);
        tavily = new TavilyStubServer();     // to prove other providers stay untouched
        keenable = new KeenableStubServer(); // installed after Tavily so both overrides apply
    }

    @AfterEach
    void tearDown() throws Exception {
        keenable.close();
        tavily.close(); // also resets all registry overrides
        stateStatic.close();
    }

    private static ToolExecutionRequest webSearch(String query) {
        return ToolExecutionRequest.builder().name("web_search").arguments("{\"query\":\"" + query + "\"}").build();
    }

    @Nested
    class AgentTool {

        @Test
        void keyless_usesThePublicEndpoint_withIdentifyingHeaders_andNoKey() throws Exception {
            keenable.respondWithHit("Keenable docs", "https://docs.keenable.ai/api-reference/search", "Search API");

            String result = new WebSearchToolExecutor().execute(webSearch("keenable api"), null);

            assertThat(result).contains("Keenable docs").contains("URL: https://docs.keenable.ai/api-reference/search");
            RecordedRequest request = keenable.takeRequest();
            assertThat(request.getMethod()).isEqualTo("POST");
            assertThat(request.getPath()).isEqualTo("/v1/search/public");
            assertThat(request.getHeader("X-Keenable-Title")).isEqualTo("DevoxxGenie");
            assertThat(request.getHeader("User-Agent")).isEqualTo("DevoxxGenie");
            assertThat(request.getHeader("X-API-Key")).isNull();
            assertThat(request.getBody().readUtf8()).contains("\"query\":\"keenable api\"").contains("\"max_results\":3");
            assertThat(tavily.requestCount()).isZero();
        }

        @Test
        void keyed_usesTheAuthenticatedEndpoint_withTheKey() throws Exception {
            state.setCredential(CredentialKey.KEENABLE_SEARCH_KEY, "kn-secret");
            keenable.respondWithHit("Keyed", "https://example.com/keyed", "ok");

            new WebSearchToolExecutor().execute(webSearch("q"), null);

            RecordedRequest request = keenable.takeRequest();
            assertThat(request.getPath()).isEqualTo("/v1/search");
            assertThat(request.getHeader("X-API-Key")).isEqualTo("kn-secret");
        }

        @Test
        void keylessRateLimit_tellsTheAgentAboutTheFreeLimit() {
            keenable.respondWithStatus(429);

            assertThat(new WebSearchToolExecutor().execute(webSearch("q"), null))
                    .startsWith("Error:").contains("free search limit").contains("API key");
        }

        @Test
        void rejectedKey_tellsTheAgentTheKeyWasRejected() {
            state.setCredential(CredentialKey.KEENABLE_SEARCH_KEY, "kn-bad");
            keenable.respondWithStatus(401);

            assertThat(new WebSearchToolExecutor().execute(webSearch("q"), null))
                    .startsWith("Error:").contains("rejected the API key").doesNotContain("kn-bad");
        }

        @Test
        void keenableIsNeverContacted_whenAnotherProviderIsSelected() {
            state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
            state.setTavilySearchKey("tvly-key");
            tavily.respondWithHit("Tavily hit", "https://example.com/t", "t");

            new WebSearchToolExecutor().execute(webSearch("q"), null);

            assertThat(keenable.requestCount()).isZero();
            assertThat(tavily.requestCount()).isEqualTo(1);
        }

        @Test
        void keenableIsNeverContacted_whenNoProviderIsSelected() {
            state.setSelectedWebSearchProvider(null);

            new WebSearchToolExecutor().execute(webSearch("q"), null);

            assertThat(keenable.requestCount()).isZero();
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
                    return ChatResponse.builder().aiMessage(AiMessage.from("Keenable answer.")).build();
                }
            };
            ChatMessageContext context = ChatMessageContext.builder()
                    .project(mock(Project.class))
                    .userPrompt("what is keenable")
                    .chatModel(chatModel)
                    .languageModel(LanguageModel.builder().provider(ModelProvider.OpenAI).modelName("m").build())
                    .build();
            WebSearchStrategyAccess.run(context, panel, resultTask);
        }

        @Test
        void answerShownInChat_hasKeenableSourceLinks() {
            keenable.respondWithHit("Keenable", "https://keenable.ai/", "search for agents");

            runSearch();

            ArgumentCaptor<ChatMessageContext> shown = ArgumentCaptor.forClass(ChatMessageContext.class);
            verify(panel).addChatResponse(shown.capture());
            assertThat(shown.getValue().getAiMessage().text())
                    .startsWith("Keenable answer.").contains("1. [Keenable](https://keenable.ai/)");
        }

        @Test
        void rateLimit_reachesTheUserAsTheSpecificMessage_notAGenericError() {
            keenable.respondWithStatus(429);

            runSearch();

            verify(panel, never()).addChatResponse(any());
            ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
            promptErrorHandlerMock.verify(() -> PromptErrorHandler.handleException(any(), error.capture(), any()));
            assertThat(error.getValue().getMessage()).contains("free search limit").contains("API key");
        }
    }

    @Nested
    class TestSearch {

        @Test
        void keyless_testSearchShowsResults() {
            keenable.respondWithHit("Stub", "https://example.com/stub", "snippet");

            String output = WebSearchTestSearchAccess.run(
                    new WebSearchConfig(WebSearchProvider.KEENABLE, Map.of(), 3), "q");

            assertThat(output).contains("Stub").contains("https://example.com/stub");
        }

        @Test
        void forbidden_testSearchShowsTheSpecificMessage() {
            keenable.respondWithStatus(403);

            String output = WebSearchTestSearchAccess.run(new WebSearchConfig(WebSearchProvider.KEENABLE,
                    Map.of(CredentialKey.KEENABLE_SEARCH_KEY, "kn-bad"), 3), "q");

            assertThat(output).contains("rejected the API key").contains("HTTP 403").doesNotContain("kn-bad");
        }
    }
}
