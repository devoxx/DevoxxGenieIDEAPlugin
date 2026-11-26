package com.devoxx.genie.service.trace.view;

import com.devoxx.genie.service.trace.StoredTraceEvent;
import com.devoxx.genie.service.trace.TraceEvent;
import com.devoxx.genie.service.trace.TraceScanStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds an {@link AgentRun} from a conversation's stored trace rows (TASK-283). Pure: no IDE or
 * database access, so it is cheap to test and can run on any thread.
 * <p>
 * Sub-agent rows carry only a label ({@code subAgentId}), not the call that started them, and a
 * label such as {@code sub-agent-1:…} is reused by every {@code parallel_explore} call. So each
 * sub-agent row is placed by time: under the latest {@code parallel_explore} call that was asked
 * for before the row and whose result came after it. Rows that fit no call are kept as the turn's
 * unattached sub-agents rather than dropped.
 */
public final class AgentRunModelBuilder {

    private static final String PARALLEL_EXPLORE = "parallel_explore";
    private static final ObjectMapper CANONICAL_JSON = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private AgentRunModelBuilder() {
    }

    public static @NotNull AgentRun build(@NotNull List<StoredTraceEvent> events) {
        Totals totals = new Totals();
        Map<String, List<StoredTraceEvent>> byTurn = new LinkedHashMap<>();
        for (StoredTraceEvent e : events) {
            byTurn.computeIfAbsent(e.turnId(), k -> new ArrayList<>()).add(e);
            totals.observe(e);
        }

        Set<String> mainCallsSeen = new HashSet<>();
        List<AgentRun.Turn> turns = new ArrayList<>();
        int turnNumber = 0;
        for (Map.Entry<String, List<StoredTraceEvent>> entry : byTurn.entrySet()) {
            turns.add(buildTurn(++turnNumber, entry.getKey(), entry.getValue(), mainCallsSeen, totals));
        }
        return new AgentRun(totals.summary(turns.size()), turns);
    }

    private static AgentRun.Turn buildTurn(int number, String turnId, List<StoredTraceEvent> rows,
                                           Set<String> callsSeen, Totals totals) {
        List<TraceEvent> main = new ArrayList<>();
        List<TraceEvent> sub = new ArrayList<>();
        for (StoredTraceEvent row : rows) {
            (row.event().subAgentId() == null ? main : sub).add(row.event());
        }

        StepsBuilder steps = new StepsBuilder(callsSeen, totals);
        steps.addAll(main);

        // Place each sub-agent row under the parallel_explore call whose window contains it, then
        // group the placed rows per (call, sub-agent label), keeping their order.
        Map<CallBuilder, Map<String, List<TraceEvent>>> placed = new LinkedHashMap<>();
        Map<String, List<TraceEvent>> unattached = new LinkedHashMap<>();
        List<CallBuilder> explores = steps.calls.stream().filter(c -> PARALLEL_EXPLORE.equals(c.name)).toList();
        for (TraceEvent e : sub) {
            CallBuilder parent = parentFor(e.timestampMs(), explores);
            Map<String, List<TraceEvent>> target = parent == null
                    ? unattached
                    : placed.computeIfAbsent(parent, k -> new LinkedHashMap<>());
            target.computeIfAbsent(e.subAgentId(), k -> new ArrayList<>()).add(e);
        }
        for (Map.Entry<CallBuilder, Map<String, List<TraceEvent>>> entry : placed.entrySet()) {
            entry.getValue().forEach((label, subRows) -> entry.getKey().subAgents.add(subAgent(label, subRows, totals)));
        }
        List<AgentRun.SubAgentRun> orphans = new ArrayList<>();
        unattached.forEach((label, subRows) -> orphans.add(subAgent(label, subRows, totals)));

        long turnStart = rows.get(0).event().timestampMs();
        return new AgentRun.Turn(number, turnId, steps.prompt,
                steps.promptTimestamp != null ? steps.promptTimestamp : turnStart, steps.freeze(), orphans);
    }

    /** The latest parallel_explore call asked for at or before {@code t} whose result (if any) came at or after it. */
    private static @Nullable CallBuilder parentFor(long t, List<CallBuilder> explores) {
        CallBuilder best = null;
        for (CallBuilder call : explores) {
            boolean started = call.stepTimestamp <= t;
            boolean stillRunning = call.resultTimestamp == null || call.resultTimestamp >= t;
            if (started && stillRunning && (best == null || call.stepTimestamp >= best.stepTimestamp)) {
                best = call;
            }
        }
        return best;
    }

    private static AgentRun.SubAgentRun subAgent(String label, List<TraceEvent> rows, Totals totals) {
        StepsBuilder steps = new StepsBuilder(new HashSet<>(), totals);
        steps.addAll(rows);
        return new AgentRun.SubAgentRun(label, steps.prompt, steps.freeze());
    }

