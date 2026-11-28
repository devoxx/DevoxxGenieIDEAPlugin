package com.devoxx.genie.ui.trace.view;

import com.devoxx.genie.service.trace.TraceScanStatus;
import com.devoxx.genie.service.trace.view.AgentRun;
import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** The run view's text and tree, built from a small hand-made {@link AgentRun}. */
class AgentRunViewTest {

    private static AgentRun.ToolCallNode call(String name, String args, String result, boolean error, long ms,
                                              boolean repeat, List<AgentRun.SubAgentRun> subs) {
        return new AgentRun.ToolCallNode("id-" + name, name, args, result, error, ms, repeat,
                result != null && result.startsWith(AgentRun.TOOL_CALL_LIMIT_MARKER), subs);
    }

    private static AgentRun.Step step(int n, String text, String reasoning, AgentRun.ToolCallNode... calls) {
        return new AgentRun.Step(n, text, reasoning, "qwen3.8", 1_000L * n, 2_500, List.of(calls));
    }

    private static AgentRun sampleRun() {
        AgentRun.SubAgentRun sub = new AgentRun.SubAgentRun("sub-agent-1:ollama:qwen3", "look at a",
                List.of(step(1, "found a", null, call("search_files", "{}", "a", false, 100, false, List.of()))));
        AgentRun.Step first = step(1, "I'll look.\nSecond line", "the user wants…",
                call("search_files", "{\"pattern\":\"Hub\"}", "3 matches", false, 400, false, List.of()),
                call("parallel_explore", "{}", "combined", false, 9_000, false, List.of(sub)));
        AgentRun.Step second = step(2, "", null,
                call("search_files", "{\"pattern\":\"Hub\"}", "3 matches", false, 300, true, List.of()),
                call("read_file", "{\"path\":\"docs/hf-upload.md\"}", "Error: not found", true, 50, false, List.of()));
        AgentRun.Turn turn = new AgentRun.Turn(1, "t1", "Investigate the Hugging Face upload", 0,
                List.of(first, second), List.of());
        AgentRun.Summary summary = new AgentRun.Summary(List.of("qwen3.8"), 1, 3, 5, 1, 1, 134_000,
                Set.of(TraceScanStatus.CLEAN), false);
        return new AgentRun(summary, List.of(turn));
    }

    @Test
    void tree_nestsTurnsStepsCallsAndSubAgents() {
        DefaultMutableTreeNode root = AgentRunTreeBuilder.build(sampleRun());

        assertThat(root.getChildCount()).isEqualTo(1);
        DefaultMutableTreeNode turn = (DefaultMutableTreeNode) root.getChildAt(0);
        assertThat(turn.getUserObject()).isInstanceOf(AgentRun.Turn.class);
        assertThat(turn.getChildCount()).as("two steps").isEqualTo(2);

        DefaultMutableTreeNode step1 = (DefaultMutableTreeNode) turn.getChildAt(0);
        assertThat(step1.getChildCount()).as("two tool calls").isEqualTo(2);
        DefaultMutableTreeNode explore = (DefaultMutableTreeNode) step1.getChildAt(1);
        assertThat(((AgentRun.ToolCallNode) explore.getUserObject()).name()).isEqualTo("parallel_explore");
        DefaultMutableTreeNode sub = (DefaultMutableTreeNode) explore.getChildAt(0);
        assertThat(sub.getUserObject()).isInstanceOf(AgentRun.SubAgentRun.class);
        DefaultMutableTreeNode subStep = (DefaultMutableTreeNode) sub.getChildAt(0);
        assertThat(subStep.getChildCount()).as("the sub-agent's own tool call").isEqualTo(1);

        assertThat(AgentRunTreeBuilder.expandedByDefault(turn)).isTrue();
        assertThat(AgentRunTreeBuilder.expandedByDefault(step1)).isTrue();
        assertThat(AgentRunTreeBuilder.expandedByDefault(subStep)).as("sub-agents start collapsed").isFalse();
    }

    @Test
    void unattachedSubAgents_hangUnderTheTurn() {
        AgentRun.SubAgentRun orphan = new AgentRun.SubAgentRun("sub-agent-1", null, List.of());
        AgentRun.Turn turn = new AgentRun.Turn(1, "t", "q", 0, List.of(), List.of(orphan));
        AgentRun run = new AgentRun(new AgentRun.Summary(List.of(), 1, 0, 0, 0, 0, 0, Set.of(), false), List.of(turn));

        DefaultMutableTreeNode turnNode = (DefaultMutableTreeNode) AgentRunTreeBuilder.build(run).getChildAt(0);

        assertThat(((DefaultMutableTreeNode) turnNode.getChildAt(0)).getUserObject()).isSameAs(orphan);
    }

