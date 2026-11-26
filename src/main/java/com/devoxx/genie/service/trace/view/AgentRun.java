package com.devoxx.genie.service.trace.view;

import com.devoxx.genie.service.trace.TraceScanStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * One conversation's recorded agent trace (task-279), shaped for reading (TASK-283): turns, the
 * assistant steps in each turn, the tool calls of each step with their results, and the
 * sub-agent runs started by {@code parallel_explore}. Built by {@link AgentRunModelBuilder}.
 */
public record AgentRun(@NotNull Summary summary, @NotNull List<Turn> turns) {

    /** Marks a tool result as the agent's tool-call limit; see {@code AgentLoopTracker}. */
    public static final String TOOL_CALL_LIMIT_MARKER = "Error: Agent loop limit reached";

    public AgentRun {
        turns = List.copyOf(turns);
    }

    /**
     * Totals over the whole run, sub-agents included.
     *
     * @param durationMs      first to last recorded event; 0 for a single event
     * @param hitToolCallLimit whether any tool result was the agent's tool-call limit message
     */
    public record Summary(@NotNull List<String> models, int turns, int steps, int toolCalls, int failedToolCalls,
                          int repeatedToolCalls, long durationMs, @NotNull Set<TraceScanStatus> scanStatuses,
                          boolean hitToolCallLimit) {
        public Summary {
            models = List.copyOf(models);
            scanStatuses = Set.copyOf(scanStatuses);
        }
    }

    /**
     * One user prompt and what the agent did with it.
     *
     * @param unattachedSubAgents sub-agent runs that could not be placed under a {@code parallel_explore} call
     */
    public record Turn(int number, @NotNull String turnId, @Nullable String prompt, long timestampMs,
                       @NotNull List<Step> steps, @NotNull List<SubAgentRun> unattachedSubAgents) {
        public Turn {
            steps = List.copyOf(steps);
            unattachedSubAgents = List.copyOf(unattachedSubAgents);
        }
    }

    /**
     * One model response: its text, reasoning and the tool calls it asked for.
     *
     * @param sincePreviousMs time since the previous step, or since the prompt for the first step
     */
    public record Step(int number, @Nullable String text, @Nullable String reasoning, @Nullable String model,
                       long timestampMs, long sincePreviousMs, @NotNull List<ToolCallNode> toolCalls) {
        public Step {
            toolCalls = List.copyOf(toolCalls);
        }
    }

    /**
     * One tool call and its result.
     *
     * @param result         {@code null} when no result was recorded for the call
     * @param durationMs     from the step that asked for the call to its result; -1 without a result
     * @param repeat         the same tool was called with the same arguments earlier in the same run
     * @param toolCallLimit  the result is the agent's tool-call limit message
     * @param subAgents      sub-agent runs started by this call ({@code parallel_explore} only)
     */
    public record ToolCallNode(@Nullable String id, @NotNull String name, @Nullable String arguments,
                               @Nullable String result, boolean error, long durationMs, boolean repeat,
                               boolean toolCallLimit, @NotNull List<SubAgentRun> subAgents) {
        public ToolCallNode {
            subAgents = List.copyOf(subAgents);
        }

        public boolean hasResult() {
            return result != null;
        }
    }

    /** The steps of one sub-agent, identified by its label (e.g. {@code sub-agent-1:ollama:qwen3}). */
    public record SubAgentRun(@NotNull String label, @Nullable String prompt, @NotNull List<Step> steps) {
        public SubAgentRun {
            steps = List.copyOf(steps);
        }
    }
}
