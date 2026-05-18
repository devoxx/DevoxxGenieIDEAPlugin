package com.devoxx.genie.ui.panel.log;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the inline-preview and tooltip helpers used by {@link AgentMcpLogPanel}.
 * These guard the contract that the list-row preview and the hover tooltip surface enough
 * information about a multi-line tool result for the user to recognise when the full output
 * needs to be inspected (instead of silently appearing as a single error line).
 */
class AgentMcpLogPanelFormatTest {

    @Test
    void flattenForRow_singleLine_returnsAsIs() {
        assertThat(AgentMcpLogPanel.flattenForRow("hello")).isEqualTo("hello");
    }

    @Test
    void flattenForRow_emptyString_returnsEmpty() {
        assertThat(AgentMcpLogPanel.flattenForRow("")).isEmpty();
    }

    @Test
    void flattenForRow_trailingNewlineOnly_isTreatedAsSingleLine() {
        // RunCommandToolExecutor appends a trailing '\n' to each line, so a single-line result
        // ends with '\n'. That trailing empty segment must not be reported as a second line.
        assertThat(AgentMcpLogPanel.flattenForRow("output\n")).isEqualTo("output");
    }

    @Test
    void flattenForRow_multiLine_usesVisibleSeparatorAndLineCount() {
        String input = "line1\nline2\nline3";
        String preview = AgentMcpLogPanel.flattenForRow(input);
        assertThat(preview).contains("line1");
        assertThat(preview).contains("line2");
        assertThat(preview).contains("line3");
        assertThat(preview).contains("\u23ce"); // visible return separator
        assertThat(preview).endsWith("(3 lines)");
    }

    @Test
    void flattenForRow_realWorldRunCommandOutput_showsBothStderrAndStdout() {
        // Reproduces the bug pattern from #1027 follow-up: the agent log row only showed
        // the sdkman stderr noise and the user thought the JAVA_HOME value was missing.
        // With the new formatting, both lines are visible plus a "(2 lines)" marker.
        String input = "/Users/x/.sdkman/path-helpers.sh: line 61: ${name^^}: bad substitution\n"
                + "/Library/Java/jdk21\n";
        String preview = AgentMcpLogPanel.flattenForRow(input);
        assertThat(preview).contains("bad substitution");
        assertThat(preview).contains("/Library/Java/jdk21");
        assertThat(preview).endsWith("(2 lines)");
    }

    @Test
    void flattenForRow_longSingleLine_isTruncatedWithEllipsis() {
        String input = "x".repeat(800);
        String preview = AgentMcpLogPanel.flattenForRow(input, 100);
        assertThat(preview).hasSize(101); // 100 chars + ellipsis
        assertThat(preview).endsWith("\u2026");
    }

    @Test
    void flattenForRow_longMultiLine_truncatesAndKeepsLineCount() {
        String input = "a".repeat(600) + "\n" + "b".repeat(600);
        String preview = AgentMcpLogPanel.flattenForRow(input, 50);
        assertThat(preview).startsWith("a");
        assertThat(preview).contains("\u2026");
        assertThat(preview).endsWith("(2 lines)");
    }

    @Test
    void flattenForRow_crlfLineEndings_areNormalised() {
        String input = "first\r\nsecond\r\nthird";
        String preview = AgentMcpLogPanel.flattenForRow(input);
        assertThat(preview).contains("first");
        assertThat(preview).contains("second");
        assertThat(preview).contains("third");
        assertThat(preview).endsWith("(3 lines)");
    }

    @Test
    void toHtmlTooltip_wrapsInHtmlAndConvertsNewlines() {
        String tooltip = AgentMcpLogPanel.toHtmlTooltip("alpha\nbeta");
        assertThat(tooltip).startsWith("<html>");
        assertThat(tooltip).endsWith("</html>");
        assertThat(tooltip).contains("alpha<br>beta");
    }

    @Test
    void toHtmlTooltip_escapesHtmlSpecials() {
        String tooltip = AgentMcpLogPanel.toHtmlTooltip("<script>alert(\"x\")</script> & done");
        assertThat(tooltip).doesNotContain("<script>");
        assertThat(tooltip).contains("&lt;script&gt;");
        assertThat(tooltip).contains("&amp; done");
    }

    @Test
    void toHtmlTooltip_truncatesVeryLongContentWithHint() {
        String tooltip = AgentMcpLogPanel.toHtmlTooltip("x".repeat(20_000), 100);
        assertThat(tooltip).contains("double-click to view full content");
    }
}
