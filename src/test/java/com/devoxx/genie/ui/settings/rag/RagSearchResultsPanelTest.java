package com.devoxx.genie.ui.settings.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RagSearchResultsPanelTest {

    private static final String BASE = "/Users/me/project";

    @Test
    void pathsAreShownRelativeToTheProject() {
        assertThat(RagSearchResultsPanel.relativePath(BASE, "/Users/me/project/src/main/A.java"))
                .isEqualTo("src/main/A.java");
        assertThat(RagSearchResultsPanel.relativePath(BASE + "/", "/Users/me/project/A.java"))
                .isEqualTo("A.java");
    }

    @Test
    void windowsPathsAreShownRelativeToo() {
        assertThat(RagSearchResultsPanel.relativePath("C:\\work\\project", "C:\\work\\project\\src\\A.java"))
                .isEqualTo("src/A.java");
    }

    @Test
    void siblingDirectoryWithTheSamePrefixKeepsItsFullPath() {
        assertThat(RagSearchResultsPanel.relativePath(BASE, "/Users/me/project-old/A.java"))
                .isEqualTo("/Users/me/project-old/A.java");
    }

    @Test
    void missingValuesDoNotBreakARow() {
        assertThat(RagSearchResultsPanel.relativePath(BASE, null)).isEmpty();
        assertThat(RagSearchResultsPanel.relativePath(null, "/a/B.java")).isEqualTo("/a/B.java");
        assertThat(RagSearchResultsPanel.formatScore(null)).isEqualTo("-");
    }

    @Test
    void scoreUsesTwoDecimalsInEveryLocale() {
        assertThat(RagSearchResultsPanel.formatScore(0.88461)).isEqualTo("0.88");
    }

    @Test
    void summaryCountsMatches() {
        assertThat(RagSearchResultsPanel.summary("action button", 1)).startsWith("1 match for \"action button\"");
        assertThat(RagSearchResultsPanel.summary("action button", 5)).startsWith("5 matches for");
    }

    @Test
    void noMatchesMessageSaysWhatToTry() {
        assertThat(RagSearchResultsPanel.noMatchesMessage("x"))
                .contains("\"x\"").contains("indexed").contains("minimum score");
    }
}
