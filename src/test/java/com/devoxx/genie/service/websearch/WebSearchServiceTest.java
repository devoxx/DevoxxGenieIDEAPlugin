package com.devoxx.genie.service.websearch;

import com.devoxx.genie.service.analytics.FeatureId;
import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchInformationResult;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSearchServiceTest {

    @AfterEach
    void restoreRegistry() {
        WebSearchService.resetDefinitionsForTesting();
    }

    @Test
    void providersAreListedWithTheDeprecatedOneLast() {
        assertThat(WebSearchProviderRegistry.all()).extracting(WebSearchProviderDefinition::provider)
                .containsExactly(WebSearchProvider.TAVILY, WebSearchProvider.KEENABLE, WebSearchProvider.PARALLEL,
                        WebSearchProvider.GOOGLE);
    }

    private static WebSearchConfig config(WebSearchProvider provider, Map<CredentialKey, String> credentials) {
        return new WebSearchConfig(provider, credentials, 5);
    }

    @Nested
    class Readiness {

        @Test
        void noProviderSelected_isNotReady_withProviderNeutralMessage() {
            WebSearchReadiness readiness = WebSearchService.readiness(config(null, Map.of()));

            assertThat(readiness.isReady()).isFalse();
            assertThat(readiness.status()).isEqualTo(WebSearchReadiness.Status.NO_PROVIDER);
            assertThat(readiness.message())
                    .contains("No web search provider is selected")
                    .contains("Settings → DevoxxGenie → Web search");
        }

        @Test
        void tavilyWithBlankKey_isNotReady_andNamesTheProviderAndField() {
            WebSearchReadiness readiness = WebSearchService.readiness(
                    config(WebSearchProvider.TAVILY, Map.of(CredentialKey.TAVILY_SEARCH_KEY, "   ")));

            assertThat(readiness.isReady()).isFalse();
            assertThat(readiness.status()).isEqualTo(WebSearchReadiness.Status.MISSING_CREDENTIALS);
            assertThat(readiness.provider()).isEqualTo(WebSearchProvider.TAVILY);
            assertThat(readiness.message())
                    .contains("Tavily")
                    .contains("API key")
                    .contains("Settings → DevoxxGenie → Web search");
        }

        @Test
        void tavilyWithKey_isReady() {
            WebSearchReadiness readiness = WebSearchService.readiness(
                    config(WebSearchProvider.TAVILY, Map.of(CredentialKey.TAVILY_SEARCH_KEY, "tvly-123")));

            assertThat(readiness.isReady()).isTrue();
            assertThat(readiness.message()).isEmpty();
        }

        @Test
        void googleWithKeyButNoSearchEngineId_isNotReady_andNamesOnlyTheMissingField() {
            WebSearchReadiness readiness = WebSearchService.readiness(
                    config(WebSearchProvider.GOOGLE, Map.of(CredentialKey.GOOGLE_SEARCH_KEY, "g-key")));

            assertThat(readiness.isReady()).isFalse();
            assertThat(readiness.message())
                    .contains("Google")
                    .contains("Search Engine ID")
                    .doesNotContain("API key");
        }

        @Test
        void googleWithNoCredentials_namesBothMissingFields() {
            WebSearchReadiness readiness = WebSearchService.readiness(config(WebSearchProvider.GOOGLE, Map.of()));

            assertThat(readiness.message()).contains("API key").contains("Search Engine ID");
        }

        @Test
        void googleWithBothCredentials_isReady() {
            WebSearchReadiness readiness = WebSearchService.readiness(config(WebSearchProvider.GOOGLE, Map.of(
                    CredentialKey.GOOGLE_SEARCH_KEY, "g-key",
                    CredentialKey.GOOGLE_CSI_KEY, "cse-id")));

            assertThat(readiness.isReady()).isTrue();
        }

        @Test
        void keenableWithoutAKey_isReady_becauseItsKeyIsOptional() {
            WebSearchReadiness readiness = WebSearchService.readiness(config(WebSearchProvider.KEENABLE, Map.of()));

            assertThat(readiness.isReady()).isTrue();
        }

        @Test
        void credentialsOfAnotherProvider_doNotMakeTheSelectedOneReady() {
            WebSearchReadiness readiness = WebSearchService.readiness(
                    config(WebSearchProvider.GOOGLE, Map.of(CredentialKey.TAVILY_SEARCH_KEY, "tvly-123")));

            assertThat(readiness.isReady()).isFalse();
        }
    }

    @Nested
    class Dispatch {

        @Test
        void createEngine_usesTheSelectedProvidersFactory_only() {
            List<WebSearchProvider> built = new ArrayList<>();
            installRecordingFactories(built);

            WebSearchService.createEngine(config(WebSearchProvider.GOOGLE, Map.of(
                    CredentialKey.GOOGLE_SEARCH_KEY, "g", CredentialKey.GOOGLE_CSI_KEY, "c")));

            assertThat(built).containsExactly(WebSearchProvider.GOOGLE);
        }

        @Test
        void createEngine_whenNotReady_throwsWithReadinessMessage_andBuildsNothing() {
            List<WebSearchProvider> built = new ArrayList<>();
            installRecordingFactories(built);

            assertThatThrownBy(() -> WebSearchService.createEngine(config(WebSearchProvider.TAVILY, Map.of())))
                    .isInstanceOf(WebSearchNotConfiguredException.class)
                    .hasMessageContaining("Tavily")
                    .hasMessageContaining("API key");
            assertThat(built).isEmpty();
        }

        @Test
        void search_passesQueryAndMaxResultsToTheEngine() {
            List<WebSearchRequest> requests = new ArrayList<>();
            WebSearchEngine engine = request -> {
                requests.add(request);
                return results("Hit", "https://example.com/hit");
            };
            WebSearchService.overrideDefinitionsForTesting(p -> definitionWithEngine(p, engine));

            WebSearchResults results = WebSearchService.search(new WebSearchConfig(WebSearchProvider.TAVILY,
                    Map.of(CredentialKey.TAVILY_SEARCH_KEY, "k"), 4), "intellij plugin docs");

            assertThat(requests).singleElement().satisfies(r -> {
                assertThat(r.searchTerms()).isEqualTo("intellij plugin docs");
                assertThat(r.maxResults()).isEqualTo(4);
            });
            assertThat(results.results()).extracting(WebSearchOrganicResult::title).containsExactly("Hit");
        }

        private void installRecordingFactories(List<WebSearchProvider> built) {
            WebSearchService.overrideDefinitionsForTesting(p -> definitionWithEngine(p, request -> {
                throw new AssertionError("search must not run in this test");
            }, built));
        }
    }

    @Nested
    class DeprecatedProviders {

        private final WebSearchConfig google = new WebSearchConfig(WebSearchProvider.GOOGLE,
                Map.of(CredentialKey.GOOGLE_SEARCH_KEY, "g", CredentialKey.GOOGLE_CSI_KEY, "c"), 3);

        @Test
        void successfulSearch_passesThroughUnchanged() {
            WebSearchServiceTestSupport.replaceEngine(WebSearchProvider.GOOGLE,
                    request -> results("Google hit", "https://example.com/g"));

            assertThat(WebSearchService.search(google, "q").results())
                    .extracting(WebSearchOrganicResult::title).containsExactly("Google hit");
        }

        @Test
        void anyFailure_becomesAProviderErrorPointingToTheDeprecation() {
            WebSearchServiceTestSupport.replaceEngine(WebSearchProvider.GOOGLE, request -> {
                throw new IllegalStateException("403 Forbidden: secret-g-key not allowed");
            });

            assertThatThrownBy(() -> WebSearchService.search(google, "q"))
                    .isInstanceOfSatisfying(WebSearchProviderException.class, e -> {
                        assertThat(e.kind()).isEqualTo(WebSearchProviderException.Kind.FAILED);
                        assertThat(e.getMessage()).contains("Google Custom Search")
                                .contains("January 1, 2027").contains("Keenable")
                                .doesNotContain("secret-g-key");
                        assertThat(e.getCause()).isInstanceOf(IllegalStateException.class);
                    });
        }

        @Test
        void nonDeprecatedProviders_areNotWrapped() {
            IllegalStateException original = new IllegalStateException("tavily boom");
            WebSearchServiceTestSupport.replaceEngine(WebSearchProvider.TAVILY, request -> {
                throw original;
            });
            WebSearchConfig tavily = new WebSearchConfig(WebSearchProvider.TAVILY,
                    Map.of(CredentialKey.TAVILY_SEARCH_KEY, "t"), 3);

            assertThatThrownBy(() -> WebSearchService.search(tavily, "q")).isSameAs(original);
        }
    }

    @Nested
    class FromState {

        @Test
        void readsOnlyTheSelectedProvidersCredentials() {
            DevoxxGenieStateService state = new DevoxxGenieStateService();
            state.setTavilySearchKey("tvly-123");
            state.setGoogleSearchKey("g-key");
            state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
            state.setMaxSearchResults(7);

            WebSearchConfig config = WebSearchConfig.fromState(state);

            assertThat(config.provider()).isEqualTo(WebSearchProvider.TAVILY);
            assertThat(config.credential(CredentialKey.TAVILY_SEARCH_KEY)).isEqualTo("tvly-123");
            assertThat(config.credentials()).doesNotContainKey(CredentialKey.GOOGLE_SEARCH_KEY);
            assertThat(config.maxResults()).isEqualTo(7);
        }

        @Test
        void noProviderSelected_readsNoCredentials() {
            DevoxxGenieStateService state = new DevoxxGenieStateService();
            state.setTavilySearchKey("tvly-123");
            state.setSelectedWebSearchProvider(null);

            WebSearchConfig config = WebSearchConfig.fromState(state);

            assertThat(config.provider()).isNull();
            assertThat(config.credentials()).isEmpty();
        }

        @Test
        void missingMaxSearchResults_defaultsToFive() {
            DevoxxGenieStateService state = new DevoxxGenieStateService();
            state.setMaxSearchResults(null);

            assertThat(WebSearchConfig.fromState(state).maxResults()).isEqualTo(5);
        }
    }

    @Nested
    class Enablement {

        @Test
        void isEnabled_followsTheMasterSwitch() {
            DevoxxGenieStateService state = new DevoxxGenieStateService();
            state.setIsWebSearchEnabled(true);
            assertThat(WebSearchService.isEnabled(state)).isTrue();
            state.setIsWebSearchEnabled(false);
            assertThat(WebSearchService.isEnabled(state)).isFalse();
            state.setIsWebSearchEnabled(null);
            assertThat(WebSearchService.isEnabled(state)).isFalse();
        }

        @Test
        void analyticsFeature_isTheSelectedProvidersFeature_orNull() {
            DevoxxGenieStateService state = new DevoxxGenieStateService();
            state.setSelectedWebSearchProvider(WebSearchProvider.GOOGLE);
            assertThat(WebSearchService.selectedAnalyticsFeature(state)).isEqualTo(FeatureId.WEB_SEARCH_GOOGLE);
            state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
            assertThat(WebSearchService.selectedAnalyticsFeature(state)).isEqualTo(FeatureId.WEB_SEARCH_TAVILY);
            state.setSelectedWebSearchProvider(null);
            assertThat(WebSearchService.selectedAnalyticsFeature(state)).isNull();
        }
    }

    // --- helpers --------------------------------------------------------------------------------

    static WebSearchProviderDefinition definitionWithEngine(WebSearchProvider provider, WebSearchEngine engine) {
        return definitionWithEngine(provider, engine, new ArrayList<>());
    }

    static WebSearchProviderDefinition definitionWithEngine(WebSearchProvider provider,
                                                            WebSearchEngine engine,
                                                            List<WebSearchProvider> built) {
        return WebSearchProviderRegistry.definition(provider).withEngineFactory(config -> {
            built.add(provider);
            return engine;
        });
    }

    static WebSearchResults results(String title, String url) {
        return WebSearchResults.from(WebSearchInformationResult.from(1L),
                List.of(WebSearchOrganicResult.from(title, URI.create(url), "snippet of " + title, null)));
    }
}
