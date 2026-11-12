package com.devoxx.genie.packaging;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IntelliJ IDEA 2026.3 no longer provides SwingX (org.jdesktop.swingx) to plugins, so a settings page that used
 * JXTitledSeparator failed to load there ("Cannot create configurable"). The tests run on an older platform where
 * SwingX still resolves, so this check keeps it out of the plugin's sources; use the platform's
 * com.intellij.ui.TitledSeparator instead.
 */
class NoSwingXDependencyTest {

    @Test
    void pluginSources_doNotUseSwingX() throws IOException {
        Path root = Paths.get("").toAbsolutePath();
        List<String> offenders;
        try (Stream<Path> files = Stream.of(root.resolve("src/main/java"), root.resolve("src/main/kotlin"))
                .filter(Files::isDirectory)
                .flatMap(NoSwingXDependencyTest::walk)) {
            offenders = files
                    .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".kt"))
                    .filter(NoSwingXDependencyTest::mentionsSwingX)
                    .map(p -> root.relativize(p).toString())
                    .sorted()
                    .toList();
        }

        assertThat(offenders).as("sources that use org.jdesktop.swingx").isEmpty();
    }

    private static Stream<Path> walk(Path dir) {
        try {
            return Files.walk(dir);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean mentionsSwingX(Path file) {
        try {
            return Files.readString(file).contains("org.jdesktop.swingx");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
