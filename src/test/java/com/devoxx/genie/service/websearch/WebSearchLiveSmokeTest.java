package com.devoxx.genie.service.websearch;

import com.devoxx.genie.service.credentials.CredentialKey;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchResults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live smoke tests against the real provider APIs, through {@link WebSearchService}. Each runs only when
 * that provider's credentials are in the environment and is skipped otherwise, so CI without keys stays
 * green. Run locally with e.g. {@code TAVILY_API_KEY=... ./gradlew test --tests '*WebSearchLiveSmokeTest'};
 * Keenable runs keyless with {@code KEENABLE_LIVE_SMOKE=true} (optionally {@code KEENABLE_API_KEY}).
 */
class WebSearchLiveSmokeTest {

    private static final String QUERY = "IntelliJ Platform plugin compatibility since-build";

    @Test
    @EnabledIfEnvironmentVariable(named = "TAVILY_API_KEY", matches = ".+")
    void tavily_returnsResultsWithUrls() {
        WebSearchConfig config = new WebSearchConfig(WebSearchProvider.TAVILY,
                Map.of(CredentialKey.TAVILY_SEARCH_KEY, System.getenv("TAVILY_API_KEY")), 3);

        assertHasResultsWithUrls(WebSearchService.search(config, QUERY));
    }

    @Test
    @EnabledIfEnvironmentVariables({
            @EnabledIfEnvironmentVariable(named = "GOOGLE_SEARCH_KEY", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "GOOGLE_CSI_KEY", matches = ".+")
    })
    void google_returnsResultsWithUrls() {
        WebSearchConfig config = new WebSearchConfig(WebSearchProvider.GOOGLE, Map.of(
                CredentialKey.GOOGLE_SEARCH_KEY, System.getenv("GOOGLE_SEARCH_KEY"),
                CredentialKey.GOOGLE_CSI_KEY, System.getenv("GOOGLE_CSI_KEY")), 3);

        assertHasResultsWithUrls(WebSearchService.search(config, QUERY));
    }

    /** Keyless needs no secret, so it must be switched on explicitly to avoid hitting a real API in CI. */
    @Test
    @EnabledIfEnvironmentVariable(named = "KEENABLE_LIVE_SMOKE", matches = "true")
    void keenable_keyless_returnsResultsWithUrls() {
        String key = System.getenv("KEENABLE_API_KEY");
        WebSearchConfig config = new WebSearchConfig(WebSearchProvider.KEENABLE,
                key == null ? Map.of() : Map.of(CredentialKey.KEENABLE_SEARCH_KEY, key), 3);

        assertHasResultsWithUrls(WebSearchService.search(config, QUERY));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PARALLEL_API_KEY", matches = ".+")
    void parallel_returnsResultsWithUrls() {
        WebSearchConfig config = new WebSearchConfig(WebSearchProvider.PARALLEL,
                Map.of(CredentialKey.PARALLEL_SEARCH_KEY, System.getenv("PARALLEL_API_KEY")), 3);

        assertHasResultsWithUrls(WebSearchService.search(config, QUERY));
    }

    private static void assertHasResultsWithUrls(WebSearchResults results) {
        assertThat(results.results()).isNotEmpty();
        for (WebSearchOrganicResult hit : results.results()) {
            assertThat(hit.url()).isNotNull();
            assertThat(hit.url().getScheme()).startsWith("http");
        }
    }
}
