package com.devoxx.genie.service.trace;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the events of the turn currently running in each chat tab (keyed by the tab's memory
 * key) until {@code ChatService} saves the conversation and hands the turn to
 * {@link AgentTraceService} (task-279). Sub-agents append to their parent's turn.
 * <p>
 * Thread-safe: streaming callbacks and parallel sub-agents append from different threads.
 */
public final class AgentTraceBuffer {

    /** Turns never saved (tab closed mid-turn, provider error) are dropped after this long. */
    static final Duration STALE_AFTER = Duration.ofHours(1);

    private static final AgentTraceBuffer INSTANCE = new AgentTraceBuffer(Clock.systemUTC());

    private final Clock clock;
    private final Map<String, Turn> turns = new ConcurrentHashMap<>();
    private final Map<UUID, String> invocationToKey = new ConcurrentHashMap<>();

    AgentTraceBuffer(@NotNull Clock clock) {
        this.clock = clock;
    }

    public static @NotNull AgentTraceBuffer getInstance() {
        return INSTANCE;
    }

    /** A buffered turn: its id, the settings it was recorded with, and its events so far. */
    public static final class Turn {
        private final String turnId = UUID.randomUUID().toString();
        private final TraceSettings settings;
        private final long startedAtMs;
        private final List<TraceEvent> events = new ArrayList<>();

        Turn(@NotNull TraceSettings settings, long startedAtMs) {
            this.settings = settings;
            this.startedAtMs = startedAtMs;
        }

        public @NotNull String turnId() {
            return turnId;
        }

        public @NotNull TraceSettings settings() {
            return settings;
        }

        public synchronized @NotNull List<TraceEvent> events() {
            return List.copyOf(events);
        }

        synchronized void add(@NotNull TraceEvent event) {
            events.add(event);
        }
    }

    /** Starts a new turn for {@code memoryKey}, replacing any unsaved previous one. */
    public void startTurn(@NotNull String memoryKey, @NotNull UUID invocationId, @NotNull TraceSettings settings) {
        purgeStale();
        turns.put(memoryKey, new Turn(settings, clock.millis()));
        invocationToKey.put(invocationId, memoryKey);
    }

    /** Appends to the running turn of {@code memoryKey}; ignored when no turn is running. */
    public void append(@NotNull String memoryKey, @NotNull TraceEvent event) {
        Turn turn = turns.get(memoryKey);
        if (turn != null) {
            turn.add(event);
        }
    }

    /** The main agent's AiServices invocation has finished; sub-agents can no longer attach to it. */
    public void endInvocation(@NotNull UUID invocationId) {
        invocationToKey.remove(invocationId);
    }

    /** Memory key of the turn a running AiServices invocation belongs to, for sub-agents. */
    public @Nullable String keyForInvocation(@Nullable UUID invocationId) {
        return invocationId == null ? null : invocationToKey.get(invocationId);
    }

    /** Whether a turn is buffered for {@code memoryKey} (cheap check before loading the writer service). */
    public boolean hasTurn(@NotNull String memoryKey) {
        return turns.containsKey(memoryKey);
    }

    /** Removes and returns the turn of {@code memoryKey}, or {@code null} when none was recorded. */
    public @Nullable Turn take(@NotNull String memoryKey) {
        return turns.remove(memoryKey);
    }

    /** Drops the running turn of a tab (e.g. on tab close) without saving it. */
    public void discard(@NotNull String memoryKey) {
        turns.remove(memoryKey);
        invocationToKey.values().removeIf(memoryKey::equals);
    }

    private void purgeStale() {
        long cutoff = clock.millis() - STALE_AFTER.toMillis();
        turns.entrySet().removeIf(e -> e.getValue().startedAtMs < cutoff);
        invocationToKey.values().removeIf(key -> !turns.containsKey(key));
    }
}
