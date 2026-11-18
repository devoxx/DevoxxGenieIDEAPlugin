package com.devoxx.genie.ui.settings.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RagSearchTestAdapterTest {

    private static final String BASE = "/Users/me/project";

    @Test
    void pathsAreShownRelativeToTheProject() {
        assertThat(RagSearchTestAdapter.relativePath(BASE, "/Users/me/project/src/main/A.java"))
                .isEqualTo("src/main/A.java");
        assertThat(RagSearchTestAdapter.relativePath(BASE + "/", "/Users/me/project/A.java"))
                .isEqualTo("A.java");
    }

    @Test
    void windowsPathsAreShownRelativeToo() {
        assertThat(RagSearchTestAdapter.relativePath("C:\\work\\project", "C:\\work\\project\\src\\A.java"))
                .isEqualTo("src/A.java");
    }

    @Test
    void siblingDirectoryWithTheSamePrefixKeepsItsFullPath() {
        assertThat(RagSearchTestAdapter.relativePath(BASE, "/Users/me/project-old/A.java"))
                .isEqualTo("/Users/me/project-old/A.java");
    }

    @Test
    void missingValuesDoNotBreakARow() {
        assertThat(RagSearchTestAdapter.relativePath(BASE, null)).isEmpty();
        assertThat(RagSearchTestAdapter.relativePath(null, "/a/B.java")).isEqualTo("/a/B.java");
        assertThat(RagSearchTestAdapter.formatScore(null)).isEqualTo("-");
    }

    @Test
    void scoreUsesTwoDecimalsInEveryLocale() {
        assertThat(RagSearchTestAdapter.formatScore(0.88461)).isEqualTo("0.88");
    }

    @Test
    void summaryCountsMatches() {
        assertThat(RagSearchTestAdapter.summary("action button", 1)).startsWith("1 match for \"action button\"");
        assertThat(RagSearchTestAdapter.summary("action button", 5)).startsWith("5 matches for");
    }

    @Test
    void noMatchesMessageSaysWhatToTry() {
        assertThat(RagSearchTestAdapter.noMatchesMessage("x"))
                .contains("\"x\"").contains("indexed").contains("minimum score");
    }

    @Test
    void chunkOffsetPointsAtTheChunksFirstSubstantialLine() {
        String file = "package a;\n\nclass A {\n    void createActionButtons() {}\n}\n";
        String chunk = "\n  }\n    void createActionButtons() {}\n}";

        assertThat(RagSearchTestAdapter.chunkOffset(file, chunk)).isEqualTo(file.indexOf("void createActionButtons"));
    }

    @Test
    void chunkOffsetFallsBackToTheTopWhenTheFileChanged() {
        assertThat(RagSearchTestAdapter.chunkOffset("class B {}", "void somethingRemoved() {}")).isZero();
        assertThat(RagSearchTestAdapter.chunkOffset("class B {}", null)).isZero();
    }

    @Test
    void chunkOffsetSkipsCommonLinesThatWouldMatchTooEarly() {
        String file = "class A {\n    @Override\n    void a() {}\n    @Override\n    void createActionButtons() {}\n}";
        String changedChunk = "    @Override\n    void createActionButtons() {}\n    // edited since indexing";

        assertThat(RagSearchTestAdapter.chunkOffset(file, changedChunk)).isEqualTo(file.indexOf("void createActionButtons"));
    }

    @Test
    void chunkOffsetPrefersTheWholeChunk() {
        String file = "x\n    @Override\n    void a() {}\n";

        assertThat(RagSearchTestAdapter.chunkOffset(file, "@Override\n    void a() {}")).isEqualTo(file.indexOf("@Override"));
    }
}
