package com.devoxx.genie.service.websearch;

import com.devoxx.genie.service.analytics.FeatureId;
import com.devoxx.genie.service.credentials.CredentialKey;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The web search providers DevoxxGenie supports. To add one: add a {@link WebSearchProvider} constant,
 * one definition here (with its engine factory), a {@link CredentialKey} per credential and a
 * {@link FeatureId} in the analytics allowlist. {@code WebSearchProviderRegistryTest.everyDefinition_isFullyWired}
 * fails until all of that is in place.
 *
 * <p>Shared code must go through {@link WebSearchService} rather than naming providers.
 */
public final class WebSearchProviderRegistry {

    private static final WebSearchProviderDefinition TAVILY = new WebSearchProviderDefinition(
            WebSearchProvider.TAVILY,
            "Tavily",
            "https://app.tavily.com/home",
            "Search API built for LLMs. Create a free API key at app.tavily.com.",
            List.of(new WebSearchCredentialField(CredentialKey.TAVILY_SEARCH_KEY, "API key",
                    "https://app.tavily.com/home", true)),
            FeatureId.WEB_SEARCH_TAVILY,
            config -> tavilyEngine(config, null));

    private static final WebSearchProviderDefinition GOOGLE = new WebSearchProviderDefinition(
            WebSearchProvider.GOOGLE,
            "Google Custom Search",
            "https://developers.google.com/custom-search/v1/overview",
            "Google's Custom Search JSON API (closed to new customers). Needs an API key and a Programmable Search Engine ID.",
            List.of(
                    new WebSearchCredentialField(CredentialKey.GOOGLE_SEARCH_KEY, "API key",
                            "https://developers.google.com/custom-search/docs/paid_element#api_key", true),
                    new WebSearchCredentialField(CredentialKey.GOOGLE_CSI_KEY, "Search Engine ID",
                            "https://programmablesearchengine.google.com/controlpanel/create", true)),
            FeatureId.WEB_SEARCH_GOOGLE,
            config -> GoogleCustomWebSearchEngine.builder()
                    .apiKey(config.credential(CredentialKey.GOOGLE_SEARCH_KEY))
                    .csi(config.credential(CredentialKey.GOOGLE_CSI_KEY))
                    .build(),
            // Confirmed on Google's overview page (2026-11-18): closed to new customers, discontinued 2027-01-01.
            new WebSearchDeprecation(LocalDate.of(2027, 1, 1),
                    "https://developers.google.com/custom-search/v1/overview",
                    "Keenable or Tavily"));

    // Engine contributed in #1430. The key is optional: without one Keenable's free public endpoint is used.
    private static final WebSearchProviderDefinition KEENABLE = new WebSearchProviderDefinition(
            WebSearchProvider.KEENABLE,
            "Keenable",
            "https://keenable.ai",
            "Works without an API key (free, rate-limited per network). Add a key for higher limits.",
            List.of(new WebSearchCredentialField(CredentialKey.KEENABLE_SEARCH_KEY, "API key (optional)",
                    "https://keenable.ai", false)),
            FeatureId.WEB_SEARCH_KEENABLE,
            config -> keenableEngine(config, null));

    // Parallel's REST Search API (not its MCP server); requires a key (TASK-277.5, #1423).
    private static final WebSearchProviderDefinition PARALLEL = new WebSearchProviderDefinition(
            WebSearchProvider.PARALLEL,
            "Parallel",
            "https://platform.parallel.ai",
            "Web search API built for AI agents. Needs a Parallel API key; searches are billed to your Parallel account.",
            List.of(new WebSearchCredentialField(CredentialKey.PARALLEL_SEARCH_KEY, "API key",
                    "https://platform.parallel.ai", true)),
            FeatureId.WEB_SEARCH_PARALLEL,
            config -> parallelEngine(config, null));

    private static final Map<WebSearchProvider, WebSearchProviderDefinition> DEFINITIONS =
            byProvider(TAVILY, GOOGLE, KEENABLE, PARALLEL);

    private WebSearchProviderRegistry() {
    }

    /** The definition for {@code provider}; every enum constant has one. */
    public static @NotNull WebSearchProviderDefinition definition(@NotNull WebSearchProvider provider) {
        WebSearchProviderDefinition definition = DEFINITIONS.get(provider);
        if (definition == null) {
            throw new IllegalStateException("No web search provider definition for " + provider);
        }
        return definition;
    }

    /** All definitions, in {@link WebSearchProvider} order (the order shown in settings). */
    public static @NotNull List<WebSearchProviderDefinition> all() {
        return Arrays.stream(WebSearchProvider.values()).map(WebSearchProviderRegistry::definition).toList();
    }

    /** Builds a Tavily engine; {@code baseUrl} is null in production and points at a stub server in tests. */
    static @NotNull TavilyWebSearchEngine tavilyEngine(@NotNull WebSearchConfig config, String baseUrl) {
        TavilyWebSearchEngine.TavilyWebSearchEngineBuilder builder = TavilyWebSearchEngine.builder()
                .apiKey(config.credential(CredentialKey.TAVILY_SEARCH_KEY));
        if (baseUrl != null) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }

    /** Builds a Keenable engine; {@code baseUrl} is null in production and points at a stub server in tests. */
    static @NotNull KeenableWebSearchEngine keenableEngine(@NotNull WebSearchConfig config, String baseUrl) {
        return new KeenableWebSearchEngine(config.credential(CredentialKey.KEENABLE_SEARCH_KEY),
                baseUrl == null ? KeenableWebSearchEngine.DEFAULT_BASE_URL : baseUrl,
                KeenableWebSearchEngine.defaultHttpClient());
    }

    /** Builds a Parallel engine; {@code baseUrl} is null in production and points at a stub server in tests. */
    static @NotNull ParallelWebSearchEngine parallelEngine(@NotNull WebSearchConfig config, String baseUrl) {
        return new ParallelWebSearchEngine(config.credential(CredentialKey.PARALLEL_SEARCH_KEY),
                baseUrl == null ? ParallelWebSearchEngine.DEFAULT_BASE_URL : baseUrl,
                WebSearchHttp.defaultHttpClient());
    }

    private static Map<WebSearchProvider, WebSearchProviderDefinition> byProvider(WebSearchProviderDefinition... definitions) {
        Map<WebSearchProvider, WebSearchProviderDefinition> map = new EnumMap<>(WebSearchProvider.class);
        for (WebSearchProviderDefinition definition : definitions) {
            map.put(definition.provider(), definition);
        }
        return map;
    }
}
