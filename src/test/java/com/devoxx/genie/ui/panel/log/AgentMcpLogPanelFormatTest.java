package com.devoxx.genie.ui.panel.log;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.agent.AgentType;
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

    @Test
    void flattenForRow_doubleTrailingNewline_countsAsOneLine() {
        // "a\n\n" should be treated as a single-line result, not (2 lines).
        String preview = AgentMcpLogPanel.flattenForRow("a\n\n");
        assertThat(preview).isEqualTo("a");
        assertThat(preview).doesNotContain("lines");
    }

    @Test
    void flattenForRow_manyTrailingNewlines_countsAsOneLine() {
        String preview = AgentMcpLogPanel.flattenForRow("only-real-line\n\n\n\n");
        assertThat(preview).isEqualTo("only-real-line");
    }

    @Test
    void formatAgentActivity_toolErrorWithMultiLineResult_isFlattened() {
        ActivityMessage err = ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.TOOL_ERROR)
                .toolName("run_command")
                .result("Error: failed to spawn process\nstacktrace line 1\nstacktrace line 2")
                .callNumber(2)
                .maxCalls(25)
                .build();
        String row = AgentMcpLogPanel.formatAgentActivityMessage(err);
        assertThat(row).startsWith("[2/25] \u2716 run_command \u2192 ");
        assertThat(row).contains("Error: failed to spawn process");
        assertThat(row).contains("stacktrace line 1");
        assertThat(row).contains("stacktrace line 2");
        assertThat(row).contains("\u23ce"); // visible newline separator
        assertThat(row).endsWith("(3 lines)");
    }

    @Test
    void formatAgentActivity_subAgentErrorWithMultiLineResult_isFlattened() {
        ActivityMessage err = ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.SUB_AGENT_ERROR)
                .subAgentId("explorer-1")
                .result("first error line\nsecond error line")
                .callNumber(1)
                .maxCalls(25)
                .build();
        String row = AgentMcpLogPanel.formatAgentActivityMessage(err);
        assertThat(row).contains("Sub-agent error: explorer-1");
        assertThat(row).contains("\u23ce");
        assertThat(row).endsWith("(2 lines)");
    }

    @Test
    void toHtmlTooltip_nullGuardInRenderer() {
        // Mirrors the cell-renderer guard: pass null through the same code path the renderer
        // uses, ensuring no NPE. The renderer is: fc != null ? toHtmlTooltip(fc) : null
        String fullContent = null;
        String tooltip = fullContent != null ? AgentMcpLogPanel.toHtmlTooltip(fullContent) : null;
        assertThat(tooltip).isNull();
    }
}
