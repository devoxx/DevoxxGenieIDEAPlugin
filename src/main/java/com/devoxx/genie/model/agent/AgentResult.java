package com.devoxx.genie.model.agent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The structured outcome of a single delegated agent run — the in-process analog of the
 * DockerAgents {@code result.json} contract. A run ALWAYS produces one of these on every
 * exit path (success, error, timeout, cancellation) so a delegating parent never blocks
 * on an unreadable child.
 * <p>
 * {@code summary} is the only content a parent should read — children are instructed to
 * make it a terse, self-contained synthesis, never a transcript.
 */
public record AgentResult(
        @NotNull String agent,
        @Nullable String intent,
        @NotNull Status status,
        @NotNull String summary,
        int toolCalls,
        long durationMs,
        @Nullable String provider,
        @Nullable String model) {

    public enum Status {
        OK, ERROR, TIMEOUT, CANCELLED
    }

    public static @NotNull AgentResult ok(@NotNull String agent, @Nullable String intent,
                                          @NotNull String summary, int toolCalls, long durationMs,
                                          @Nullable String provider, @Nullable String model) {
        return new AgentResult(agent, intent, Status.OK, summary, toolCalls, durationMs, provider, model);
    }

    public static @NotNull AgentResult error(@NotNull String agent, @Nullable String intent,
                                             @NotNull String summary, int toolCalls, long durationMs,
                                             @Nullable String provider, @Nullable String model) {
        return new AgentResult(agent, intent, Status.ERROR, summary, toolCalls, durationMs, provider, model);
    }

    public static @NotNull AgentResult timeout(@NotNull String agent, @Nullable String intent,
                                               int timeoutSeconds) {
        return new AgentResult(agent, intent, Status.TIMEOUT,
                "Agent '" + agent + "' timed out after " + timeoutSeconds + "s.",
                0, timeoutSeconds * 1000L, null, null);
    }

    public static @NotNull AgentResult cancelled(@NotNull String agent, @Nullable String intent) {
        return new AgentResult(agent, intent, Status.CANCELLED,
                "Agent '" + agent + "' was cancelled.", 0, 0, null, null);
    }

    /** Label like {@code reviewer (Ollama · qwen3)} used in progress events and reports. */
    public @NotNull String label() {
        if (provider == null || provider.isBlank()) {
            return agent;
        }
        String modelPart = (model != null && !model.isBlank()) ? " · " + model : "";
        return agent + " (" + provider + modelPart + ")";
    }
}
