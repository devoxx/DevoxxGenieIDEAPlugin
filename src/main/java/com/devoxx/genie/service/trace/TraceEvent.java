package com.devoxx.genie.service.trace;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One recorded step of an agent turn (task-279). Roles mirror the Hugging Face Session Trace
 * format so a later export (task-280) can map them one-to-one:
 * <ul>
 *   <li>{@link Role#USER}: the prompt.</li>
 *   <li>{@link Role#ASSISTANT}: one model response — text, reasoning and the tool calls it asked for.</li>
 *   <li>{@link Role#TOOL}: the result of one tool call, linked by {@link #toolCallId}.</li>
 * </ul>
 */
public record TraceEvent(
        @NotNull Role role,
        @Nullable String content,
        @Nullable String reasoning,
        @NotNull List<ToolCall> toolCalls,
        @Nullable String toolCallId,
        @Nullable String toolName,
        boolean error,
        @Nullable String model,
        @Nullable String subAgentId,
        long timestampMs) {

    public enum Role { USER, ASSISTANT, TOOL }

    /** A tool call requested by the model. {@code arguments} is the raw JSON string the model sent. */
    public record ToolCall(@Nullable String id, @NotNull String name, @Nullable String arguments) {}

    public TraceEvent {
        toolCalls = List.copyOf(toolCalls);
    }

    public static @NotNull TraceEvent user(@Nullable String content, @Nullable String subAgentId, long timestampMs) {
        return new TraceEvent(Role.USER, content, null, List.of(), null, null, false, null, subAgentId, timestampMs);
    }

    public static @NotNull TraceEvent assistant(@Nullable String content, @Nullable String reasoning,
                                                @NotNull List<ToolCall> toolCalls, @Nullable String model,
                                                @Nullable String subAgentId, long timestampMs) {
        return new TraceEvent(Role.ASSISTANT, content, reasoning, toolCalls, null, null, false, model, subAgentId, timestampMs);
    }

    public static @NotNull TraceEvent tool(@Nullable String toolCallId, @NotNull String toolName, @Nullable String result,
                                           boolean error, @Nullable String subAgentId, long timestampMs) {
        return new TraceEvent(Role.TOOL, result, null, List.of(), toolCallId, toolName, error, null, subAgentId, timestampMs);
    }

    /** Copy with every free-text field passed through {@code f} (content, reasoning, tool arguments). */
    public @NotNull TraceEvent mapText(@NotNull java.util.function.UnaryOperator<String> f) {
        List<ToolCall> calls = toolCalls.stream()
                .map(c -> new ToolCall(c.id(), c.name(), c.arguments() == null ? null : f.apply(c.arguments())))
                .toList();
        return new TraceEvent(role, content == null ? null : f.apply(content),
                reasoning == null ? null : f.apply(reasoning), calls, toolCallId, toolName, error, model,
                subAgentId, timestampMs);
    }
}
