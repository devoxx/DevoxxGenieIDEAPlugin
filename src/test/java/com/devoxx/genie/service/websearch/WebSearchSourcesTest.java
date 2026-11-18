package com.devoxx.genie.service.websearch;

import dev.langchain4j.web.search.WebSearchOrganicResult;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** TASK-277.2: the "Sources" list appended to /search answers. */
class WebSearchSourcesTest {

    // langchain4j requires a URL on every organic result, so "not linkable" means a non-http(s) URL.
    private static WebSearchOrganicResult hit(String title, String url) {
        return WebSearchOrganicResult.from(title, URI.create(url), "snippet", null);
    }

    private static List<WebSearchOrganicResult> hits(WebSearchOrganicResult... results) {
        return Arrays.asList(results);
    }

    @Test
    void sourcesPresent_rendersANumberedListOfMarkdownLinks() {
        String markdown = WebSearchSources.toMarkdown(hits(
                hit("Build number ranges", "https://plugins.jetbrains.com/docs/build-number-ranges.html"),
                hit("plugin.xml reference", "https://plugins.jetbrains.com/docs/plugin-configuration-file.html")));

        assertThat(markdown).isEqualTo("""
                **Sources**
                1. [Build number ranges](https://plugins.jetbrains.com/docs/build-number-ranges.html)
                2. [plugin.xml reference](https://plugins.jetbrains.com/docs/plugin-configuration-file.html)
                """);
    }

    /**
     * langchain4j's factory rejects a missing title, but engines built in this repo (and subclasses) may
     * still hand one over, so the fallback is exercised with a mocked result.
     */
    private static WebSearchOrganicResult hitWithTitle(String title, String url) {
        WebSearchOrganicResult result = org.mockito.Mockito.mock(WebSearchOrganicResult.class);
        org.mockito.Mockito.when(result.title()).thenReturn(title);
        org.mockito.Mockito.when(result.url()).thenReturn(URI.create(url));
        return result;
    }

    @Test
    void missingTitle_fallsBackToTheHost() {
        assertThat(WebSearchSources.toMarkdown(hits(hitWithTitle(null, "https://www.example.com/a/b?c=d"))))
                .contains("1. [www.example.com](https://www.example.com/a/b?c=d)");
    }

    @Test
    void blankTitle_fallsBackToTheHost() {
        assertThat(WebSearchSources.toMarkdown(hits(hitWithTitle("   ", "https://example.com/x"))))
                .contains("1. [example.com](https://example.com/x)");
    }

    @Test
    void titleWithMarkdownLinkSyntax_isEscaped_soTheLinkStaysIntact() {
        assertThat(WebSearchSources.toMarkdown(hits(hit("[Docs] Release *notes*", "https://example.com/r"))))
                .contains("1. [\\[Docs\\] Release \\*notes\\*](https://example.com/r)");
    }

    @Test
    void titleWithNewlines_isFlattenedToOneLine() {
        assertThat(WebSearchSources.toMarkdown(hits(hit("Line one\nLine two", "https://example.com/n"))))
                .contains("1. [Line one Line two](https://example.com/n)");
    }

    @Test
    void urlWithParentheses_isEncoded_soTheLinkIsNotCutShort() {
        assertThat(WebSearchSources.toMarkdown(hits(hit("Wiki", "https://en.wikipedia.org/wiki/Java_(language)"))))
                .contains("1. [Wiki](https://en.wikipedia.org/wiki/Java_%28language%29)");
    }

    @Test
    void resultsWithoutAnHttpUrl_areSkipped_andNumberingStaysContinuous() {
        String markdown = WebSearchSources.toMarkdown(hits(
                hit("Ftp", "ftp://example.com/file"),
                hit("Mail", "mailto:someone@example.com"),
                hit("Script", "javascript:alert(1)"),
                hit("Kept", "http://example.com/kept"),
                null));

        assertThat(markdown).isEqualTo("""
                **Sources**
                1. [Kept](http://example.com/kept)
                """);
    }

    @Test
    void duplicateUrls_areListedOnce() {
        String markdown = WebSearchSources.toMarkdown(hits(
                hit("First", "https://example.com/same"),
                hit("Second", "https://example.com/same")));

        assertThat(markdown).contains("1. [First](https://example.com/same)").doesNotContain("2.");
    }

    @Test
    void noLinkableResults_rendersNothing() {
        assertThat(WebSearchSources.toMarkdown(hits(hit("Ftp only", "ftp://example.com/f")))).isEmpty();
        assertThat(WebSearchSources.toMarkdown(List.of())).isEmpty();
        assertThat(WebSearchSources.toMarkdown(null)).isEmpty();
    }

    @Test
    void noResultsMessage_namesTheQuery_andSuggestsWhatToTry() {
        assertThat(WebSearchSources.noResultsMessage("kotlin 3.0 release date"))
                .contains("No web results found for \"kotlin 3.0 release date\"")
                .contains("different keywords")
                .contains("Settings → DevoxxGenie → Web search");
    }
}
