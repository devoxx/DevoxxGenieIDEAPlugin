package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.service.websearch.TavilyStubServer;
import com.devoxx.genie.service.websearch.WebSearchProvider;
import com.devoxx.genie.service.websearch.WebSearchService;
import com.devoxx.genie.service.websearch.WebSearchServiceTestSupport;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * TASK-277.1 end-to-end: the agent {@code web_search} tool runs the real Tavily engine through
 * {@link WebSearchService} against a local stub server, and never contacts an unselected provider.
 */
class WebSearchToolExecutorE2ETest {

    private MockedStatic<DevoxxGenieStateService> stateStatic;
    private DevoxxGenieStateService state;
    private TavilyStubServer tavily;
    private final WebSearchToolExecutor executor = new WebSearchToolExecutor();

    @BeforeEach
    void setUp() throws Exception {
        state = new DevoxxGenieStateService();
        state.setMaxSearchResults(3);
        stateStatic = mockStatic(DevoxxGenieStateService.class);
        stateStatic.when(DevoxxGenieStateService::getInstance).thenReturn(state);
        tavily = new TavilyStubServer();
    }

    @AfterEach
    void tearDown() throws Exception {
        tavily.close();
        stateStatic.close();
    }

    private static ToolExecutionRequest search(String query) {
        return ToolExecutionRequest.builder()
                .name("web_search")
                .arguments("{\"query\":\"" + query + "\"}")
                .build();
    }

    @Test
    void selectedTavily_searchesTheStub_andFormatsTitleUrlAndSnippet() throws Exception {
        state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
        state.setTavilySearchKey("tvly-e2e-key");
        tavily.respondWithHit("Plugin compatibility", "https://plugins.jetbrains.com/docs/compat.html",
                "Declare since-build and until-build.");

        String result = executor.execute(search("intellij plugin compatibility"), null);

        assertThat(result)
                .contains("Found 1 result")
                .contains("Plugin compatibility")
                .contains("URL: https://plugins.jetbrains.com/docs/compat.html")
                .contains("Declare since-build and until-build.");
        RecordedRequest request = tavily.takeRequest();
        assertThat(request.getPath()).isEqualTo("/search");
        String body = request.getBody().readUtf8();
        assertThat(body).contains("intellij plugin compatibility").contains("tvly-e2e-key");
        assertThat(body).contains("3"); // max results from settings
    }

    @Test
    void noResults_fromTheStub_isReportedToTheAgent() {
        state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
        state.setTavilySearchKey("tvly-e2e-key");
        tavily.respondWithNoResults();

        assertThat(executor.execute(search("nothing matches this"), null))
                .isEqualTo("No results found for query: nothing matches this");
    }

    @Test
    void providerHttpError_isReturnedAsAnErrorString_notThrown() {
        state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
        state.setTavilySearchKey("bad-key");
        tavily.respondWithStatus(401, "{\"detail\":{\"error\":\"Unauthorized\"}}");

        String result = executor.execute(search("anything"), null);

        assertThat(result).startsWith("Error: Web search failed").contains("Tavily");
        assertThat(tavily.requestCount()).isEqualTo(1);
    }

    @Test
    void selectedTavilyWithBlankKey_sendsNoRequest_andNamesWhatIsMissing() {
        state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);

        String result = executor.execute(search("anything"), null);

        assertThat(result).startsWith("Error:").contains("Tavily").contains("API key")
                .contains("Settings → DevoxxGenie → Web search");
        assertThat(tavily.requestCount()).isZero();
    }

    @Test
    void selectedGoogle_usesGoogle_andNeverContactsTavily_evenWithATavilyKey() {
        state.setSelectedWebSearchProvider(WebSearchProvider.GOOGLE);
        state.setGoogleSearchKey("g-key");
        state.setGoogleCSIKey("cse-id");
        state.setTavilySearchKey("tvly-key-present-but-unselected");
        List<String> googleQueries = new ArrayList<>();
        WebSearchServiceTestSupport.replaceEngine(WebSearchProvider.GOOGLE, request -> {
            googleQueries.add(request.searchTerms());
            return WebSearchServiceTestSupport.results("Google hit", "https://example.com/google");
        });

        String result = executor.execute(search("which provider"), null);

        assertThat(result).contains("Google hit");
        assertThat(googleQueries).containsExactly("which provider");
        assertThat(tavily.requestCount()).isZero();
    }

    @Test
    void noProviderSelected_sendsNoRequest() {
        state.setSelectedWebSearchProvider(null);
        state.setTavilySearchKey("tvly-key");

        assertThat(executor.execute(search("anything"), null))
                .startsWith("Error:").contains("No web search provider is selected");
        assertThat(tavily.requestCount()).isZero();
    }

    @Test
    void missingQuery_isRejected_beforeAnySearch() {
        state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
        state.setTavilySearchKey("tvly-key");

        assertThat(executor.execute(ToolExecutionRequest.builder().name("web_search").arguments("{}").build(), null))
                .isEqualTo("Error: 'query' parameter is required.");
        assertThat(tavily.requestCount()).isZero();
    }
}
