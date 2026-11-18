package com.devoxx.genie.service.websearch;

import dev.langchain4j.web.search.WebSearchOrganicResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Renders the "Sources" list appended to {@code /search} answers (TASK-277.2), so users can open the pages an
 * answer is based on. Provider-independent: it only reads {@link WebSearchOrganicResult}.
 */
public final class WebSearchSources {

    private WebSearchSources() {
    }

    /**
     * A markdown "Sources" block with one numbered link per result, or "" when no result has an http(s) URL.
     * Titles fall back to the URL's host; duplicate URLs are listed once.
     */
    public static @NotNull String toMarkdown(@Nullable List<WebSearchOrganicResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        StringBuilder list = new StringBuilder();
        Set<String> seen = new LinkedHashSet<>();
        int number = 1;
        for (WebSearchOrganicResult result : results) {
            String url = result == null ? null : linkableUrl(result.url());
            if (url == null || !seen.add(url)) {
                continue;
            }
            list.append(number++).append(". [").append(escapeLinkText(label(result))).append("](")
                    .append(url).append(")\n");
        }
        return list.isEmpty() ? "" : "**Sources**\n" + list;
    }

    /** The answer shown when the provider returns no results. */
    public static @NotNull String noResultsMessage(@NotNull String query) {
        return "No web results found for \"" + query.trim() + "\". Try different keywords, or check the provider in "
                + WebSearchReadiness.SETTINGS_PATH + ".";
    }

    /** The URL as a markdown link target, or null when it is missing or not http(s). */
    static @Nullable String linkableUrl(@Nullable URI uri) {
        if (uri == null || uri.getScheme() == null || uri.getHost() == null) {
            return null;
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            return null;
        }
        // Parentheses and spaces would end or break the markdown link target.
        return uri.toString().replace("(", "%28").replace(")", "%29").replace(" ", "%20");
    }

    private static @NotNull String label(@NotNull WebSearchOrganicResult result) {
        String title = result.title();
        if (title != null && !title.isBlank()) {
            return title.replaceAll("\\s+", " ").trim();
        }
        return result.url().getHost();
    }

    /** Escapes characters that would end the link text or be read as emphasis. */
    private static @NotNull String escapeLinkText(@NotNull String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if ("\\[]*_`".indexOf(c) >= 0) {
                escaped.append('\\');
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}
