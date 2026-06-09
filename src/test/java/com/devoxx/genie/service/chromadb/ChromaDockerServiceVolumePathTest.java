package com.devoxx.genie.service.chromadb;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ChromaDockerService#toDockerVolumePath(String, String)}.
 *
 * <p>Covers the WSL bind-mount bug (GitHub issue #1085): when IntelliJ runs on
 * Windows and Docker is backed by a WSL Linux daemon, the JVM produces a
 * Windows-style path ({@code C:\Users\...}) which the Linux daemon rejects.
 * The method must convert such paths to the WSL {@code /mnt/<drive>/...} format.
 */
class ChromaDockerServiceVolumePathTest {

    // ── Linux daemon + Windows JVM path (WSL scenario) ─────────────────────────

    @Test
    void linuxDaemon_windowsPathWithBackslashes_convertsToMntFormat() {
        String result = ChromaDockerService.toDockerVolumePath(
                "linux",
                "C:\\Users\\abc\\AppData\\Local\\DevoxxGenie\\chromadb\\data-123");

        assertThat(result)
                .isEqualTo("/mnt/c/Users/abc/AppData/Local/DevoxxGenie/chromadb/data-123");
    }

    @Test
    void linuxDaemon_windowsPathWithForwardSlashes_convertsToMntFormat() {
        String result = ChromaDockerService.toDockerVolumePath(
                "linux",
                "C:/Users/abc/AppData/Local/DevoxxGenie/chromadb/data-123");

        assertThat(result)
                .isEqualTo("/mnt/c/Users/abc/AppData/Local/DevoxxGenie/chromadb/data-123");
    }

    @Test
    void linuxDaemon_windowsPathDriveLetterLowercased() {
        String upper = ChromaDockerService.toDockerVolumePath("linux", "D:\\some\\path");
        String lower = ChromaDockerService.toDockerVolumePath("linux", "d:\\some\\path");

        assertThat(upper).isEqualTo("/mnt/d/some/path");
        assertThat(lower).isEqualTo("/mnt/d/some/path");
    }

    @Test
    void linuxDaemon_daemonOsIsCaseInsensitive() {
        String linux  = ChromaDockerService.toDockerVolumePath("Linux",  "C:\\path\\dir");
        String LINUX  = ChromaDockerService.toDockerVolumePath("LINUX",  "C:\\path\\dir");
        String linuxl = ChromaDockerService.toDockerVolumePath("linux",  "C:\\path\\dir");

        assertThat(linux).isEqualTo(LINUX).isEqualTo(linuxl)
                .isEqualTo("/mnt/c/path/dir");
    }

    @Test
    void linuxDaemon_exactPathFromIssue1085_convertsCorrectly() {
        // The exact path that appeared in the bug report
        String input = "C:\\Users\\abc\\AppData\\Local\\Google\\AndroidStudio2026.1.1" +
                       "\\DevoxxGenie\\chromadb\\data-1446501950";

        String result = ChromaDockerService.toDockerVolumePath("linux", input);

        assertThat(result)
                .startsWith("/mnt/c/")
                .doesNotContain("\\")
                .doesNotContain("C:");
    }

    // ── Linux daemon + already-Linux path ──────────────────────────────────────

    @Test
    void linuxDaemon_linuxPath_returnedUnchanged() {
        String path = "/home/user/.local/share/DevoxxGenie/chromadb/data-123";

        String result = ChromaDockerService.toDockerVolumePath("linux", path);

        assertThat(result).isEqualTo(path);
    }

    @Test
    void linuxDaemon_mntPath_returnedUnchanged() {
        String path = "/mnt/c/Users/already-converted/data-123";

        String result = ChromaDockerService.toDockerVolumePath("linux", path);

        assertThat(result).isEqualTo(path);
    }

    // ── Windows daemon + Windows JVM path ──────────────────────────────────────

    @Test
    void windowsDaemon_windowsPath_returnedUnchanged() {
        String path = "C:\\Users\\abc\\AppData\\Local\\DevoxxGenie\\chromadb\\data-123";

        String result = ChromaDockerService.toDockerVolumePath("windows", path);

        assertThat(result).isEqualTo(path);
    }

    @Test
    void windowsDaemonCaseInsensitive_windowsPath_returnedUnchanged() {
        String path = "C:\\Users\\abc\\data-123";

        assertThat(ChromaDockerService.toDockerVolumePath("Windows", path)).isEqualTo(path);
        assertThat(ChromaDockerService.toDockerVolumePath("WINDOWS", path)).isEqualTo(path);
    }

    // ── Null / unknown daemon OS ────────────────────────────────────────────────

    @Test
    void unknownDaemonOs_windowsPath_returnedUnchanged() {
        String path = "C:\\Users\\abc\\data-123";

        String result = ChromaDockerService.toDockerVolumePath("unknown", path);

        assertThat(result).isEqualTo(path);
    }

    @Test
    void nullDaemonOs_windowsPath_returnedUnchanged() {
        String path = "C:\\Users\\abc\\data-123";

        String result = ChromaDockerService.toDockerVolumePath(null, path);

        assertThat(result).isEqualTo(path);
    }
}
