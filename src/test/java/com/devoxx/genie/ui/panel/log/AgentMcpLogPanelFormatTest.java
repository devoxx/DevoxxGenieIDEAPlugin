package com.devoxx.genie.ui.panel.log;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.agent.AgentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the row-preview and clipboard helpers used by {@link AgentMcpLogPanel}.
 * These guard the contract that:
 * <ul>
 *   <li>panel rows flatten multi-line output into bounded single-line previews,</li>
 *   <li>copy-to-clipboard preserves the original newlines verbatim under the entry header,</li>
 *   <li>the detailed editor view retains the complete content.</li>
 * </ul>
 */
class AgentMcpLogPanelFormatTest {

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

    // --- formatAgentActivityMessage -------------------------------------------------------

    @Test
    void formatAgentActivityRow_toolErrorWithMultiLineResult_isSingleLine() {
        ActivityMessage err = ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.TOOL_ERROR)
                .toolName("run_command")
                .result("Error: failed to spawn process\nstacktrace line 1\nstacktrace line 2")
                .callNumber(2)
                .maxCalls(25)
                .build();
        String row = AgentMcpLogPanel.formatAgentActivityRow(err);
        assertThat(row).isEqualTo(
                "[2/25] \u2716 run_command \u2192 Error: failed to spawn process"
                        + " ↵ stacktrace line 1 ↵ stacktrace line 2");
        assertThat(row).doesNotContain("\n");
    }

    @Test
    void formatAgentActivityRow_subAgentErrorWithMultiLineResult_isSingleLine() {
        ActivityMessage err = ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.SUB_AGENT_ERROR)
                .subAgentId("explorer-1")
                .result("first error line\nsecond error line")
                .callNumber(1)
                .maxCalls(25)
                .build();
        String row = AgentMcpLogPanel.formatAgentActivityRow(err);
        assertThat(row).isEqualTo(
                "[1/25] [explorer-1] \u2716 Sub-agent error: explorer-1 \u2192 "
                        + "first error line ↵ second error line");
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
    void formatAgentActivityRow_systemPrompt_hasNoCallPrefixAndRendersBody() {
        ActivityMessage prompt = ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.SYSTEM_PROMPT)
                .result("You are a helpful assistant.\n<ProjectContext>\nrules\n</ProjectContext>")
                .build();
        String row = AgentMcpLogPanel.formatAgentActivityRow(prompt);
        // No "[n/n]" call-count prefix for an informational system-prompt entry.
        assertThat(row).doesNotContain("[0/0]");
        assertThat(row).isEqualTo(
                "\ud83d\udccb System prompt ↵ You are a helpful assistant. ↵ "
                        + "<ProjectContext> ↵ rules ↵ </ProjectContext>");
    }
}
