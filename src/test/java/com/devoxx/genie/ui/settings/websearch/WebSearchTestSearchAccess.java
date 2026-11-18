package com.devoxx.genie.ui.settings.websearch;

import com.devoxx.genie.service.websearch.WebSearchConfig;

/** Test-only bridge: runs the settings Test search from end-to-end tests in other packages. */
public final class WebSearchTestSearchAccess {

    private WebSearchTestSearchAccess() {
    }

    public static String run(WebSearchConfig config, String query) {
        return WebSearchProvidersComponent.performTestSearch(config, query);
    }
}
