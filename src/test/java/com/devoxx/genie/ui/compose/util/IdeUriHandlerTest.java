package com.devoxx.genie.ui.compose.util;

import com.intellij.ide.BrowserUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/** TASK-277.2: chat markdown links open through IntelliJ's browser support, and only safe schemes open. */
class IdeUriHandlerTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html",
            "http://example.com/page",
            "HTTPS://EXAMPLE.COM/UPPER",
            "mailto:someone@example.com"})
    void safeLinks_areOpened(String uri) {
        List<String> opened = new ArrayList<>();

        new IdeUriHandler(recordInto(opened)).openUri(uri);

        assertThat(opened).containsExactly(uri);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "file:///etc/passwd",
            "jar:file:/tmp/x.jar!/a",
            "relative/path",
            "",
            "   "})
    void unsafeOrRelativeLinks_areIgnored(String uri) {
        List<String> opened = new ArrayList<>();

        new IdeUriHandler(recordInto(opened)).openUri(uri);

        assertThat(opened).isEmpty();
    }

    /** Kotlin's (String) -> Unit, recording every opened link. */
    private static kotlin.jvm.functions.Function1<String, kotlin.Unit> recordInto(List<String> opened) {
        return uri -> {
            opened.add(uri);
            return kotlin.Unit.INSTANCE;
        };
    }

    @Test
    void defaultHandler_delegatesToBrowserUtil() {
        try (MockedStatic<BrowserUtil> browser = mockStatic(BrowserUtil.class)) {
            new IdeUriHandler().openUri("https://genie.devoxx.com/docs/features/web-search");

            browser.verify(() -> BrowserUtil.browse("https://genie.devoxx.com/docs/features/web-search"));
        }
    }
}
