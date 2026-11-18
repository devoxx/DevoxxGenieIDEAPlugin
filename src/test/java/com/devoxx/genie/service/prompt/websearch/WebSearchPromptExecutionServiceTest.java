package com.devoxx.genie.service.prompt.websearch;

import com.devoxx.genie.chatmodel.ChatModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.websearch.TavilyStubServer;
import com.devoxx.genie.service.websearch.WebSearchNotConfiguredException;
import com.devoxx.genie.service.websearch.WebSearchProvider;
import com.devoxx.genie.service.websearch.WebSearchServiceTestSupport;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * /search execution (TASK-277.1). End-to-end cases run the real Tavily engine against a local stub
 * server and a recording chat model, so the test sees exactly what reached the LLM.
 */
class WebSearchPromptExecutionServiceTest {

    /** A chat model that records every request and answers with a fixed text. */
    static final class RecordingChatModel implements ChatModel {
        final List<ChatRequest> requests = new ArrayList<>();

        @Override
        public ChatResponse doChat(ChatRequest request) {
            requests.add(request);
            return ChatResponse.builder().aiMessage(AiMessage.from("Grounded answer.")).build();
        }

        String lastUserText() {
            List<ChatMessage> messages = requests.get(requests.size() - 1).messages();
            return messages.stream()
                    .filter(UserMessage.class::isInstance)
                    .map(m -> ((UserMessage) m).singleText())
                    .reduce((a, b) -> b)
                    .orElse("");
        }
    }

    private MockedStatic<DevoxxGenieStateService> stateStatic;
    private DevoxxGenieStateService state;
    private TavilyStubServer tavily;
    private RecordingChatModel chatModel;
    private ChatModelProvider chatModelProvider;
    private WebSearchPromptExecutionService service;

    @BeforeEach
    void setUp() throws Exception {
        state = new DevoxxGenieStateService();
        state.setIsWebSearchEnabled(true);
        state.setMaxSearchResults(3);
        stateStatic = mockStatic(DevoxxGenieStateService.class);
        stateStatic.when(DevoxxGenieStateService::getInstance).thenReturn(state);

        tavily = new TavilyStubServer();
        chatModel = new RecordingChatModel();
        chatModelProvider = mock(ChatModelProvider.class);
        service = new WebSearchPromptExecutionService();
        service.chatModelProvider = chatModelProvider;
    }

    @AfterEach
    void tearDown() throws Exception {
        tavily.close();
        stateStatic.close();
    }

    private ChatMessageContext context(ChatModel model) {
        return ChatMessageContext.builder()
                .userPrompt("Find the IntelliJ plugin compatibility docs")
                .chatModel(model)
                .build();
    }

    @Test
    void e2e_tavilyResultsReachTheChatModel_andTheAnswerIsReturned() throws Exception {
        state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
        state.setTavilySearchKey("tvly-e2e-key");
        tavily.respondWithHit("Build number ranges", "https://plugins.jetbrains.com/docs/build-number-ranges.html",
                "Use since-build to declare the minimum IDE version.");

        Optional<AiMessage> answer = service.searchWeb(context(chatModel));

        assertThat(answer).map(AiMessage::text).contains("Grounded answer.");
        assertThat(tavily.takeRequest().getBody().readUtf8()).contains("Find the IntelliJ plugin compatibility docs");
        assertThat(chatModel.lastUserText())
                .contains("Find the IntelliJ plugin compatibility docs")
                .contains("Use since-build to declare the minimum IDE version.");
    }

    @Test
    void e2e_googleSelected_neverContactsTavily() {
        state.setSelectedWebSearchProvider(WebSearchProvider.GOOGLE);
        state.setGoogleSearchKey("g-key");
        state.setGoogleCSIKey("cse-id");
        state.setTavilySearchKey("tvly-key-present-but-unselected");
        List<String> googleQueries = new ArrayList<>();
        WebSearchServiceTestSupport.replaceEngine(WebSearchProvider.GOOGLE, request -> {
            googleQueries.add(request.searchTerms());
            return WebSearchServiceTestSupport.results("Google result", "https://example.com/g");
        });

        assertThat(service.searchWeb(context(chatModel))).isPresent();
        assertThat(googleQueries).hasSize(1);
        assertThat(chatModel.lastUserText()).contains("snippet of Google result");
        assertThat(tavily.requestCount()).isZero();
    }

    @Test
    void selectedProviderWithBlankKey_failsWithAClearMessage_andSendsNothing() {
        state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);

        assertThatThrownBy(() -> service.searchWeb(context(chatModel)))
                .isInstanceOf(WebSearchNotConfiguredException.class)
                .hasMessageContaining("Tavily")
                .hasMessageContaining("API key")
                .hasMessageContaining("Settings → DevoxxGenie → Web search");
        assertThat(tavily.requestCount()).isZero();
        assertThat(chatModel.requests).isEmpty();
    }

    @Test
    void googleWithoutSearchEngineId_failsWithAClearMessage() {
        state.setSelectedWebSearchProvider(WebSearchProvider.GOOGLE);
        state.setGoogleSearchKey("g-key");

        assertThatThrownBy(() -> service.searchWeb(context(chatModel)))
                .isInstanceOf(WebSearchNotConfiguredException.class)
                .hasMessageContaining("Search Engine ID");
    }

    @Test
    void noProviderSelected_failsWithAClearMessage() {
        state.setSelectedWebSearchProvider(null);

        assertThatThrownBy(() -> service.searchWeb(context(chatModel)))
                .isInstanceOf(WebSearchNotConfiguredException.class)
                .hasMessageContaining("No web search provider is selected");
    }

    // Regression: in streaming mode getChatModel() is null. AiServices needs a non-streaming chatModel
    // for a synchronous String method, so the service must ask ChatModelProvider for one.
    @Test
    void streamingMode_withNullChatModel_usesChatModelProvider() {
        state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
        state.setTavilySearchKey("tvly-e2e-key");
        tavily.respondWithHit("t", "https://example.com/t", "c");
        ChatMessageContext context = context(null);
        when(chatModelProvider.getChatLanguageModel(context)).thenReturn(chatModel);

        assertThat(service.searchWeb(context)).isPresent();
        verify(chatModelProvider).getChatLanguageModel(context);
    }

    @Test
    void nonStreamingMode_withChatModel_doesNotConsultProvider() {
        state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
        state.setTavilySearchKey("tvly-e2e-key");
        tavily.respondWithHit("t", "https://example.com/t", "c");

        assertThat(service.searchWeb(context(chatModel))).isPresent();
        verify(chatModelProvider, never()).getChatLanguageModel(any());
    }
}
