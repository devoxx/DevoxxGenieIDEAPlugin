package com.devoxx.genie.service.rag.benchmark;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkCorpusTest {

    private static final List<String> EXT = List.of("java", "md");
    private static final List<String> DIRS = List.of("build", "node_modules");

    @Test
    void keepsOnlyIncludedExtensionsCaseInsensitively() {
        List<String> kept = BenchmarkCorpus.select(
                List.of("src/A.java", "README.MD", "logo.png", "Makefile", "x.javax"), EXT, DIRS, List.of());

        assertThat(kept).containsExactly("src/A.java", "README.MD");
    }

    @Test
    void dropsFilesInsideExcludedDirectoriesAtAnyDepth() {
        List<String> kept = BenchmarkCorpus.select(
                List.of("build/Gen.java", "web/node_modules/lib/x.md", "src/build/Gen.java", "src/Ok.java"),
                EXT, DIRS, List.of());

        assertThat(kept).containsExactly("src/Ok.java");
    }

    @Test
    void excludedDirectoryNameMustMatchAWholeSegment() {
        // "buildSrc" is not "build"; and a file named like an excluded dir isn't a directory.
        List<String> kept = BenchmarkCorpus.select(
                List.of("buildSrc/A.java", "docs/build.md"), EXT, DIRS, List.of());

        assertThat(kept).containsExactly("buildSrc/A.java", "docs/build.md");
    }

    @Test
    void dropsExplicitPathPrefixes() {
        List<String> kept = BenchmarkCorpus.select(
                List.of("src/test/resources/rag-chunking/fixtures/Sample.java", "src/test/Real.java"),
                EXT, DIRS, List.of("src/test/resources/rag-chunking/"));

        assertThat(kept).containsExactly("src/test/Real.java");
    }

    @Test
    void preservesInputOrder() {
        List<String> kept = BenchmarkCorpus.select(List.of("b.md", "a.java", "c.md"), EXT, DIRS, List.of());

        assertThat(kept).containsExactly("b.md", "a.java", "c.md");
    }
}
