package com.devoxx.genie.ui.settings.websearch;

import com.devoxx.genie.service.websearch.WebSearchConfig;
import com.devoxx.genie.ui.settings.common.SearchTestOutcome;

/** Test-only bridge: runs the settings Test search from end-to-end tests in other packages. */
public final class WebSearchTestSearchAccess {

    private WebSearchTestSearchAccess() {
    }

    /** The outcome as text: the status line, then each hit's title, URL and cleaned snippet. */
    public static String run(WebSearchConfig config, String query) {
        SearchTestOutcome<WebSearchTestSearch.Hit> outcome = WebSearchTestSearch.search(config, query);
        StringBuilder text = new StringBuilder(outcome.message());
        for (WebSearchTestSearch.Hit hit : outcome.items()) {
            text.append("\n").append(hit.title()).append("\n").append(hit.url()).append("\n").append(hit.snippet());
        }
        return text.toString();
    }
}
