package com.devoxx.genie.service.rag;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProjectIndexerService#isRagExcluded(Path, List, Path)} — the
 * project-relative path-prefix exclusion filter introduced in task-220.
 *
 * <p>Semantics under test: an entry is a project-relative path. A file is excluded if its
 * project-relative path equals an entry or starts with {@code entry + "/"}. Single-segment
 * entries match only the corresponding top-level dir (NOT a nested one with the same name);
 * this is the regression guard the user reported after the first cut, which used segment
 * matching and showed misleading rows like just {@code book} for an entry that should have
 * been {@code docs/book}.
 */
class ProjectIndexerServiceTest {

    private static final Path BASE = Paths.get("/repo");

    @Test
    void isRagExcluded_matchesExactRelativePath() {
        Path file = Paths.get("/repo/docs/book/chapter-1.md");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("docs/book"), BASE)).isTrue();
    }

    @Test
    void isRagExcluded_matchesDescendantOfRelativePath() {
        Path file = Paths.get("/repo/docs/book/sub/chapter-1.md");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("docs/book"), BASE)).isTrue();
    }

    @Test
    void isRagExcluded_doesNotMatchSiblingWithSameLeafName() {
        // The user's report: "docs/book" must NOT exclude "/repo/book/..." just because the
        // last segment matches. Critical regression for path-prefix semantics.
        Path file = Paths.get("/repo/book/intro.md");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("docs/book"), BASE)).isFalse();
    }

    @Test
    void isRagExcluded_topLevelSingleSegmentStillExcludesTopLevelDir() {
        // Single-segment entries (e.g. "node_modules") still work for top-level dirs because
        // the relative path begins with that segment.
        Path file = Paths.get("/repo/node_modules/x/foo.js");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("node_modules"), BASE)).isTrue();
    }

    @Test
    void isRagExcluded_singleSegmentMatchesAnyDirAnywhere() {
        // Single-segment entries (no "/" in the entry) match the directory wherever it
        // appears in the file's path — matching the project-scanner's existing behavior.
        // The user reported their RAG exclusions weren't taking effect when entries were
        // typed as bare names like "obsidian"; this is the fallback that catches that case.
        Path nested = Paths.get("/repo/foo/obsidian/bar.md");
        assertThat(ProjectIndexerService.isRagExcluded(nested, List.of("obsidian"), BASE)).isTrue();
    }

    @Test
    void isRagExcluded_multiSegmentEntryDoesNotFallBackToSegmentMatch() {
        // Entries containing "/" stay strictly prefix-matched — the segment fallback only
        // applies when the entry is a single bare segment. So "docs/book" does NOT match
        // "/repo/book/..." (sibling with same leaf) and does NOT match a nested
        // "docs/book" appearing under a different parent.
        Path file = Paths.get("/repo/book/intro.md");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("docs/book"), BASE)).isFalse();
    }

    @Test
    void isRagExcluded_doesNotMatchPartialSegmentSubstrings() {
        // "build/generated" must NOT match "build/generated-sources" (no segment-boundary).
        Path file = Paths.get("/repo/build/generated-sources/foo.java");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("build/generated"), BASE)).isFalse();
    }

    @Test
    void isRagExcluded_returnsFalseForEmptyExcludedList() {
        Path file = Paths.get("/repo/anywhere/anything.java");
        assertThat(ProjectIndexerService.isRagExcluded(file, Collections.emptyList(), BASE)).isFalse();
    }

    @Test
    void isRagExcluded_matchesAnyOfMultipleEntries() {
        List<String> excluded = List.of("dist", "node_modules", "docs/book");
        assertThat(ProjectIndexerService.isRagExcluded(Paths.get("/repo/dist/foo.js"), excluded, BASE)).isTrue();
        assertThat(ProjectIndexerService.isRagExcluded(Paths.get("/repo/docs/book/x.md"), excluded, BASE)).isTrue();
        assertThat(ProjectIndexerService.isRagExcluded(Paths.get("/repo/node_modules/x.js"), excluded, BASE)).isTrue();
        assertThat(ProjectIndexerService.isRagExcluded(Paths.get("/repo/src/foo.js"), excluded, BASE)).isFalse();
    }

    @Test
    void isRagExcluded_toleratesTrailingSlashesInEntries() {
        // Trailing slashes are stripped. Leading slashes are NOT stripped — a leading "/"
        // signals an absolute path (which then matches against the file's absolute path).
        Path file = Paths.get("/repo/docs/book/x.md");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("docs/book/"), BASE)).isTrue();
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("/repo/docs/book/"), BASE)).isTrue();
    }

    @Test
    void isRagExcluded_isCaseSensitive() {
        Path file = Paths.get("/repo/Docs/Book/x.md");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("docs/book"), BASE)).isFalse();
    }

    @Test
    void isRagExcluded_fileOutsideProject_multiSegmentEntryDoesNotMatch() {
        // Multi-segment entries are strict prefix matches; an outside-project file's
        // absolute path doesn't start with the entry, and there's no project-relative form.
        Path file = Paths.get("/elsewhere/docs/book/foo.md");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("docs/book"), BASE)).isFalse();
    }

    @Test
    void isRagExcluded_fileOutsideProject_singleSegmentStillSegmentMatches() {
        // The single-segment fallback fires regardless of project base (it iterates the
        // file's own path segments). This is intentional — typing "node_modules" should
        // skip any node_modules dir even for sources outside the project root.
        Path file = Paths.get("/elsewhere/node_modules/foo.js");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("node_modules"), BASE)).isTrue();
    }

    @Test
    void isRagExcluded_nullProjectBase_usesAbsolutePathAsRelative() {
        // When we don't know the project base (atypical), entries must match the file's
        // absolute-path string prefix. Entries like "docs/book" therefore won't fire on
        // typical absolute paths starting with "/" — pinning the degraded but defined behavior.
        Path file = Paths.get("/repo/docs/book/x.md");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("docs/book"), null)).isFalse();
    }

    @Test
    void isRagExcluded_ignoresBlankEntries() {
        Path file = Paths.get("/repo/docs/book/x.md");
        // null, empty, whitespace-only, and slashes-only entries are silently skipped (defensive
        // against editing artifacts from the dialog).
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("", "   ", "/", "//"), BASE)).isFalse();
    }

    // --- absolute-path entries (what Browse... inserts so users see the full path) ----

    @Test
    void isRagExcluded_matchesAbsolutePathEntry() {
        // Browse... inserts the absolute path; the file under that dir must still be excluded.
        Path file = Paths.get("/repo/obsidian/notes.md");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("/repo/obsidian"), BASE)).isTrue();
    }

    @Test
    void isRagExcluded_matchesAbsolutePathDescendant() {
        Path file = Paths.get("/repo/obsidian/sub/notes.md");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("/repo/obsidian"), BASE)).isTrue();
    }

    @Test
    void isRagExcluded_absoluteEntryDoesNotMatchSiblingWithSameLeafName() {
        // "/other-project/obsidian" must not exclude /repo/obsidian/... — different absolute
        // path even though leaf segment is the same.
        Path file = Paths.get("/repo/obsidian/notes.md");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("/other-project/obsidian"), BASE))
                .isFalse();
    }

    @Test
    void isRagExcluded_absoluteAndRelativeEntriesCoexist() {
        // Power user mixes both forms in a single list — both should work.
        Path absMatchFile = Paths.get("/repo/obsidian/n.md");
        Path relMatchFile = Paths.get("/repo/docs/book/x.md");
        List<String> excluded = List.of("/repo/obsidian", "docs/book");
        assertThat(ProjectIndexerService.isRagExcluded(absMatchFile, excluded, BASE)).isTrue();
        assertThat(ProjectIndexerService.isRagExcluded(relMatchFile, excluded, BASE)).isTrue();
    }

    @Test
    void isRagExcluded_userScenario_workshopProjectAbsolutePaths() {
        // Verbatim scenario from the user (task-220 second feedback). Browse... inserted
        // four absolute paths; files under each must be excluded, files outside must not.
        Path base = Paths.get("/Users/stephan/IdeaProjects/AgenticEngineeringWorkshop");
        List<String> excluded = List.of(
                "/Users/stephan/IdeaProjects/AgenticEngineeringWorkshop/book",
                "/Users/stephan/IdeaProjects/AgenticEngineeringWorkshop/slides/asciidoc",
                "/Users/stephan/IdeaProjects/AgenticEngineeringWorkshop/backlog",
                "/Users/stephan/IdeaProjects/AgenticEngineeringWorkshop/slides/js"
        );

        assertThat(ProjectIndexerService.isRagExcluded(
                Paths.get("/Users/stephan/IdeaProjects/AgenticEngineeringWorkshop/slides/asciidoc/02-fundamentals.adoc"),
                excluded, base)).isTrue();
        assertThat(ProjectIndexerService.isRagExcluded(
                Paths.get("/Users/stephan/IdeaProjects/AgenticEngineeringWorkshop/book/chapter1.md"),
                excluded, base)).isTrue();
        assertThat(ProjectIndexerService.isRagExcluded(
                Paths.get("/Users/stephan/IdeaProjects/AgenticEngineeringWorkshop/slides/js/page.js"),
                excluded, base)).isTrue();
        // Siblings of excluded dirs must NOT be excluded.
        assertThat(ProjectIndexerService.isRagExcluded(
                Paths.get("/Users/stephan/IdeaProjects/AgenticEngineeringWorkshop/slides/html/page.html"),
                excluded, base)).isFalse();
        assertThat(ProjectIndexerService.isRagExcluded(
                Paths.get("/Users/stephan/IdeaProjects/AgenticEngineeringWorkshop/src/Main.java"),
                excluded, base)).isFalse();
    }

    @Test
    void isRagExcluded_absoluteEntryWorksWithoutProjectBase() {
        // Even when projectBase is unknown, an absolute entry should still match the file's
        // absolute path.
        Path file = Paths.get("/repo/obsidian/notes.md");
        assertThat(ProjectIndexerService.isRagExcluded(file, List.of("/repo/obsidian"), null)).isTrue();
    }
}
