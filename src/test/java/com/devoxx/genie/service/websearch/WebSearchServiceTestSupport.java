package com.devoxx.genie.service.websearch;

import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchInformationResult;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchResults;

import java.net.URI;
import java.util.List;

/** Shared helpers for web search tests in other packages. Call {@link #reset()} (or close the stub) after each test. */
public final class WebSearchServiceTestSupport {

    private WebSearchServiceTestSupport() {
    }

    /** Substitutes {@code provider}'s engine, e.g. Google, whose real engine cannot target a stub server. */
    public static void replaceEngine(WebSearchProvider provider, WebSearchEngine engine) {
        WebSearchService.replaceEngineForTesting(provider, engine);
    }

    public static void reset() {
        WebSearchService.resetDefinitionsForTesting();
    }

    public static WebSearchResults results(String title, String url) {
        return WebSearchResults.from(WebSearchInformationResult.from(1L),
                List.of(WebSearchOrganicResult.from(title, URI.create(url), "snippet of " + title, null)));
    }
}
