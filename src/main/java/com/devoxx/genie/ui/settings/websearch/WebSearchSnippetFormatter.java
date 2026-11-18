package com.devoxx.genie.ui.settings.websearch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Turns a provider's raw result snippet into short plain text for the settings Test search
 * preview. Providers often return page extracts in Markdown; the preview is not a Markdown
 * renderer, so the syntax is stripped rather than shown.
 */
final class WebSearchSnippetFormatter {

    /** Longest snippet shown, in characters, before it is cut with an ellipsis. */
    static final int MAX_LENGTH = 280;

    private static final Pattern IMAGE = Pattern.compile("!\\[[^\\]]*]\\([^)]*\\)");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)]\\([^)]*\\)");
    private static final Pattern HEADING = Pattern.compile("(?m)^\\s{0,3}#{1,6}\\s+");
    private static final Pattern LIST_MARKER = Pattern.compile("(?m)^\\s*(?:[-*+]|\\d+[.)])\\s+");
    private static final Pattern QUOTE = Pattern.compile("(?m)^\\s*>\\s?");
    private static final Pattern STRONG = Pattern.compile("(\\*\\*|__)(?=\\S)(.+?)(?<=\\S)\\1");
    private static final Pattern STRIKE = Pattern.compile("~~(?=\\S)(.+?)(?<=\\S)~~");
    private static final Pattern EM_STAR = Pattern.compile("(?<![\\w*])\\*(?=\\S)(.+?)(?<=\\S)\\*(?![\\w*])");
    private static final Pattern EM_UNDERSCORE = Pattern.compile("(?<![\\w_])_(?=\\S)(.+?)(?<=\\S)_(?![\\w_])");
    private static final Pattern CODE = Pattern.compile("`([^`]*)`");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private WebSearchSnippetFormatter() {
    }

    /** Plain, single-paragraph text of at most {@link #MAX_LENGTH} characters plus an ellipsis. */
    static @NotNull String clean(@Nullable String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return "";
        }
        String text = snippet;
        text = IMAGE.matcher(text).replaceAll("");
        text = LINK.matcher(text).replaceAll("$1");
        text = HEADING.matcher(text).replaceAll("");
        text = LIST_MARKER.matcher(text).replaceAll("");
        text = QUOTE.matcher(text).replaceAll("");
        text = CODE.matcher(text).replaceAll("$1");
        text = STRONG.matcher(text).replaceAll("$2");
        text = STRIKE.matcher(text).replaceAll("$1");
        text = EM_STAR.matcher(text).replaceAll("$1");
        text = EM_UNDERSCORE.matcher(text).replaceAll("$1");
        text = WHITESPACE.matcher(text).replaceAll(" ").strip();
        return truncate(text);
    }

    private static @NotNull String truncate(@NotNull String text) {
        if (text.length() <= MAX_LENGTH) {
            return text;
        }
        String cut = text.substring(0, MAX_LENGTH);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > MAX_LENGTH / 2) {
            cut = cut.substring(0, lastSpace);
        }
        return cut.stripTrailing() + "…";
    }

    /** The URL's host without a leading {@code www.}, or the input itself when it has no host. */
    static @NotNull String domain(@Nullable String url) {
        if (url == null) {
            return "";
        }
        try {
            String host = URI.create(url.strip()).getHost();
            if (host == null) {
                return url;
            }
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (IllegalArgumentException e) {
            return url;
        }
    }

    /** Only web pages are opened from results; a result URL is untrusted provider data. */
    static boolean isOpenable(@Nullable String url) {
        if (url == null) {
            return false;
        }
        String lower = url.strip().toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://");
    }
}
