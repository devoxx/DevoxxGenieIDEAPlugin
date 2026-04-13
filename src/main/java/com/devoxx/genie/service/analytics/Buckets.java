package com.devoxx.genie.service.analytics;

import org.jetbrains.annotations.NotNull;

/**
 * Coarse-count bucketing for analytics (task-209, AC #7, #17).
 *
 * <p>Raw counts are never emitted — they could fingerprint an install. All counts go through
 * this utility so the wire schema stays consistent with the shared GA4 schema both DevoxxGenie
 * and the GenieBuilder admin panel agree on.
 *
 * <p>Two bucket ladders exist:
 * <ul>
 *   <li>"standard" — {@code 0}, {@code 1}, {@code 2-5}, {@code 6-10}, {@code 11+} — used for
 *       {@code mcp_server_count}, {@code custom_prompt_count}, {@code tool_call_count}.</li>
 *   <li>"chatMemory" — {@code 0}, {@code 1-5}, {@code 6-10}, {@code 11-20}, {@code 21+} — used
 *       for {@code chat_memory_bucket}.</li>
 * </ul>
 */
public final class Buckets {

    private Buckets() {
        // utility
    }

    /**
     * Standard count bucket: {@code 0}, {@code 1}, {@code 2-5}, {@code 6-10}, {@code 11+}.
     * Used for MCP server count, custom prompt count, and tool call count.
     */
    @NotNull
    public static String standard(int count) {
        if (count <= 0) return "0";
        if (count == 1) return "1";
        if (count <= 5) return "2-5";
        if (count <= 10) return "6-10";
        return "11+";
    }

    /**
     * Chat-memory bucket: {@code 0}, {@code 1-5}, {@code 6-10}, {@code 11-20}, {@code 21+}.
     * Chat memory sizes cluster differently from tool counts, so they get their own ladder.
     */
    @NotNull
    public static String chatMemory(int size) {
        if (size <= 0) return "0";
        if (size <= 5) return "1-5";
        if (size <= 10) return "6-10";
        if (size <= 20) return "11-20";
        return "21+";
    }
}
