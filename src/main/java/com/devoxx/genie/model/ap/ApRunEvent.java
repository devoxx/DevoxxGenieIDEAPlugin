package com.devoxx.genie.model.ap;

/**
 * Streamed JSON event emitted on stdout by {@code ap run --json}.
 * Only the events the plugin actively renders are modelled as concrete subtypes;
 * everything else is wrapped in {@link Other} so callers can still log or count it.
 */
public sealed interface ApRunEvent {

    /** A chunk of text produced by an agent (the visible response). */
    record AgentOutput(String agent, String content, boolean reasoning) implements ApRunEvent {}

    /** Signals that the streamed turn has started. */
    record StreamStarted(String agent) implements ApRunEvent {}

    /** Signals that the streamed turn has ended (terminal event for the response). */
    record StreamStopped() implements ApRunEvent {}

    /**
     * Any other event type the CLI emits — opaque status/queue/token-usage envelopes.
     * Kept so listeners can log them or surface them in an activity panel later.
     */
    record Other(String type, String rawJson) implements ApRunEvent {}
}
