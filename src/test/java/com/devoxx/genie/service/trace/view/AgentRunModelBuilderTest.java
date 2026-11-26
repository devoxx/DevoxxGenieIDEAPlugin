package com.devoxx.genie.service.trace.view;

import com.devoxx.genie.service.trace.StoredTraceEvent;
import com.devoxx.genie.service.trace.TraceEvent;
import com.devoxx.genie.service.trace.TraceScanStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunModelBuilderTest {

    private static final String SUB1 = "sub-agent-1:ollama:qwen3";
    private static final String SUB2 = "sub-agent-2:ollama:qwen3";

    /** Builds rows in insertion order, like trace_events read back by id. */
    private static final class Rows {
        private final List<StoredTraceEvent> rows = new ArrayList<>();
        private TraceScanStatus status = TraceScanStatus.CLEAN;
        private String turn = "turn-1";

        Rows turn(String turnId) {
            this.turn = turnId;
            return this;
        }

        Rows status(TraceScanStatus status) {
            this.status = status;
            return this;
        }

        Rows user(long t, String text) {
            return add(TraceEvent.user(text, null, t));
        }

        Rows assistant(long t, String text, TraceEvent.ToolCall... calls) {
            return add(TraceEvent.assistant(text, null, List.of(calls), "qwen3.8", null, t));
        }

        Rows assistantWithReasoning(long t, String text, String reasoning) {
            return add(TraceEvent.assistant(text, reasoning, List.of(), "qwen3.8", null, t));
        }

        Rows tool(long t, String id, String name, String result, boolean error) {
            return add(TraceEvent.tool(id, name, result, error, null, t));
        }

        Rows subUser(long t, String sub, String text) {
            return add(TraceEvent.user(text, sub, t));
        }

        Rows subAssistant(long t, String sub, String text, TraceEvent.ToolCall... calls) {
            return add(TraceEvent.assistant(text, null, List.of(calls), "qwen3:0.6b", sub, t));
        }

        Rows subTool(long t, String sub, String id, String name, String result) {
            return add(TraceEvent.tool(id, name, result, false, sub, t));
        }

        private Rows add(TraceEvent event) {
            rows.add(new StoredTraceEvent(rows.size() + 1, "conv", turn, event, status));
            return this;
        }

        AgentRun build() {
            return AgentRunModelBuilder.build(rows);
        }
    }

    private static TraceEvent.ToolCall call(String id, String name, String args) {
        return new TraceEvent.ToolCall(id, name, args);
    }

    @Test
    void emptyTrace_givesAnEmptyRun() {
        AgentRun run = AgentRunModelBuilder.build(List.of());

        assertThat(run.turns()).isEmpty();
        assertThat(run.summary().steps()).isZero();
        assertThat(run.summary().durationMs()).isZero();
    }

    @Test
    void oneTurn_groupsPromptStepsAndToolCalls_inRecordedOrder() {
        AgentRun run = new Rows()
                .user(1_000, "what's the weather")
                .assistant(4_000, "I'll check.", call("c1", "run_command", "{\"command\":\"date\"}"),
                        call("c2", "web_search", "{\"query\":\"weather\"}"))
                .tool(5_000, "c1", "run_command", "Thursday", false)
                .tool(5_500, "c2", "web_search", "Sunny", false)
                .assistant(9_000, "It's sunny.")
                .build();

        assertThat(run.turns()).hasSize(1);
        AgentRun.Turn turn = run.turns().get(0);
        assertThat(turn.prompt()).isEqualTo("what's the weather");
        assertThat(turn.steps()).extracting(AgentRun.Step::text).containsExactly("I'll check.", "It's sunny.");
        assertThat(turn.steps()).extracting(AgentRun.Step::number).containsExactly(1, 2);

        List<AgentRun.ToolCallNode> calls = turn.steps().get(0).toolCalls();
        assertThat(calls).extracting(AgentRun.ToolCallNode::name).containsExactly("run_command", "web_search");
        assertThat(calls).extracting(AgentRun.ToolCallNode::result).containsExactly("Thursday", "Sunny");
        assertThat(turn.steps().get(1).toolCalls()).isEmpty();
    }

    @Test
    void resultsAreMatchedByToolCallId_notByOrder() {
        AgentRun run = new Rows()
                .user(0, "q")
                .assistant(10, "", call("a", "search_files", "{}"), call("b", "read_file", "{}"))
                .tool(20, "b", "read_file", "file body", false)
                .tool(30, "a", "search_files", "3 matches", false)
                .build();

        List<AgentRun.ToolCallNode> calls = run.turns().get(0).steps().get(0).toolCalls();
        assertThat(calls.get(0).result()).isEqualTo("3 matches");
        assertThat(calls.get(1).result()).isEqualTo("file body");
    }

    @Test
    void callWithoutAResult_isKeptWithNoResultAndUnknownDuration() {
        AgentRun run = new Rows()
                .user(0, "q")
                .assistant(10, "", call("a", "run_tests", "{}"))
                .build();

        AgentRun.ToolCallNode call = run.turns().get(0).steps().get(0).toolCalls().get(0);
        assertThat(call.hasResult()).isFalse();
        assertThat(call.durationMs()).isEqualTo(-1);
    }

    @Test
    void timings_stepSincePrevious_andToolDuration() {
        AgentRun run = new Rows()
                .user(1_000, "q")
                .assistant(3_000, "", call("a", "run_tests", "{}"))
                .tool(15_000, "a", "run_tests", "ok", false)
                .assistant(16_000, "done")
                .build();

        List<AgentRun.Step> steps = run.turns().get(0).steps();
        assertThat(steps.get(0).sincePreviousMs()).as("since the prompt").isEqualTo(2_000);
        assertThat(steps.get(1).sincePreviousMs()).as("since the previous step").isEqualTo(13_000);
        assertThat(steps.get(0).toolCalls().get(0).durationMs()).isEqualTo(12_000);
        assertThat(run.summary().durationMs()).isEqualTo(15_000);
    }

    @Test
    void failedCalls_areMarked_andCounted() {
        AgentRun run = new Rows()
                .user(0, "q")
                .assistant(10, "", call("a", "read_file", "{\"path\":\"docs/hf-upload.md\"}"))
                .tool(20, "a", "read_file", "Error: file not found", true)
                .build();

        assertThat(run.turns().get(0).steps().get(0).toolCalls().get(0).error()).isTrue();
        assertThat(run.summary().failedToolCalls()).isEqualTo(1);
    }

    @Test
    void sameToolWithSameArguments_isFlaggedAsRepeat_differentArgumentsAreNot() {
        AgentRun run = new Rows()
                .user(0, "q")
                .assistant(10, "", call("a", "search_files", "{\"pattern\":\"Hub\"}"))
                .tool(20, "a", "search_files", "x", false)
                .assistant(30, "", call("b", "search_files", "{\"pattern\":\"Hub\"}"),
                        call("c", "search_files", "{\"pattern\":\"commit\"}"))
                .tool(40, "b", "search_files", "x", false)
                .tool(41, "c", "search_files", "y", false)
                .build();

        List<AgentRun.Step> steps = run.turns().get(0).steps();
        assertThat(steps.get(0).toolCalls().get(0).repeat()).isFalse();
        assertThat(steps.get(1).toolCalls()).extracting(AgentRun.ToolCallNode::repeat).containsExactly(true, false);
        assertThat(run.summary().repeatedToolCalls()).isEqualTo(1);
    }

    @Test
    void repeatDetection_ignoresJsonWhitespace() {
        AgentRun run = new Rows()
                .user(0, "q")
                .assistant(10, "", call("a", "read_file", "{\"path\": \"A.java\"}"))
                .tool(20, "a", "read_file", "x", false)
                .assistant(30, "", call("b", "read_file", "{\"path\":\"A.java\"}"))
                .tool(40, "b", "read_file", "x", false)
                .build();

        assertThat(run.turns().get(0).steps().get(1).toolCalls().get(0).repeat()).isTrue();
    }

    @Test
    void toolCallLimitResult_isFlagged_onTheCallAndTheSummary() {
        AgentRun run = new Rows()
                .user(0, "q")
                .assistant(10, "", call("a", "read_file", "{}"))
                .tool(20, "a", "read_file", AgentRun.TOOL_CALL_LIMIT_MARKER + " (25 calls).", false)
                .build();

        assertThat(run.turns().get(0).steps().get(0).toolCalls().get(0).toolCallLimit()).isTrue();
        assertThat(run.summary().hitToolCallLimit()).isTrue();
    }

    @Test
    void reasoningAndModel_areKeptOnTheStep() {
        AgentRun run = new Rows()
                .user(0, "q")
                .assistantWithReasoning(10, "answer", "the user wants…")
                .build();

        AgentRun.Step step = run.turns().get(0).steps().get(0);
        assertThat(step.reasoning()).isEqualTo("the user wants…");
        assertThat(step.model()).isEqualTo("qwen3.8");
        assertThat(run.summary().models()).containsExactly("qwen3.8");
    }

    @Test
    void severalTurns_areNumbered_andRepeatsCountAcrossTurns() {
        AgentRun run = new Rows()
                .turn("t1").user(0, "first")
                .assistant(10, "", call("a", "read_file", "{\"path\":\"A\"}"))
                .tool(20, "a", "read_file", "x", false)
                .assistant(30, "one")
                .turn("t2").user(100, "second")
                .assistant(110, "", call("b", "read_file", "{\"path\":\"A\"}"))
                .tool(120, "b", "read_file", "x", false)
                .assistant(130, "two")
                .build();

        assertThat(run.turns()).extracting(AgentRun.Turn::number).containsExactly(1, 2);
        assertThat(run.turns()).extracting(AgentRun.Turn::prompt).containsExactly("first", "second");
        assertThat(run.turns().get(1).steps().get(0).toolCalls().get(0).repeat()).isTrue();
        assertThat(run.summary().turns()).isEqualTo(2);
        assertThat(run.summary().steps()).isEqualTo(4);
        assertThat(run.summary().toolCalls()).isEqualTo(2);
    }

    @Test
    void interleavedSubAgents_areNestedUnderTheParallelExploreCall_thatStartedThem() {
        AgentRun run = new Rows()
                .user(0, "explore")
                .assistant(10, "", call("p", "parallel_explore", "{\"queries\":[\"a\",\"b\"]}"))
                .subUser(11, SUB1, "look at a")
                .subUser(12, SUB2, "look at b")
                .subAssistant(20, SUB1, "", call("s1", "search_files", "{\"pattern\":\"a\"}"))
                .subAssistant(21, SUB2, "", call("s2", "search_files", "{\"pattern\":\"b\"}"))
                .subTool(30, SUB2, "s2", "search_files", "b found")
                .subTool(31, SUB1, "s1", "search_files", "a found")
                .subAssistant(40, SUB1, "a summary")
                .subAssistant(41, SUB2, "b summary")
                .tool(50, "p", "parallel_explore", "combined", false)
                .assistant(60, "final")
                .build();

        AgentRun.Turn turn = run.turns().get(0);
        assertThat(turn.steps()).as("sub-agent steps are not main steps").hasSize(2);
        AgentRun.ToolCallNode explore = turn.steps().get(0).toolCalls().get(0);
        assertThat(explore.result()).isEqualTo("combined");
        assertThat(explore.subAgents()).extracting(AgentRun.SubAgentRun::label).containsExactly(SUB1, SUB2);

        AgentRun.SubAgentRun first = explore.subAgents().get(0);
        assertThat(first.prompt()).isEqualTo("look at a");
        assertThat(first.steps()).extracting(AgentRun.Step::text).containsExactly("", "a summary");
        assertThat(first.steps().get(0).toolCalls().get(0).result()).isEqualTo("a found");
        assertThat(explore.subAgents().get(1).steps().get(0).toolCalls().get(0).result()).isEqualTo("b found");

        assertThat(turn.unattachedSubAgents()).isEmpty();
        assertThat(run.summary().steps()).as("all steps, sub-agents included").isEqualTo(6);
        assertThat(run.summary().toolCalls()).isEqualTo(3);
        assertThat(run.summary().models()).containsExactly("qwen3.8", "qwen3:0.6b");
    }

    @Test
    void subAgentsOfTwoParallelExploreCalls_goToTheRightCall() {
        AgentRun run = new Rows()
                .user(0, "q")
                .assistant(10, "", call("p1", "parallel_explore", "{}"))
                .subAssistant(20, SUB1, "first round")
                .tool(30, "p1", "parallel_explore", "r1", false)
                .assistant(40, "", call("p2", "parallel_explore", "{}"))
                .subAssistant(50, SUB1, "second round")
                .tool(60, "p2", "parallel_explore", "r2", false)
                .build();

        List<AgentRun.Step> steps = run.turns().get(0).steps();
        assertThat(steps.get(0).toolCalls().get(0).subAgents().get(0).steps())
                .extracting(AgentRun.Step::text).containsExactly("first round");
        assertThat(steps.get(1).toolCalls().get(0).subAgents().get(0).steps())
                .extracting(AgentRun.Step::text).containsExactly("second round");
    }

    @Test
    void subAgentRowsWithoutAParallelExploreCall_areKeptAsUnattached() {
        AgentRun run = new Rows()
                .user(0, "q")
                .subAssistant(5, SUB1, "orphan")
                .assistant(10, "done")
                .build();

        assertThat(run.turns().get(0).unattachedSubAgents()).extracting(AgentRun.SubAgentRun::label)
                .containsExactly(SUB1);
    }

    @Test
    void scanStatuses_areCollected() {
        AgentRun run = new Rows()
                .turn("t1").status(TraceScanStatus.NOT_SCANNED).user(0, "a").assistant(1, "x")
                .turn("t2").status(TraceScanStatus.CLEAN).user(2, "b").assistant(3, "y")
                .build();

        assertThat(run.summary().scanStatuses())
                .containsExactlyInAnyOrder(TraceScanStatus.NOT_SCANNED, TraceScanStatus.CLEAN);
    }

    @Test
    void largeRun_buildsQuickly() {
        Rows rows = new Rows().user(0, "q");
        for (int i = 0; i < 400; i++) {
            rows.assistant(i * 10L + 1, "step " + i, call("c" + i, "read_file", "{\"path\":\"F" + (i % 20) + "\"}"));
            rows.tool(i * 10L + 5, "c" + i, "read_file", "x".repeat(2_000), false);
        }

        long start = System.nanoTime();
        AgentRun run = rows.build();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(run.summary().toolCalls()).isEqualTo(400);
        assertThat(run.summary().repeatedToolCalls()).isEqualTo(380);
        assertThat(elapsedMs).as("building the model must not be a bottleneck").isLessThan(1_000);
    }
}