    /** Key for repeat detection: the tool name plus its arguments, as canonical JSON when they parse. */
    static @NotNull String callKey(@NotNull String name, @Nullable String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return name + "\u0000";
        }
        try {
            JsonNode node = CANONICAL_JSON.readTree(arguments);
            Object canonical = CANONICAL_JSON.treeToValue(node, Object.class);
            return name + "\u0000" + CANONICAL_JSON.writeValueAsString(canonical);
        } catch (Exception e) {
            return name + "\u0000" + arguments.strip();
        }
    }

    /** Turns one thread's events (the main agent, or one sub-agent) into steps with matched tool calls. */
    private static final class StepsBuilder {
        private final Set<String> callsSeen;
        private final Totals totals;
        private final List<StepBuilder> steps = new ArrayList<>();
        private final List<CallBuilder> calls = new ArrayList<>();
        private final Map<String, CallBuilder> callsById = new HashMap<>();
        private String prompt;
        private Long promptTimestamp;
        private Long previousTimestamp;

        StepsBuilder(Set<String> callsSeen, Totals totals) {
            this.callsSeen = callsSeen;
            this.totals = totals;
        }

        void addAll(List<TraceEvent> events) {
            for (TraceEvent e : events) {
                switch (e.role()) {
                    case USER -> {
                        if (prompt == null) {
                            prompt = e.content();
                            promptTimestamp = e.timestampMs();
                        }
                        previousTimestamp = e.timestampMs();
                    }
                    case ASSISTANT -> addStep(e);
                    case TOOL -> addResult(e);
                }
            }
        }

        private void addStep(TraceEvent e) {
            long since = previousTimestamp == null ? 0 : Math.max(0, e.timestampMs() - previousTimestamp);
            StepBuilder step = new StepBuilder(steps.size() + 1, e, since);
            for (TraceEvent.ToolCall tc : e.toolCalls()) {
                CallBuilder call = new CallBuilder(tc, e.timestampMs(), !callsSeen.add(callKey(tc.name(), tc.arguments())));
                step.calls.add(call);
                calls.add(call);
                if (tc.id() != null) {
                    callsById.put(tc.id(), call);
                }
            }
            steps.add(step);
            previousTimestamp = e.timestampMs();
            totals.steps++;
        }

        private void addResult(TraceEvent e) {
            CallBuilder call = e.toolCallId() == null ? null : callsById.get(e.toolCallId());
            if (call == null || call.resultTimestamp != null) {
                // No (or a reused) id: fall back to the first call of that tool still waiting for a result.
                call = calls.stream()
                        .filter(c -> c.resultTimestamp == null && c.name.equals(e.toolName()))
                        .findFirst().orElse(null);
            }
            if (call == null) {
                return; // A result for a call that wasn't recorded; nothing to attach it to.
            }
            call.result = e.content() == null ? "" : e.content();
            call.error = e.error();
            call.resultTimestamp = e.timestampMs();
        }

        List<AgentRun.Step> freeze() {
            List<AgentRun.Step> frozen = new ArrayList<>(steps.size());
            for (StepBuilder step : steps) {
                List<AgentRun.ToolCallNode> nodes = new ArrayList<>(step.calls.size());
                for (CallBuilder call : step.calls) {
                    nodes.add(call.freeze(totals));
                }
                frozen.add(new AgentRun.Step(step.number, step.event.content(), step.event.reasoning(),
                        step.event.model(), step.event.timestampMs(), step.sincePrevious, nodes));
            }
            return frozen;
        }
    }

    private static final class StepBuilder {
        final int number;
        final TraceEvent event;
        final long sincePrevious;
        final List<CallBuilder> calls = new ArrayList<>();

        StepBuilder(int number, TraceEvent event, long sincePrevious) {
            this.number = number;
            this.event = event;
            this.sincePrevious = sincePrevious;
        }
    }

    private static final class CallBuilder {
        final String id;
        final String name;
        final String arguments;
        final long stepTimestamp;
        final boolean repeat;
        final List<AgentRun.SubAgentRun> subAgents = new ArrayList<>();
        String result;
        boolean error;
        Long resultTimestamp;

        CallBuilder(TraceEvent.ToolCall call, long stepTimestamp, boolean repeat) {
            this.id = call.id();
            this.name = call.name();
            this.arguments = call.arguments();
            this.stepTimestamp = stepTimestamp;
            this.repeat = repeat;
        }

        AgentRun.ToolCallNode freeze(Totals totals) {
            boolean limit = result != null && result.startsWith(AgentRun.TOOL_CALL_LIMIT_MARKER);
            long duration = resultTimestamp == null ? -1 : Math.max(0, resultTimestamp - stepTimestamp);
            totals.toolCalls++;
            if (error) totals.failed++;
            if (repeat) totals.repeats++;
            if (limit) totals.hitLimit = true;
            return new AgentRun.ToolCallNode(id, name, arguments, result, error, duration, repeat, limit, subAgents);
        }
    }

    /** Run-wide totals, collected while building. */
    private static final class Totals {
        private final Set<String> models = new LinkedHashSet<>();
        private final Set<TraceScanStatus> scanStatuses = EnumSet.noneOf(TraceScanStatus.class);
        private long first = Long.MAX_VALUE;
        private long last = Long.MIN_VALUE;
        int steps;
        int toolCalls;
        int failed;
        int repeats;
        boolean hitLimit;

        void observe(StoredTraceEvent row) {
            TraceEvent e = row.event();
            if (e.model() != null && !e.model().isBlank()) {
                models.add(e.model());
            }
            scanStatuses.add(row.scanStatus());
            first = Math.min(first, e.timestampMs());
            last = Math.max(last, e.timestampMs());
        }

        AgentRun.Summary summary(int turns) {
            long duration = first == Long.MAX_VALUE ? 0 : last - first;
            return new AgentRun.Summary(List.copyOf(models), turns, steps, toolCalls, failed, repeats, duration,
                    scanStatuses, hitLimit);
        }
    }
}
