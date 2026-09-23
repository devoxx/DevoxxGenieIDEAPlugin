package com.devoxx.genie.service.agent.loop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Per-run cache of read-only tool results, keyed by tool name plus canonicalised JSON
 * arguments (key order and whitespace do not matter).
 *
 * <p>An identical read-only call made while nothing has been modified returns the earlier
 * result instead of executing again. Any call to a tool outside the read-only set (write,
 * edit, run_command, MCP tools, …) may have changed the workspace, so it clears the cache —
 * except for "neutral" tools that are known not to modify anything but whose results should
 * not be cached either (e.g. {@code search_tools}, skill activation, {@code parallel_explore}).
 * Error results are never cached.
 *
 * <p>Models sometimes get stuck issuing the same call over and over. After
 * {@link #MAX_CACHED_REPEATS} cache-served repeats of one call, {@link #lookup} reports
 * {@link Lookup.Kind#REPEAT_BLOCKED} so the caller can tell the model to move on.
 */
public class ToolCallCache {

    /** Cache-served repeats allowed for one identical call before it is blocked. */
    public static final int MAX_CACHED_REPEATS = 2;

    private final Set<String> cacheableTools;
    private final Set<String> neutralTools;
    private final Map<String, Entry> entries = new HashMap<>();

    private static final class Entry {
        final String result;
        final int firstCallNumber;
        int hits;

        Entry(String result, int firstCallNumber) {
            this.result = result;
            this.firstCallNumber = firstCallNumber;
        }
    }

    /** Result of {@link #lookup}. */
    public record Lookup(@NotNull Kind kind, @Nullable String text) {
        public enum Kind { MISS, HIT, REPEAT_BLOCKED }

        static final Lookup MISS = new Lookup(Kind.MISS, null);
    }

    public ToolCallCache(@NotNull Set<String> cacheableTools, @NotNull Set<String> neutralTools) {
        this.cacheableTools = Set.copyOf(cacheableTools);
        this.neutralTools = Set.copyOf(neutralTools);
    }

    public boolean isCacheable(@NotNull String toolName) {
        return cacheableTools.contains(toolName);
    }

    /**
     * Looks up an earlier result for this exact call; a hit always repeats the full result.
     * For a tool outside the cacheable set this invalidates the whole cache (the call may
     * modify the workspace) and misses.
     */
    public synchronized @NotNull Lookup lookup(@NotNull String toolName, @Nullable String arguments) {
        return lookup(toolName, arguments, 0, -1);
    }

    /**
     * Like {@link #lookup(String, String)}, but when the earlier call is at most
     * {@code recentWindow} calls back — so its full result is still verbatim in the
     * conversation — a hit returns a short reference instead of repeating the content.
     *
     * @param currentCallNumber the run-wide number of the call being made
     * @param recentWindow      how many calls back a result counts as still in context;
     *                          negative to always repeat the full result
     */
    public synchronized @NotNull Lookup lookup(@NotNull String toolName, @Nullable String arguments,
                                               int currentCallNumber, int recentWindow) {
        if (!isCacheable(toolName)) {
            if (!neutralTools.contains(toolName)) {
                entries.clear();
            }
            return Lookup.MISS;
        }
        Entry entry = entries.get(key(toolName, arguments));
        if (entry == null) {
            return Lookup.MISS;
        }
        if (entry.hits >= MAX_CACHED_REPEATS) {
            return new Lookup(Lookup.Kind.REPEAT_BLOCKED,
                    "Error: You have already called " + toolName + " with these exact arguments "
                            + (entry.hits + 1) + " times in this run and nothing has been modified since, "
                            + "so the result is unchanged. Use the result you already have, change the "
                            + "arguments, or try a different approach.");
        }
        entry.hits++;
        if (recentWindow >= 0 && currentCallNumber - entry.firstCallNumber <= recentWindow) {
            return new Lookup(Lookup.Kind.HIT,
                    "[Identical to tool call #" + entry.firstCallNumber + " a few steps above (same " + toolName
                            + " arguments, nothing modified since) — its result is unchanged, use that output.]");
        }
        return new Lookup(Lookup.Kind.HIT,
                "[Same result as tool call #" + entry.firstCallNumber + " — identical " + toolName
                        + " call, nothing has been modified since.]\n" + entry.result);
    }

    /** Stores a successful result of a cacheable tool. */
    public synchronized void store(@NotNull String toolName, @Nullable String arguments,
                                   @Nullable String result, int callNumber) {
        if (!isCacheable(toolName) || result == null || isError(result)) {
            return;
        }
        entries.putIfAbsent(key(toolName, arguments), new Entry(result, callNumber));
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized int size() {
        return entries.size();
    }

    private static boolean isError(@NotNull String result) {
        return result.stripLeading().startsWith("Error:");
    }

    static @NotNull String key(@NotNull String toolName, @Nullable String arguments) {
        return toolName + '\u0000' + canonicalArguments(arguments);
    }

    /** Canonical JSON form: object keys sorted recursively; falls back to the trimmed raw text. */
    static @NotNull String canonicalArguments(@Nullable String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return "{}";
        }
        try {
            return canonical(JsonParser.parseString(arguments)).toString();
        } catch (Exception e) {
            return arguments.trim();
        }
    }

    private static @NotNull JsonElement canonical(@NotNull JsonElement element) {
        if (element.isJsonObject()) {
            TreeMap<String, JsonElement> sorted = new TreeMap<>();
            for (Map.Entry<String, JsonElement> e : element.getAsJsonObject().entrySet()) {
                sorted.put(e.getKey(), canonical(e.getValue()));
            }
            JsonObject out = new JsonObject();
            sorted.forEach(out::add);
            return out;
        }
        if (element.isJsonArray()) {
            JsonArray out = new JsonArray();
            for (JsonElement e : element.getAsJsonArray()) {
                out.add(canonical(e));
            }
            return out;
        }
        return element;
    }
}
