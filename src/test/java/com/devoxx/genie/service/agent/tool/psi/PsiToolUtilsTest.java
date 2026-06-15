package com.devoxx.genie.service.agent.tool.psi;

import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the pure path-resolution helpers in {@link PsiToolUtils}.
 * <p>
 * These guard the fallback that makes PSI tools tolerant of slightly wrong paths
 * (e.g. the LLM guessing {@code .../ui/Foo.java} when the file is under
 * {@code .../controller/Foo.java}). See {@link PsiToolUtils#resolvePsiFile}.
 */
class PsiToolUtilsTest {

    // --- normalizeRelativePath ---

    @Test
    void normalizeRelativePath_trimsWhitespace() {
        assertThat(PsiToolUtils.normalizeRelativePath("  src/Foo.java  ")).isEqualTo("src/Foo.java");
    }

    @Test
    void normalizeRelativePath_convertsBackslashes() {
        assertThat(PsiToolUtils.normalizeRelativePath("src\\main\\Foo.java")).isEqualTo("src/main/Foo.java");
    }

    @Test
    void normalizeRelativePath_stripsLeadingDotSlash() {
        assertThat(PsiToolUtils.normalizeRelativePath("./src/Foo.java")).isEqualTo("src/Foo.java");
    }

    @Test
    void normalizeRelativePath_stripsLeadingSlash() {
        assertThat(PsiToolUtils.normalizeRelativePath("/src/Foo.java")).isEqualTo("src/Foo.java");
    }

    @Test
    void normalizeRelativePath_stripsMultipleLeadingSegments() {
        assertThat(PsiToolUtils.normalizeRelativePath(".//src/Foo.java")).isEqualTo("src/Foo.java");
    }

    @Test
    void normalizeRelativePath_leavesPlainPathUntouched() {
        assertThat(PsiToolUtils.normalizeRelativePath("src/main/java/Foo.java"))
                .isEqualTo("src/main/java/Foo.java");
    }

    // --- fileNameOf ---

    @Test
    void fileNameOf_returnsLastSegment() {
        assertThat(PsiToolUtils.fileNameOf("src/main/java/com/devoxx/Foo.java")).isEqualTo("Foo.java");
    }

    @Test
    void fileNameOf_returnsWholeStringWhenNoSlash() {
        assertThat(PsiToolUtils.fileNameOf("Foo.java")).isEqualTo("Foo.java");
    }

    // --- pickBestPathMatch ---

    @Test
    void pickBestPathMatch_emptyCandidates_returnsNull() {
        assertThat(PsiToolUtils.pickBestPathMatch(List.of(), "src/Foo.java")).isNull();
    }

    @Test
    void pickBestPathMatch_singleCandidate_returnsIt() {
        VirtualFile file = mockFile("/project/src/main/java/com/devoxx/genie/controller/Foo.java", false);
        assertThat(PsiToolUtils.pickBestPathMatch(List.of(file), "anything/Foo.java")).isSameAs(file);
    }

    @Test
    void pickBestPathMatch_picksLongestTrailingSegmentOverlap() {
        // Two files share the name; the requested path's trailing segments match one of them more
        // closely, so that candidate must win regardless of list order.
        VirtualFile decoy = mockFile("/project/x/y/other/impl/Foo.java", false);
        VirtualFile wanted = mockFile("/project/a/b/service/impl/Foo.java", false);

        String requested = "service/impl/Foo.java";

        assertThat(PsiToolUtils.pickBestPathMatch(List.of(decoy, wanted), requested)).isSameAs(wanted);
        // Order must not matter.
        assertThat(PsiToolUtils.pickBestPathMatch(List.of(wanted, decoy), requested)).isSameAs(wanted);
    }

    @Test
    void pickBestPathMatch_wrongImmediateParent_stillResolvesToSameNamedFile() {
        // Reproduces the reported bug shape: the LLM guessed the wrong package directory
        // ("ui" instead of "controller"). When the differing directory is immediately before the
        // file name, the trailing overlap is just the file name for every candidate, so we cannot
        // tell them apart - but we must still return a same-named file rather than a hard error.
        VirtualFile controller =
                mockFile("/project/src/main/java/com/devoxx/genie/controller/ActionButtonsPanelController.java", false);

        String requested = "src/main/java/com/devoxx/genie/ui/ActionButtonsPanelController.java";

        assertThat(PsiToolUtils.pickBestPathMatch(List.of(controller), requested)).isSameAs(controller);
    }

    @Test
    void pickBestPathMatch_skipsDirectories() {
        VirtualFile dir = mockFile("/project/src/Foo.java", true);
        VirtualFile realFile = mockFile("/project/other/Foo.java", false);
        assertThat(PsiToolUtils.pickBestPathMatch(List.of(dir, realFile), "x/Foo.java")).isSameAs(realFile);
    }

    @Test
    void pickBestPathMatch_allDirectories_returnsNull() {
        VirtualFile dir = mockFile("/project/src/Foo.java", true);
        assertThat(PsiToolUtils.pickBestPathMatch(List.of(dir), "x/Foo.java")).isNull();
    }

    private static VirtualFile mockFile(String path, boolean isDirectory) {
        VirtualFile file = mock(VirtualFile.class);
        lenient().when(file.getPath()).thenReturn(path);
        lenient().when(file.isDirectory()).thenReturn(isDirectory);
        return file;
    }
}
