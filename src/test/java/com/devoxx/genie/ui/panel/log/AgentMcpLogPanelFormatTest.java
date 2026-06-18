package com.devoxx.genie.ui.panel.log;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.agent.AgentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the row-preview, clipboard, and tooltip helpers used by {@link AgentMcpLogPanel}.
 * These guard the contract that:
 * <ul>
 *   <li>panel rows render multi-line tool output across real lines (no {@code ⏎} markers),</li>
 *   <li>copy-to-clipboard preserves the original newlines verbatim under the entry header,</li>
 *   <li>the hover tooltip surfaces the full multi-line content as HTML.</li>
 * </ul>
 */
class AgentMcpLogPanelFormatTest {

    // --- formatForRow ---------------------------------------------------------------------

    @Test
    void formatForRow_singleLine_returnsAsIs() {
        assertThat(AgentMcpLogPanel.formatForRow("hello")).isEqualTo("hello");
    }

    @Test
    void formatForRow_emptyString_returnsEmpty() {
        assertThat(AgentMcpLogPanel.formatForRow("")).isEmpty();
    }

    @Test
    void formatForRow_trailingNewlineOnly_isTreatedAsSingleLine() {
        // RunCommandToolExecutor appends a trailing '\n' to each line, so a single-line result
        // ends with '\n'. That trailing empty segment must not be reported as a second line.
        assertThat(AgentMcpLogPanel.formatForRow("output\n")).isEqualTo("output");
    }

    @Test
    void formatForRow_multiLine_keepsRealNewlinesNoMarker() {
        String input = "line1\nline2\nline3";
        String preview = AgentMcpLogPanel.formatForRow(input);
        assertThat(preview).isEqualTo("line1\nline2\nline3");
        assertThat(preview).doesNotContain("⏎");
    }

    @Test
    void formatForRow_realWorldRunCommandOutput_showsBothStderrAndStdout() {
        // Reproduces the bug pattern from #1027 follow-up: the agent log row used to collapse
        // both lines into one with a ⏎ marker. With multi-line rendering both lines appear on
        // their own line in the panel.
        String input = "/Users/x/.sdkman/path-helpers.sh: line 61: ${name^^}: bad substitution\n"
                + "/Library/Java/jdk21\n";
        String preview = AgentMcpLogPanel.formatForRow(input);
        assertThat(preview.split("\n")).containsExactly(
                "/Users/x/.sdkman/path-helpers.sh: line 61: ${name^^}: bad substitution",
                "/Library/Java/jdk21"
        );
    }

    @Test
    void formatForRow_longSingleLine_isTruncatedWithEllipsis() {
        String input = "x".repeat(800);
        String preview = AgentMcpLogPanel.formatForRow(input, 100, 10);
        assertThat(preview).hasSize(101); // 100 chars + ellipsis
        assertThat(preview).endsWith("…");
    }

