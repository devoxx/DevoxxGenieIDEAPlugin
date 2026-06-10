package com.devoxx.genie;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiCompatibilityTest {

    private static final List<String> FORBIDDEN_USAGES = List.of(
            "StartupUiUtil.isUnderDarcula",
            "ReadAction.compute",
            "ReadAction.run",
            "ToolProviderResult.tools(",
            "LocalClipboardManager",
            "ClipboardManager"
    );

    @Test
    void productionSourcesDoNotUseReportedRemovalOrDeprecatedApis() throws IOException {
        List<String> violations;
        try (var paths = Files.walk(Path.of("src/main"))) {
            violations = paths
                    .filter(Files::isRegularFile)
                    .filter(ApiCompatibilityTest::isSourceFile)
                    .flatMap(path -> findForbiddenUsages(path).stream())
                    .toList();
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void settingsDialogIsNeverOpenedSynchronouslyOnTheEdt() throws IOException {
        // ShowSettingsUtil.getInstance().showSettingsDialog(...) collects ALL configurable
        // groups on the calling thread. On the EDT this crashes in PyCharm: the Flask console
        // configurable calls runBlockingCancellable during group collection, which asserts a
        // background thread (IllegalStateException "This method is forbidden on EDT").
        // All settings opening must go through SettingsDialogUtil, which collects the groups
        // on a pooled thread before showing the dialog on the EDT.
        List<String> violations;
        try (var paths = Files.walk(Path.of("src/main"))) {
            violations = paths
                    .filter(Files::isRegularFile)
                    .filter(ApiCompatibilityTest::isSourceFile)
                    .filter(path -> fileContains(path, "ShowSettingsUtil.getInstance().showSettingsDialog"))
                    .map(Path::toString)
                    .toList();
        }

        assertThat(violations).isEmpty();
    }

    private static boolean fileContains(Path path, String needle) {
        try {
            return Files.readString(path).contains(needle);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }

    private static boolean isSourceFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".java") || fileName.endsWith(".kt");
    }

    private static List<String> findForbiddenUsages(Path path) {
        try {
            String content = Files.readString(path);
            return FORBIDDEN_USAGES.stream()
                    .filter(content::contains)
                    .map(usage -> path + ": " + usage)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }
}
