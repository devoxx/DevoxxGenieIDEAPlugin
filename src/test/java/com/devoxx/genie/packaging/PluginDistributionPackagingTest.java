package com.devoxx.genie.packaging;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Regression test for issue #1054.
 *
 * <p>The IntelliJ Platform ships its own Kotlin coroutines runtime. When the plugin
 * distribution bundles its <em>own</em> copy of {@code kotlinx-coroutines-core(-jvm)} the
 * plugin's {@code PluginClassLoader} loads classes such as
 * {@code kotlinx.coroutines.flow.FlowCollector} while the platform's {@code PathClassLoader}
 * loads {@code kotlinx.coroutines.flow.internal.SafeCollector}. When the platform inline
 * completion API hands a {@code SafeCollector} to the plugin's compiled Kotlin
 * ({@code DevoxxGenieInlineCompletionProvider.getSuggestionDebounced}) the cross-classloader
 * cast fails with:
 *
 * <pre>
 * java.lang.ClassCastException: class kotlinx.coroutines.flow.internal.SafeCollector
 *   cannot be cast to class kotlinx.coroutines.flow.FlowCollector
 *   (SafeCollector is in unnamed module of loader PathClassLoader;
 *    FlowCollector is in unnamed module of loader PluginClassLoader)
 * </pre>
 *
 * <p>{@code stripBinaryIncompatibleRuntimeJars} already removes these jars from the
 * {@code prepareSandbox}/{@code prepareTestSandbox} outputs (so {@code runIde} and the
 * test sandbox are clean and the bug is invisible during development), but it was NOT applied
 * to the {@code buildPlugin} distribution that is actually published. This test inspects the
 * built distribution ZIP and fails if any forbidden coroutines jar is present in the plugin
 * {@code lib/} directory.
 */
class PluginDistributionPackagingTest {

    /**
     * Jars that must never be bundled in the plugin distribution because the IntelliJ Platform
     * provides them at runtime. Mirrors {@code binaryIncompatibleRuntimeJarPatterns} in
     * {@code build.gradle.kts} (the coroutines entries).
     */
    private static final List<Pattern> FORBIDDEN_LIB_JARS = Arrays.asList(
            Pattern.compile("kotlinx-coroutines-core-.*\\.jar"),
            Pattern.compile("kotlinx-coroutines-core-jvm-.*\\.jar")
    );

    @Test
    void distributionMustNotBundlePlatformCoroutinesJars() throws Exception {
        Optional<File> distribution = findLatestDistributionZip();

        // The distribution is only produced by `./gradlew buildPlugin`. When it is absent
        // (e.g. a plain `./gradlew test` on a clean checkout) there is nothing to verify, so
        // skip rather than fail spuriously. Run `./gradlew buildPlugin` to exercise this test.
        assumeTrue(distribution.isPresent(),
                "No plugin distribution ZIP found under build/distributions/ - run ./gradlew buildPlugin first");

        File zip = distribution.get();
        List<String> offending = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(zip)) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                // Only inspect jars that live in the packaged plugin's lib/ directory,
                // e.g. "DevoxxGenie/lib/kotlinx-coroutines-core-jvm-1.8.0.jar".
                int libIdx = name.indexOf("/lib/");
                if (libIdx < 0) {
                    continue;
                }
                String fileName = name.substring(name.lastIndexOf('/') + 1);
                for (Pattern forbidden : FORBIDDEN_LIB_JARS) {
                    if (forbidden.matcher(fileName).matches()) {
                        offending.add(name);
                        break;
                    }
                }
            }
        }

        if (!offending.isEmpty()) {
            fail("Plugin distribution '" + zip.getName() + "' bundles coroutines jars that conflict "
                    + "with the IntelliJ Platform classloader (issue #1054). "
                    + "These must be excluded from the published plugin:\n  "
                    + String.join("\n  ", offending));
        }
    }

    /**
     * Locates the most recently modified plugin distribution ZIP produced by buildPlugin.
     * Walks up from the working directory so the test works regardless of the module the
     * test runner is launched from.
     */
    private static Optional<File> findLatestDistributionZip() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < 4 && dir != null; depth++) {
            File distributions = dir.resolve("build").resolve("distributions").toFile();
            if (distributions.isDirectory()) {
                File[] zips = distributions.listFiles(
                        (d, fileName) -> fileName.endsWith(".zip") && fileName.startsWith("DevoxxGenie"));
                if (zips != null && zips.length > 0) {
                    return Arrays.stream(zips).max(Comparator.comparingLong(File::lastModified));
                }
            }
            dir = dir.getParent();
        }
        return Optional.empty();
    }
}
