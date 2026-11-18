package com.devoxx.genie.ui.settings.websearch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebSearchSnippetFormatterTest {

    @Test
    void plainSnippetIsKeptVerbatim() {
        assertThat(WebSearchSnippetFormatter.clean("Snippet from the stub.")).isEqualTo("Snippet from the stub.");
    }

    @Test
    void nullOrBlankBecomesEmpty() {
        assertThat(WebSearchSnippetFormatter.clean(null)).isEmpty();
        assertThat(WebSearchSnippetFormatter.clean("  \n\t ")).isEmpty();
    }

    @Test
    void markdownLinksKeepOnlyTheirText() {
        assertThat(WebSearchSnippetFormatter.clean(
                "Generated from the [CHANGELOG.md on GitHub](https://github.com/x/y/blob/main/CHANGELOG.md)."))
                .isEqualTo("Generated from the CHANGELOG.md on GitHub.");
    }

    @Test
    void imagesAreDropped() {
        assertThat(WebSearchSnippetFormatter.clean("Logo ![alt text](https://x/logo.png) here"))
                .isEqualTo("Logo here");
    }

    @Test
    void emphasisAndInlineCodeMarkersAreStripped() {
        assertThat(WebSearchSnippetFormatter.clean("**Added** __bold__ *it* _em_ ~~old~~ run `claude --version`"))
                .isEqualTo("Added bold it em old run claude --version");
    }

    @Test
    void underscoresInsideWordsAreKept() {
        assertThat(WebSearchSnippetFormatter.clean("use snake_case_names and 2*3*4"))
                .isEqualTo("use snake_case_names and 2*3*4");
    }

    @Test
    void headingsListMarkersAndQuotesAreStripped() {
        assertThat(WebSearchSnippetFormatter.clean("## Release notes\n- First item\n* Second item\n1. Third\n> quoted"))
                .isEqualTo("Release notes First item Second item Third quoted");
    }

    @Test
    void whitespaceAndNewlinesCollapseToSingleSpaces() {
        assertThat(WebSearchSnippetFormatter.clean("November 16, 2026\n\n\n  Added   support"))
                .isEqualTo("November 16, 2026 Added support");
    }

    @Test
    void longSnippetIsCutAtAWordBoundaryWithEllipsis() {
        String word = "abcdefghi "; // 10 chars
        String clean = WebSearchSnippetFormatter.clean(word.repeat(100));

        assertThat(clean).endsWith("…");
        assertThat(clean.length()).isLessThanOrEqualTo(WebSearchSnippetFormatter.MAX_LENGTH + 1);
        assertThat(clean.substring(0, clean.length() - 1)).endsWith("abcdefghi");
    }

    @Test
    void singleHugeWordIsCutHard() {
        String clean = WebSearchSnippetFormatter.clean("x".repeat(1000));

        assertThat(clean).hasSize(WebSearchSnippetFormatter.MAX_LENGTH + 1).endsWith("…");
    }

    @Test
    void domainIsTheHostWithoutWww() {
        assertThat(WebSearchSnippetFormatter.domain("https://www.example.com/a/b?c=d")).isEqualTo("example.com");
        assertThat(WebSearchSnippetFormatter.domain("https://docs.claude.com/en/docs")).isEqualTo("docs.claude.com");
    }

    @Test
    void domainFallsBackForOddUrls() {
        assertThat(WebSearchSnippetFormatter.domain(null)).isEmpty();
        assertThat(WebSearchSnippetFormatter.domain("not a url")).isEqualTo("not a url");
    }

    @Test
    void onlyHttpUrlsAreOpenable() {
        assertThat(WebSearchSnippetFormatter.isOpenable("https://example.com")).isTrue();
        assertThat(WebSearchSnippetFormatter.isOpenable("HTTP://example.com")).isTrue();
        assertThat(WebSearchSnippetFormatter.isOpenable("file:///etc/passwd")).isFalse();
        assertThat(WebSearchSnippetFormatter.isOpenable("javascript:alert(1)")).isFalse();
        assertThat(WebSearchSnippetFormatter.isOpenable(null)).isFalse();
    }
}
