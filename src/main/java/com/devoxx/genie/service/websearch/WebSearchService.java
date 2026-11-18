package com.devoxx.genie.service.websearch;

import com.devoxx.genie.service.analytics.FeatureId;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The single place that decides which web search provider is in use and whether it can run.
 * {@code /search}, the chat Web toggle, the agent {@code web_search} tool, the settings Test search,
 * the agent settings warning and analytics all go through here, so they apply the same rules.
 *
 * <p>Static by design (like {@code LocalProviderRegistry}): no IntelliJ service lookup, so it works
 * in plain unit tests that mock {@code ApplicationManager}.
 */
public final class WebSearchService {

    private static volatile Function<WebSearchProvider, WebSearchProviderDefinition> definitions =
            WebSearchProviderRegistry::definition;

    private WebSearchService() {
    }

    /** The master "Enable feature" switch in Settings → Web search (gates {@code /search}). */
    public static boolean isEnabled(@NotNull DevoxxGenieStateService state) {
        return Boolean.TRUE.equals(state.getIsWebSearchEnabled());
    }

    /** True when a provider is selected; reads no credentials, so it is cheap enough for every prompt. */
    public static boolean isProviderSelected(@NotNull DevoxxGenieStateService state) {
        return state.getSelectedWebSearchProvider() != null;
    }

    /** The definition currently in effect for {@code provider} (the registry's, unless a test overrode it). */
    public static @NotNull WebSearchProviderDefinition definition(@NotNull WebSearchProvider provider) {
        return definitions.apply(provider);
    }

    public static @NotNull WebSearchReadiness readiness(@NotNull WebSearchConfig config) {
        WebSearchProvider provider = config.provider();
        if (provider == null) {
            return new WebSearchReadiness(WebSearchReadiness.Status.NO_PROVIDER, null, List.of());
        }
        List<String> missing = new ArrayList<>();
        for (WebSearchCredentialField field : definition(provider).fields()) {
            if (field.required() && config.credential(field.key()).isEmpty()) {
                missing.add(field.label());
            }
        }
        return missing.isEmpty()
                ? new WebSearchReadiness(WebSearchReadiness.Status.READY, provider, List.of())
                : new WebSearchReadiness(WebSearchReadiness.Status.MISSING_CREDENTIALS, provider, missing);
    }

    /**
     * Builds the selected provider's engine.
     *
     * @throws WebSearchNotConfiguredException when no provider is selected or credentials are missing;
     *                                         no engine is built and no request is sent in that case
     */
    public static @NotNull WebSearchEngine createEngine(@NotNull WebSearchConfig config) {
        WebSearchReadiness readiness = readiness(config);
        if (!readiness.isReady()) {
            throw new WebSearchNotConfiguredException(readiness);
        }
        return definition(config.provider()).engineFactory().create(config);
    }

    /** Runs one search with the selected provider. */
    public static @NotNull WebSearchResults search(@NotNull WebSearchConfig config, @NotNull String query) {
        return createEngine(config).search(WebSearchRequest.builder()
                .searchTerms(query)
                .maxResults(config.maxResults())
                .build());
    }

    /** The analytics feature for the selected provider, or null when none is selected. */
    public static @Nullable FeatureId selectedAnalyticsFeature(@NotNull DevoxxGenieStateService state) {
        WebSearchProvider provider = state.getSelectedWebSearchProvider();
        return provider == null ? null : definition(provider).analyticsFeature();
    }

    @TestOnly
    public static void overrideDefinitionsForTesting(
            @NotNull Function<WebSearchProvider, WebSearchProviderDefinition> override) {
        definitions = override;
    }

    /** Points the real Tavily engine at {@code baseUrl} (a local stub server); other providers are unchanged. */
    @TestOnly
    public static void useTavilyBaseUrlForTesting(@NotNull String baseUrl) {
        replaceEngineFactoryForTesting(WebSearchProvider.TAVILY,
                config -> WebSearchProviderRegistry.tavilyEngine(config, baseUrl));
    }

    /** Points the real Keenable engine at {@code baseUrl} (a local stub server); other providers are unchanged. */
    @TestOnly
    public static void useKeenableBaseUrlForTesting(@NotNull String baseUrl) {
        replaceEngineFactoryForTesting(WebSearchProvider.KEENABLE,
                config -> WebSearchProviderRegistry.keenableEngine(config, baseUrl));
    }

    /**
     * Replaces one provider's engine (keeping any other override in place, e.g. a stub base URL).
     * Used for providers whose langchain4j engine has no base-URL setting, such as Google.
     */
    @TestOnly
    public static void replaceEngineForTesting(@NotNull WebSearchProvider target, @NotNull WebSearchEngine engine) {
        replaceEngineFactoryForTesting(target, config -> engine);
    }

    private static void replaceEngineFactoryForTesting(@NotNull WebSearchProvider target,
                                                       @NotNull WebSearchProviderDefinition.EngineFactory factory) {
        Function<WebSearchProvider, WebSearchProviderDefinition> current = definitions;
        overrideDefinitionsForTesting(provider -> {
            WebSearchProviderDefinition base = current.apply(provider);
            if (provider != target) {
                return base;
            }
            return new WebSearchProviderDefinition(base.provider(), base.displayName(), base.signupUrl(),
                    base.hint(), base.fields(), base.analyticsFeature(), factory);
        });
    }

    @TestOnly
    public static void resetDefinitionsForTesting() {
        definitions = WebSearchProviderRegistry::definition;
    }
}
