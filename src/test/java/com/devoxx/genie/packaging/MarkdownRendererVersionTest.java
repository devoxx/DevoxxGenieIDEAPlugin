package com.devoxx.genie.packaging;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Regression test for the Compose code-highlighter crash:
 *
 * <pre>
 * java.lang.NoSuchMethodError: 'void kotlinx.coroutines.Job.cancel$default(
 *     kotlinx.coroutines.Job, java.util.concurrent.CancellationException, int, java.lang.Object)'
 *   at com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeKt$produceHighlightsState...
 * </pre>
 *
 * <p>The {@code com.mikepenz:multiplatform-markdown-renderer-code} module's
 * {@code produceHighlightsState} calls {@code Job.cancel()} in its {@code awaitDispose} cleanup,
 * which fires when a streamed message containing a code block is disposed (e.g. after an MCP call).
 * From {@code 0.39.0} onward the renderer links against a Kotlin 2.3 stdlib / newer
 * kotlinx-coroutines ABI whose synthetic {@code cancel$default} bridge is absent from the
 * coroutines runtime bundled in IJ 2025.3.3 (Kotlin 2.2.20), crashing at runtime.
 *
 * <p>Two Dependabot bumps ({@code 0.38.1 -> 0.41.0 -> 0.43.0}) previously overrode the documented
 * ceiling. This test parses {@code build.gradle.kts} and fails if {@code markdownRendererVersion}
 * drifts to {@code 0.39.0} or later, so the regression cannot be reintroduced silently.
 */
class MarkdownRendererVersionTest {

    private static final Pattern VERSION_DECL = Pattern.compile(
            "val\\s+markdownRendererVersion\\s*=\\s*\"(\\d+)\\.(\\d+)\\.(\\d+)\"");

    @Test
    void markdownRendererMustStayOnCompatibleLine() throws IOException {
        Path buildFile = findBuildGradle()
                .orElseThrow(() -> new AssertionError(
                        "Could not locate build.gradle.kts from working directory "
                                + Paths.get("").toAbsolutePath()));

        String content = Files.readString(buildFile, StandardCharsets.UTF_8);
        Matcher matcher = VERSION_DECL.matcher(content);
        if (!matcher.find()) {
            fail("Could not find 'val markdownRendererVersion = \"x.y.z\"' in " + buildFile
                    + " - did the declaration change? Update this regression test accordingly.");
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        String version = matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3);

        // 0.38.x is the newest line compatible with the Kotlin 2.2.20 / coroutines runtime bundled
        // in IJ 2025.3.3. 0.39.x+ crashes the Compose code-highlighter with NoSuchMethodError on
        // Job.cancel$default. See class javadoc.
        boolean compatible = major == 0 && minor <= 38;
        assertTrue(compatible,
                "multiplatform-markdown-renderer is pinned to " + version + ", but 0.39.0+ links "
                        + "against an incompatible kotlinx-coroutines ABI and crashes the Compose "
                        + "code-highlighter (NoSuchMethodError: Job.cancel$default) on IJ 2025.3.3. "
                        + "Keep it on the 0.38.x line until the minimum supported IDE ships Kotlin 2.3 "
                        + "(IJ 261+). This was bumped by Dependabot before - check .github/dependabot.yaml.");
    }

    /**
     * Walks up from the working directory to find build.gradle.kts, so the test works regardless of
     * the module the test runner is launched from.
     */
    private static Optional<Path> findBuildGradle() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < 4 && dir != null; depth++) {
            Path candidate = dir.resolve("build.gradle.kts");
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
            dir = dir.getParent();
        }
        return Optional.empty();
    }
}
