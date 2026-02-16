package com.devoxx.genie.ui.webview.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebViewDebugLoggerTest {

    private WebViewDebugLogger logger;

    @BeforeEach
    void setUp() {
        logger = new WebViewDebugLogger("TestContext");
    }

    @Test
    void debugShouldAddEntryToRecentLogs() {
        logger.debug("Test debug message %s", "arg1");

        String recentLogs = logger.getRecentLogs();
        assertThat(recentLogs).contains("TestContext");
        assertThat(recentLogs).contains("DEBUG");
    }

    @Test
    void infoShouldAddEntryToRecentLogs() {
        logger.info("Test info message %s", "arg1");

        String recentLogs = logger.getRecentLogs();
        assertThat(recentLogs).contains("INFO");
    }

    @Test
    void warnShouldAddEntryToRecentLogs() {
        logger.warn("Test warning message %s", "arg1");

        String recentLogs = logger.getRecentLogs();
        assertThat(recentLogs).contains("WARN");
    }

    @Test
    void errorWithArgsShouldAddEntryToRecentLogs() {
        logger.error("Test error message %s", "arg1");

        String recentLogs = logger.getRecentLogs();
        assertThat(recentLogs).contains("ERROR");
    }

    @Test
    void errorWithThrowableShouldAddEntryToRecentLogs() {
        RuntimeException exception = new RuntimeException("test exception");
        logger.error("Test error", exception);

        String recentLogs = logger.getRecentLogs();
        assertThat(recentLogs).contains("ERROR");
        assertThat(recentLogs).contains("test exception");
    }

    @Test
    void logStateShouldFormatKeyValuePairs() {
        logger.logState("testOperation", "key1", "value1", "key2", "value2");

        String recentLogs = logger.getRecentLogs();
        assertThat(recentLogs).contains("STATE");
        assertThat(recentLogs).contains("testOperation");
    }

    @Test
    void logTimingShouldIncludeDuration() {
        long startTime = System.currentTimeMillis() - 100;
        logger.logTiming("testOp", startTime);

        String recentLogs = logger.getRecentLogs();
        assertThat(recentLogs).contains("TIMING");
        assertThat(recentLogs).contains("testOp");
    }

    @Test
    void logBrowserInfoShouldIncludeAllFields() {
        logger.logBrowserInfo("navigate", "http://test.com", 200, "ok");

        String recentLogs = logger.getRecentLogs();
        assertThat(recentLogs).contains("BROWSER");
        assertThat(recentLogs).contains("navigate");
    }

    @Test
    void logComponentInfoShouldIncludeAllFields() {
        logger.logComponentInfo("resize", true, true, 800, 600, "normal");

        String recentLogs = logger.getRecentLogs();
        assertThat(recentLogs).contains("COMPONENT");
        assertThat(recentLogs).contains("resize");
    }

    @Test
    void logRecoveryAttemptShouldIncludeAllFields() {
        logger.logRecoveryAttempt("reload", "blank screen", true, "recovered");

        String recentLogs = logger.getRecentLogs();
        assertThat(recentLogs).contains("RECOVERY");
        assertThat(recentLogs).contains("reload");
    }

    @Test
    void getRecentLogsShouldContainHeaderWithContext() {
        String recentLogs = logger.getRecentLogs();
        assertThat(recentLogs).contains("=== Recent Log Entries (TestContext) ===");
    }

    @Test
    void recentLogsShouldBeLimitedTo50Entries() {
        // Add more than 50 log entries
        for (int i = 0; i < 60; i++) {
            logger.debug("Message %d", i);
        }

        String recentLogs = logger.getRecentLogs();
        // Each log entry produces 2 entries in recentLogs (one from formatMessage addToRecentLogs call)
        // The queue should be capped at 50
        // Count the number of lines (excluding header)
        String[] lines = recentLogs.split("\\\\n");
        // Subtract 1 for the header line, and account for potential trailing empty
        int logLineCount = 0;
        for (String line : lines) {
            if (line.contains("[DEBUG]") || line.contains("[INFO]") || line.contains("[WARN]") || line.contains("[ERROR]") || line.contains("[STATE]") || line.contains("[TIMING]") || line.contains("[BROWSER]") || line.contains("[COMPONENT]") || line.contains("[RECOVERY]")) {
                logLineCount++;
            }
        }
        assertThat(logLineCount).isLessThanOrEqualTo(50);
    }

    @Test
    void formatMessageShouldHandleFormattingErrors() {
        // Pass mismatched format args - should not throw
        logger.debug("Message with %d but no int arg");

        String recentLogs = logger.getRecentLogs();
        // Should contain FORMATTING_ERROR or the original message
        assertThat(recentLogs).isNotEmpty();
    }

    @Test
    void multipleLogLevelsShouldAllAppearInRecentLogs() {
        logger.debug("debug msg");
        logger.info("info msg");
        logger.warn("warn msg");
        logger.error("error msg");

        String recentLogs = logger.getRecentLogs();
        assertThat(recentLogs).contains("DEBUG");
        assertThat(recentLogs).contains("INFO");
        assertThat(recentLogs).contains("WARN");
        assertThat(recentLogs).contains("ERROR");
    }

    @Test
    void dumpRecentLogsShouldNotThrow() {
        logger.info("some message");
        // dumpRecentLogs just logs to the main logger; should not throw
        logger.dumpRecentLogs();
    }
}
