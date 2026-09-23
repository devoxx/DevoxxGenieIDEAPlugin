package com.devoxx.genie.service.agent.loop;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Per-run agent loop metrics: tool timings, cache hits, compaction savings, deferred-tool
 * activity and LLM round trips / token usage.
 *
 * <p>Tool time is reported twice: <em>summed</em> (every call's duration added up) and
 * <em>wall</em> (the union of call intervals, so overlapping calls — e.g. parallel sub-agents —
 * are not double counted). No derived "orchestration time" is computed by subtracting
 * overlapping timers.
 *
 * <p>All methods are thread-safe; tool executors can run on several threads at once.
 */
public class AgentRunMetrics {

    private final long startedNanos = System.nanoTime();

    private final Map<String, ToolStats> tools = new TreeMap<>();
    private final List<long[]> intervals = new ArrayList<>();

    private int roundTrips;
    private long requestChars;
    private long compactedChars;
    private int compactedResults;
    private int cacheHits;
    private int repeatBlocks;
    private int hiddenToolSpecs;
    private int toolsUnlocked;
    private int validationRejects;
    private int retries;
    private long inputTokens;
    private long outputTokens;

    /** Stats for one tool name. */
    public static final class ToolStats {
        private int calls;
        private int errors;
        private int cached;
        private long summedNanos;

        public int calls() { return calls; }
        public int errors() { return errors; }
        public int cached() { return cached; }
        public long summedMillis() { return summedNanos / 1_000_000L; }
    }

    /**
     * Records one executed (or cache-served) tool call.
     *
     * @param startNanos {@link System#nanoTime()} when the call started
     * @param endNanos   {@link System#nanoTime()} when it finished
     */
    public synchronized void recordToolCall(@NotNull String toolName, long startNanos, long endNanos,
                                            boolean error, boolean cached) {
        ToolStats stats = tools.computeIfAbsent(toolName, k -> new ToolStats());
        stats.calls++;
        if (error) stats.errors++;
        if (cached) {
            stats.cached++;
            cacheHits++;
            return;
        }
        stats.summedNanos += Math.max(0, endNanos - startNanos);
        intervals.add(new long[]{startNanos, endNanos});
    }

    public synchronized void recordRepeatBlocked() {
        repeatBlocks++;
    }

    /** Records one outgoing LLM request after all request transformers have run. */
    public synchronized void recordRequest(long chars) {
        roundTrips++;
        requestChars += chars;
    }

    /** Records characters of earlier tool results that were not re-sent in one request. */
    public synchronized void recordCompaction(int results, long charsSaved) {
        compactedResults += results;
        compactedChars += charsSaved;
    }

    /** Records how many tool specifications were withheld from one request. */
    public synchronized void recordHiddenToolSpecs(int hidden) {
        hiddenToolSpecs += hidden;
    }

    public synchronized void recordToolsUnlocked(int count) {
        toolsUnlocked += count;
    }

    public synchronized void recordValidationReject() {
        validationRejects++;
    }

    public synchronized void recordRetry() {
        retries++;
    }

    public synchronized void recordTokens(long input, long output) {
        inputTokens += Math.max(0, input);
        outputTokens += Math.max(0, output);
    }

    public synchronized int getRoundTrips() { return roundTrips; }
    public synchronized long getRequestChars() { return requestChars; }
    public synchronized long getCompactedChars() { return compactedChars; }
    public synchronized int getCompactedResults() { return compactedResults; }
    public synchronized int getCacheHits() { return cacheHits; }
    public synchronized int getRepeatBlocks() { return repeatBlocks; }
    public synchronized int getHiddenToolSpecs() { return hiddenToolSpecs; }
    public synchronized int getToolsUnlocked() { return toolsUnlocked; }
    public synchronized int getValidationRejects() { return validationRejects; }
    public synchronized int getRetries() { return retries; }
    public synchronized long getInputTokens() { return inputTokens; }
    public synchronized long getOutputTokens() { return outputTokens; }

    /** Number of tool calls that actually executed (cache hits excluded). */
    public synchronized int getExecutedToolCalls() {
        return intervals.size();
    }

    public synchronized long getSummedToolMillis() {
        return intervals.stream().mapToLong(i -> Math.max(0, i[1] - i[0])).sum() / 1_000_000L;
    }

    /** Union of all tool-call intervals, so concurrent calls are counted once. */
    public synchronized long getWallToolMillis() {
        if (intervals.isEmpty()) return 0;
        List<long[]> sorted = new ArrayList<>(intervals);
        sorted.sort(Comparator.comparingLong(i -> i[0]));
        long total = 0;
        long start = sorted.get(0)[0];
        long end = sorted.get(0)[1];
        for (int i = 1; i < sorted.size(); i++) {
            long[] next = sorted.get(i);
            if (next[0] <= end) {
                end = Math.max(end, next[1]);
            } else {
                total += end - start;
                start = next[0];
                end = next[1];
            }
        }
        total += end - start;
        return total / 1_000_000L;
    }

    public synchronized @NotNull Map<String, ToolStats> getToolStats() {
        return new TreeMap<>(tools);
    }

    /** Human-readable multi-line summary, suitable for the activity log and idea.log. */
    public synchronized @NotNull String summary() {
        long totalMillis = (System.nanoTime() - startedNanos) / 1_000_000L;
        StringBuilder sb = new StringBuilder("Agent run summary\n");
        sb.append("  LLM round trips: ").append(roundTrips)
                .append(", request chars sent: ").append(requestChars);
        if (inputTokens > 0 || outputTokens > 0) {
            sb.append(", tokens in/out: ").append(inputTokens).append('/').append(outputTokens);
        }
        sb.append('\n');
        sb.append("  Tool calls executed: ").append(getExecutedToolCalls())
                .append(", served from cache: ").append(cacheHits)
                .append(", repeat calls blocked: ").append(repeatBlocks).append('\n');
        sb.append("  Tool time: wall ").append(getWallToolMillis()).append(" ms, summed ")
                .append(getSummedToolMillis()).append(" ms; run total ").append(totalMillis).append(" ms\n");
        sb.append("  Compaction: ").append(compactedResults).append(" older tool results trimmed, ")
                .append(compactedChars).append(" chars not re-sent\n");
        sb.append("  Deferred tools: ").append(hiddenToolSpecs).append(" tool definitions withheld, ")
                .append(toolsUnlocked).append(" unlocked via search_tools\n");
        sb.append("  MCP: ").append(validationRejects).append(" calls rejected by local argument validation, ")
                .append(retries).append(" transient-failure retries\n");
        for (Map.Entry<String, ToolStats> e : tools.entrySet()) {
            ToolStats s = e.getValue();
            sb.append("    ").append(e.getKey()).append(": calls=").append(s.calls)
                    .append(", errors=").append(s.errors)
                    .append(", cached=").append(s.cached)
                    .append(", summedMs=").append(s.summedMillis()).append('\n');
        }
        return sb.toString();
    }
}
