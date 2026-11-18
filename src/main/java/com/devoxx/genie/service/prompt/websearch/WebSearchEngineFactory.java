package com.devoxx.genie.service.prompt.websearch;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the web search engine selected in Settings. Existing Tavily and Google
 * behaviour is unchanged; Keenable is used only when the user enables it and
 * neither of the other providers is selected.
 */
public final class WebSearchEngineFactory {

    private WebSearchEngineFactory() {
    }

    public static boolean isConfigured(@NotNull DevoxxGenieStateService state) {
        return tavilyReady(state) || googleReady(state) || keenableReady(state);
    }

    public static @Nullable WebSearchEngine create(@NotNull DevoxxGenieStateService state) {
        if (tavilyReady(state)) {
            return TavilyWebSearchEngine.builder()
                    .apiKey(state.getTavilySearchKey())
                    .build();
        }
        if (googleReady(state)) {
            return GoogleCustomWebSearchEngine.builder()
                    .apiKey(state.getGoogleSearchKey())
                    .csi(state.getGoogleCSIKey())
                    .build();
        }
        if (keenableReady(state)) {
            return new KeenableWebSearchEngine(state.getKeenableSearchKey());
        }
        return null;
    }

    private static boolean tavilyReady(DevoxxGenieStateService state) {
        return state.isTavilySearchEnabled()
                && state.getTavilySearchKey() != null && !state.getTavilySearchKey().isBlank();
    }

    private static boolean googleReady(DevoxxGenieStateService state) {
        return state.isGoogleSearchEnabled()
                && state.getGoogleSearchKey() != null && !state.getGoogleSearchKey().isBlank()
                && state.getGoogleCSIKey() != null && !state.getGoogleCSIKey().isBlank();
    }

    private static boolean keenableReady(DevoxxGenieStateService state) {
        return state.isKeenableSearchEnabled();
    }
}
