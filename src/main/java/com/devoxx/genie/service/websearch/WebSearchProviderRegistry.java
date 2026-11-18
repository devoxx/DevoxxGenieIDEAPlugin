package com.devoxx.genie.service.websearch;

import com.devoxx.genie.service.analytics.FeatureId;
import com.devoxx.genie.service.credentials.CredentialKey;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import org.jetbrains.annotations.NotNull;

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
            "Google's Custom Search JSON API. Needs an API key and a Programmable Search Engine ID.",
            List.of(
                    new WebSearchCredentialField(CredentialKey.GOOGLE_SEARCH_KEY, "API key",
                            "https://developers.google.com/custom-search/docs/paid_element#api_key", true),
                    new WebSearchCredentialField(CredentialKey.GOOGLE_CSI_KEY, "Search Engine ID",
                            "https://programmablesearchengine.google.com/controlpanel/create", true)),
            FeatureId.WEB_SEARCH_GOOGLE,
            config -> GoogleCustomWebSearchEngine.builder()
                    .apiKey(config.credential(CredentialKey.GOOGLE_SEARCH_KEY))
                    .csi(config.credential(CredentialKey.GOOGLE_CSI_KEY))
                    .build());

    private static final Map<WebSearchProvider, WebSearchProviderDefinition> DEFINITIONS = byProvider(TAVILY, GOOGLE);

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

    private static Map<WebSearchProvider, WebSearchProviderDefinition> byProvider(WebSearchProviderDefinition... definitions) {
        Map<WebSearchProvider, WebSearchProviderDefinition> map = new EnumMap<>(WebSearchProvider.class);
        for (WebSearchProviderDefinition definition : definitions) {
            map.put(definition.provider(), definition);
        }
        return map;
    }
}
