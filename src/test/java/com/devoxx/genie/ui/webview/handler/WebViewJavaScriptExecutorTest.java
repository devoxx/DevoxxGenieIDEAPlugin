package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.ui.webview.JCEFChecker;
import com.intellij.ui.jcef.JBCefBrowser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebViewJavaScriptExecutorTest {

    private WebViewJavaScriptExecutor executor;
    private WebViewJavaScriptExecutor nullBrowserExecutor;
    private MockedStatic<JCEFChecker> mockedJCEFChecker;

    @BeforeEach
    void setUp() {
        // Mock JCEFChecker to avoid real JCEF initialization
        mockedJCEFChecker = Mockito.mockStatic(JCEFChecker.class);
        mockedJCEFChecker.when(JCEFChecker::isJCEFAvailable).thenReturn(false);

        // Create executor with null browser (safe for unit testing)
        nullBrowserExecutor = new WebViewJavaScriptExecutor(null);

        // Create a mock browser for some tests
        JBCefBrowser mockBrowser = mock(JBCefBrowser.class);
        executor = new WebViewJavaScriptExecutor(mockBrowser);
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.dispose();
        }
        if (nullBrowserExecutor != null) {
            nullBrowserExecutor.dispose();
        }
        if (mockedJCEFChecker != null) {
            mockedJCEFChecker.close();
        }
    }

    @Test
    void escapeJsShouldEscapeBackslashes() {
        String result = executor.escapeJS("path\\to\\file");
        assertThat(result).isEqualTo("path\\\\to\\\\file");
    }

    @Test
    void escapeJsShouldEscapeBackticks() {
        String result = executor.escapeJS("template `literal`");
        assertThat(result).isEqualTo("template \\`literal\\`");
    }

    @Test
    void escapeJsShouldEscapeTemplateLiteralExpressions() {
        String result = executor.escapeJS("value is ${name}");
        assertThat(result).isEqualTo("value is \\${name}");
    }

    @Test
    void escapeJsShouldHandleAllEscapesInCombination() {
        String result = executor.escapeJS("\\`${test}`\\");
        assertThat(result).isEqualTo("\\\\\\`\\${test}\\`\\\\");
    }

    @Test
    void escapeHtmlShouldEscapeAmpersand() {
        String result = executor.escapeHtml("a & b");
        assertThat(result).isEqualTo("a &amp; b");
    }

    @Test
    void escapeHtmlShouldEscapeLessThan() {
        String result = executor.escapeHtml("<script>");
        assertThat(result).isEqualTo("&lt;script&gt;");
    }

    @Test
    void escapeHtmlShouldEscapeGreaterThan() {
        String result = executor.escapeHtml("a > b");
        assertThat(result).isEqualTo("a &gt; b");
    }

    @Test
    void escapeHtmlShouldEscapeDoubleQuotes() {
        String result = executor.escapeHtml("say \"hello\"");
        assertThat(result).isEqualTo("say &quot;hello&quot;");
    }

    @Test
    void escapeHtmlShouldEscapeSingleQuotes() {
        String result = executor.escapeHtml("it's");
        assertThat(result).isEqualTo("it&#39;s");
    }

    @Test
    void escapeHtmlShouldEscapeAllSpecialCharsInCombination() {
        String result = executor.escapeHtml("<div class=\"test\">&'value'</div>");
        assertThat(result).isEqualTo("&lt;div class=&quot;test&quot;&gt;&amp;&#39;value&#39;&lt;/div&gt;");
    }

    @Test
    void isLoadedShouldReturnFalseInitially() {
        assertThat(executor.isLoaded()).isFalse();
    }

    @Test
    void setLoadedShouldUpdateState() {
        executor.setLoaded(true);
        assertThat(executor.isLoaded()).isTrue();
    }

    @Test
    void setLoadedShouldToggleState() {
        executor.setLoaded(true);
        assertThat(executor.isLoaded()).isTrue();

        executor.setLoaded(false);
        assertThat(executor.isLoaded()).isFalse();
    }

    @Test
    void getExecutionStatsShouldReturnFormattedString() {
        String stats = executor.getExecutionStats();
        assertThat(stats).contains("Executions:");
        assertThat(stats).contains("Failures:");
        assertThat(stats).contains("Pending:");
        assertThat(stats).contains("Last execution:");
    }

    @Test
    void getExecutionStatsInitialValuesShouldBeZero() {
        String stats = executor.getExecutionStats();
        assertThat(stats).startsWith("Executions: 0, Failures: 0, Pending: 0");
    }

    @Test
    void clearPendingExecutionsShouldClearQueue() {
        // The queue starts empty
        executor.clearPendingExecutions();
        String stats = executor.getExecutionStats();
        assertThat(stats).contains("Pending: 0");
    }

    @Test
    void disposeShouldCleanUpResources() {
        executor.dispose();
        // After disposal, executor should still have valid state for stats
        String stats = executor.getExecutionStats();
        assertThat(stats).contains("Pending: 0");
    }

    @Test
    void executeJavaScriptWithNullScriptShouldNotThrow() {
        // Should not throw, just log a warning
        executor.executeJavaScript(null);
    }

    @Test
    void executeJavaScriptWithEmptyScriptShouldNotThrow() {
        executor.executeJavaScript("");
        executor.executeJavaScript("   ");
    }

    @Test
    void executeJavaScriptWhenJCEFNotAvailableShouldSkip() {
        // JCEF is mocked to return false, so this should be a no-op
        executor.executeJavaScript("console.log('test')");
        // No exception should occur
    }

    @Test
    void nullBrowserExecutorShouldNotThrow() {
        nullBrowserExecutor.executeJavaScript("console.log('test')");
        assertThat(nullBrowserExecutor.getExecutionStats()).contains("Executions:");
    }

    @Test
    void forcePendingExecutionShouldNotThrowWhenQueueEmpty() {
        executor.forcePendingExecution();
        // Should not throw
    }

    @Test
    void executeJavaScriptWithRetryShouldHandleNullScript() {
        executor.executeJavaScriptWithRetry(null, 3);
        // Should not throw
    }

    @Test
    void executeJavaScriptWithRetryShouldHandleEmptyScript() {
        executor.executeJavaScriptWithRetry("", 3);
        executor.executeJavaScriptWithRetry("   ", 3, 500);
        // Should not throw
    }
}