    @Test
    void rows_areSingleLine_andShowStatus() {
        AgentRun run = sampleRun();
        AgentRun.Turn turn = run.turns().get(0);
        AgentRun.Step first = turn.steps().get(0);
        AgentRun.Step second = turn.steps().get(1);

        AgentRunText.Row stepRow = AgentRunText.row(first);
        assertThat(stepRow.text()).isEqualTo("Step 1  I'll look. Second line").doesNotContain("\n");
        assertThat(stepRow.suffix()).isEqualTo("+2.5 s  · reasoning");

        assertThat(AgentRunText.row(second).text()).as("no text: shows the call count").isEqualTo("Step 2  (2 tool calls)");

        AgentRunText.Row repeat = AgentRunText.row(second.toolCalls().get(0));
        assertThat(repeat.text()).isEqualTo("search_files({\"pattern\":\"Hub\"})");
        assertThat(repeat.suffix()).isEqualTo("300 ms  ⟳ repeated");

        AgentRunText.Row failed = AgentRunText.row(second.toolCalls().get(1));
        assertThat(failed.style()).isEqualTo(AgentRunText.Style.ERROR);

        assertThat(AgentRunText.row(turn).text()).isEqualTo("Turn 1  Investigate the Hugging Face upload");
    }

    @Test
    void turnRow_showsWhatTheUserTyped_detailAlsoShowsTheFullMessage() {
        String sent = "<ProjectPath> ~/IdeaProjects/Demo</ProjectPath><UserPrompt> Investigate the upload\n</UserPrompt>";
        AgentRun.Turn turn = new AgentRun.Turn(1, "t", sent, 0, List.of(), List.of());

        assertThat(AgentRunText.row(turn).text()).isEqualTo("Turn 1  Investigate the upload");
        assertThat(AgentRunText.detail(turn))
                .contains("── Prompt ──\nInvestigate the upload")
                .contains("── Sent to the model ──\n" + sent);
        assertThat(AgentRunText.userPrompt("plain prompt")).isEqualTo("plain prompt");
    }

    @Test
    void longTextInARow_isCutWithAnEllipsis() {
        String longText = String.join(" ", Collections.nCopies(60, "word"));

        String line = AgentRunText.oneLine(longText, AgentRunText.ROW_TEXT_LIMIT);

        assertThat(line).hasSize(AgentRunText.ROW_TEXT_LIMIT).endsWith("…");
    }

    @Test
    void callWithoutResult_andLimit_areWarnings() {
        AgentRun.ToolCallNode pending = call("run_tests", "{}", null, false, -1, false, List.of());
        AgentRun.ToolCallNode limit = call("read_file", "{}", AgentRun.TOOL_CALL_LIMIT_MARKER + " (25).", false, 5, false, List.of());

        assertThat(AgentRunText.row(pending).style()).isEqualTo(AgentRunText.Style.WARNING);
        assertThat(AgentRunText.row(pending).suffix()).isEqualTo("no result");
        assertThat(AgentRunText.row(limit).style()).isEqualTo(AgentRunText.Style.WARNING);
        assertThat(AgentRunText.row(limit).suffix()).contains("hit the tool-call limit");
    }

    @Test
    void callDetail_showsPrettyArgumentsAndTheFullResult() {
        AgentRun.ToolCallNode call = call("read_file", "{\"path\":\"A.java\",\"limit\":10}",
                "line 1\nline 2", false, 1_500, true, List.of());

        String detail = AgentRunText.detail(call);

        assertThat(detail).startsWith("read_file · ok · 1.5 s · repeated");
        assertThat(detail).contains("── Arguments ──\n{\n  \"path\" : \"A.java\",");
        assertThat(detail).endsWith("── Result ──\nline 1\nline 2");
    }

    @Test
    void stepDetail_includesReasoningAndItsCalls() {
        AgentRun.Step step = sampleRun().turns().get(0).steps().get(0);

        String detail = AgentRunText.detail(step);

        assertThat(detail).startsWith("Step 1 · qwen3.8 · +2.5 s");
        assertThat(detail).contains("── Reasoning ──\nthe user wants…");
        assertThat(detail).contains("- parallel_explore({})");
    }

    @Test
    void summary_readsAsOneLine() {
        assertThat(AgentRunText.summary(sampleRun().summary()))
                .isEqualTo("qwen3.8 · 1 turn · 3 steps · 5 tool calls · 1 failed · 1 repeated · 2 m 14 s · scan: CLEAN");
    }

    @Test
    void durations() {
        assertThat(AgentRunText.duration(450)).isEqualTo("450 ms");
        assertThat(AgentRunText.duration(3_210)).isEqualTo("3.2 s");
        assertThat(AgentRunText.duration(134_000)).isEqualTo("2 m 14 s");
    }
}
