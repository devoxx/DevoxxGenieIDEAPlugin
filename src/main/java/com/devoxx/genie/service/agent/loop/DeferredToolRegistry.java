package com.devoxx.genie.service.agent.loop;

import dev.langchain4j.agent.tool.ToolSpecification;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Per-run registry of tools whose definitions are withheld from LLM requests until the model
 * asks for them via {@code search_tools} (or calls one directly by name).
 *
 * <p>With many MCP servers enabled, dozens of tool definitions — often several KB each — are
 * sent on every round trip although a task typically needs a handful. Deferred tools stay
 * fully executable (their executors are registered with langchain4j as usual); only their
 * specifications are filtered out of outgoing requests by {@link AgentRequestTransformer}.
 */
public class DeferredToolRegistry {

    public static final int DEFAULT_SEARCH_RESULTS = 5;
    public static final int MAX_SEARCH_RESULTS = 10;

    private final Map<String, ToolSpecification> deferred = new LinkedHashMap<>();
    private final Set<String> unlocked = new HashSet<>();

    /** Tool found by {@link #search}. */
    public record Match(@NotNull ToolSpecification spec, int score) {
    }

    /** Registers tools to defer (replacing any previous registration, keeping unlocks). */
    public synchronized void defer(@NotNull List<ToolSpecification> specs) {
        deferred.clear();
        for (ToolSpecification spec : specs) {
            deferred.put(spec.name(), spec);
        }
    }

    public synchronized boolean isActive() {
        return !deferred.isEmpty();
    }

    public synchronized boolean isDeferred(@NotNull String toolName) {
        return deferred.containsKey(toolName);
    }

    public synchronized boolean isUnlocked(@NotNull String toolName) {
        return unlocked.contains(toolName);
    }

    /** Unlocks a deferred tool; returns {@code true} when it was hidden until now. */
    public synchronized boolean unlock(@NotNull String toolName) {
        return deferred.containsKey(toolName) && unlocked.add(toolName);
    }

    public synchronized @NotNull List<String> hiddenToolNames() {
        return deferred.keySet().stream().filter(name -> !unlocked.contains(name)).toList();
    }

    /** Filters a request's tool specifications, dropping deferred tools that are still locked. */
    public synchronized @NotNull List<ToolSpecification> visible(@NotNull List<ToolSpecification> specs) {
        if (deferred.isEmpty()) {
            return specs;
        }
        List<ToolSpecification> out = new ArrayList<>(specs.size());
        for (ToolSpecification spec : specs) {
            if (!deferred.containsKey(spec.name()) || unlocked.contains(spec.name())) {
                out.add(spec);
            }
        }
        return out;
    }

    /**
     * Ranks deferred tools against a keyword query. Exact tool names (comma or space
     * separated, optionally prefixed with {@code select:}) always rank first; otherwise tools
     * score by query-term overlap with their name (weighted) and description.
     */
    public synchronized @NotNull List<Match> search(@NotNull String query, int maxResults) {
        int limit = Math.max(1, Math.min(maxResults, MAX_SEARCH_RESULTS));
        String q = query.trim();
        if (q.toLowerCase(Locale.ROOT).startsWith("select:")) {
            q = q.substring("select:".length());
        }
        Set<String> exactNames = new HashSet<>();
        for (String part : q.split("[,\\s]+")) {
            if (deferred.containsKey(part)) exactNames.add(part);
        }
        Set<String> terms = tokens(q);

        List<Match> matches = new ArrayList<>();
        for (ToolSpecification spec : deferred.values()) {
            int score;
            if (exactNames.contains(spec.name())) {
                score = 1_000;
            } else {
                Set<String> nameTokens = tokens(spec.name());
                Set<String> descTokens = tokens(spec.description() != null ? spec.description() : "");
                score = 0;
                for (String term : terms) {
                    if (nameTokens.contains(term)) score += 3;
                    else if (nameTokens.stream().anyMatch(t -> t.startsWith(term) || term.startsWith(t))) score += 2;
                    if (descTokens.contains(term)) score += 1;
                }
            }
            if (score > 0) {
                matches.add(new Match(spec, score));
            }
        }
        matches.sort(Comparator.comparingInt(Match::score).reversed()
                .thenComparing(m -> m.spec().name()));
        return matches.size() > limit ? matches.subList(0, limit) : matches;
    }

    /** Lower-cased word tokens, splitting on non-alphanumerics and camelCase boundaries. */
    static @NotNull Set<String> tokens(@NotNull String text) {
        String split = text.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        Set<String> out = new HashSet<>();
        for (String t : split.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (t.length() >= 2) out.add(t);
        }
        return out;
    }
}