    @Test
    void formatForRow_exceedingMaxLines_isCappedWithMoreLinesHint() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 25; i++) sb.append("line").append(i).append('\n');
        String preview = AgentMcpLogPanel.formatForRow(sb.toString(), 500, 10);
        String[] lines = preview.split("\n");
        assertThat(lines).hasSize(11); // 10 content lines + 1 hint line
        assertThat(lines[0]).isEqualTo("line0");
        assertThat(lines[9]).isEqualTo("line9");
        assertThat(lines[10]).isEqualTo("… (15 more lines)");
    }

    @Test
    void formatForRow_crlfLineEndings_areNormalised() {
        String input = "first\r\nsecond\r\nthird";
        String preview = AgentMcpLogPanel.formatForRow(input);
        assertThat(preview).isEqualTo("first\nsecond\nthird");
    }

    // --- formatForClipboard ---------------------------------------------------------------

    @Test
    void formatForClipboard_singleLine_returnsAsIs() {
        assertThat(AgentMcpLogPanel.formatForClipboard("hello")).isEqualTo("hello");
    }

    @Test
    void formatForClipboard_emptyString_returnsEmpty() {
        assertThat(AgentMcpLogPanel.formatForClipboard("")).isEmpty();
    }

    @Test
    void formatForClipboard_trailingNewlineOnly_isTreatedAsSingleLine() {
        assertThat(AgentMcpLogPanel.formatForClipboard("output\n")).isEqualTo("output");
    }

    @Test
    void formatForClipboard_multiLine_indentsUnderHeaderWithRealNewlines() {
        // Multi-line content is moved onto subsequent indented lines so the entry header
        // (timestamp, tool name) stays on its own line and the body is unambiguously attached.
        String input = "UID PID CMD\n0 1 launchd\n0 2 logd";
        String formatted = AgentMcpLogPanel.formatForClipboard(input);
        assertThat(formatted).isEqualTo(
                "\n    UID PID CMD" +
                "\n    0 1 launchd" +
                "\n    0 2 logd"
        );
    }

    @Test
    void formatForClipboard_doesNotTruncateVeryLongOutput() {
        String input = "x".repeat(20_000);
        assertThat(AgentMcpLogPanel.formatForClipboard(input)).isEqualTo("x".repeat(20_000));
    }

    @Test
    void formatForClipboard_crlfLineEndings_areNormalised() {
        String input = "first\r\nsecond";
        assertThat(AgentMcpLogPanel.formatForClipboard(input)).isEqualTo("\n    first\n    second");
    }

    @Test
    void formatForRow_doubleTrailingNewline_countsAsOneLine() {
        // "a\n\n" should collapse to a single-line result, not show a "(N more lines)" hint.
        String preview = AgentMcpLogPanel.formatForRow("a\n\n");
        assertThat(preview).isEqualTo("a");
        assertThat(preview).doesNotContain("more lines");
    }

    @Test
    void formatForRow_manyTrailingNewlines_countsAsOneLine() {
        String preview = AgentMcpLogPanel.formatForRow("only-real-line\n\n\n\n");
        assertThat(preview).isEqualTo("only-real-line");
    }

    // --- formatAgentActivityMessage -------------------------------------------------------

    @Test
    void formatAgentActivityMessage_toolErrorWithMultiLineResult_rendersAcrossLines() {
        ActivityMessage err = ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.TOOL_ERROR)
                .toolName("run_command")
                .result("Error: failed to spawn process\nstacktrace line 1\nstacktrace line 2")
                .callNumber(2)
                .maxCalls(25)
                .build();
        String row = AgentMcpLogPanel.formatAgentActivityMessage(err, AgentMcpLogPanel::formatForRow);
        assertThat(row).startsWith("[2/25] \u2716 run_command \u2192 ");
        assertThat(row.split("\n")).containsExactly(
                "[2/25] \u2716 run_command \u2192 Error: failed to spawn process",
                "stacktrace line 1",
                "stacktrace line 2"
        );
        assertThat(row).doesNotContain("\u23ce");
    }

    @Test
    void formatAgentActivityMessage_subAgentErrorWithMultiLineResult_rendersAcrossLines() {
        ActivityMessage err = ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.SUB_AGENT_ERROR)
                .subAgentId("explorer-1")
                .result("first error line\nsecond error line")
                .callNumber(1)
                .maxCalls(25)
                .build();
        String row = AgentMcpLogPanel.formatAgentActivityMessage(err, AgentMcpLogPanel::formatForRow);
        assertThat(row).contains("Sub-agent error: explorer-1");
        assertThat(row.split("\n")).containsExactly(
                "[1/25] [explorer-1] \u2716 Sub-agent error: explorer-1 \u2192 first error line",
                "second error line"
        );
    }

    @Test
    void formatAgentActivityMessage_toolErrorWithMultiLineResult_clipboardIndentsBody() {
        ActivityMessage err = ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.TOOL_ERROR)
                .toolName("run_command")
                .result("Error: failed to spawn process\nstacktrace line 1")
                .callNumber(2)
                .maxCalls(25)
                .build();
        String row = AgentMcpLogPanel.formatAgentActivityMessage(err, AgentMcpLogPanel::formatForClipboard);
        assertThat(row).isEqualTo(
                "[2/25] \u2716 run_command \u2192 " +
                "\n    Error: failed to spawn process" +
                "\n    stacktrace line 1"
        );
    }

    @Test
    void formatAgentActivityMessage_systemPrompt_hasNoCallPrefixAndRendersBody() {
        ActivityMessage prompt = ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.SYSTEM_PROMPT)
                .result("You are a helpful assistant.\n<ProjectContext>\nrules\n</ProjectContext>")
                .build();
        String row = AgentMcpLogPanel.formatAgentActivityMessage(prompt, AgentMcpLogPanel::formatForRow);
        // No "[n/n]" call-count prefix for an informational system-prompt entry.
        assertThat(row).doesNotContain("[0/0]");
        assertThat(row.split("\n")).containsExactly(
                "\ud83d\udccb System prompt",
                "You are a helpful assistant.",
                "<ProjectContext>",
                "rules",
                "</ProjectContext>"
        );
    }
}
