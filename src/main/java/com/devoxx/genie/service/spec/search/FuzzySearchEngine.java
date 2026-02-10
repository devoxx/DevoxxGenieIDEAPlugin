package com.devoxx.genie.service.spec.search;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Fuzzy search engine using Levenshtein edit distance to find documents containing
 * terms similar (but not identical) to query terms. This complements BM25 by catching
 * cases where terminology differs slightly — e.g. "auth" matches "authentication",
 * "authetication" matches "authentication".
 *
 * <p>For each query token, this engine finds the best-matching token in each document
 * (by normalized edit-distance similarity) and aggregates a score across all query terms.
 * A configurable similarity threshold filters out weak matches.
 */
public class FuzzySearchEngine {

    /**
     * Minimum similarity (0.0–1.0) for a fuzzy match to count.
     * 0.6 means the strings must share at least 60% of their characters.
     */
    private static final double MIN_SIMILARITY = 0.6;

    private final Map<String, List<String>> documentTokens = new LinkedHashMap<>();

    /**
     * Indexes a document for fuzzy searching.
     *
     * @param docId unique document identifier
     * @param text  text content to index
     */
    public void index(@NotNull String docId, @NotNull String text) {
        documentTokens.put(docId, BM25SearchEngine.tokenize(text));
    }

    /**
     * Searches for documents with terms fuzzy-matching the query.
     *
     * @param query search query
     * @param limit maximum results
     * @return scored results, highest first
     */
    public @NotNull List<BM25SearchEngine.ScoredResult> search(@NotNull String query, int limit) {
        List<String> queryTokens = BM25SearchEngine.tokenize(query);
        if (queryTokens.isEmpty() || documentTokens.isEmpty()) {
            return Collections.emptyList();
        }

        return documentTokens.entrySet().stream()
                .map(entry -> {
                    double score = computeFuzzyScore(entry.getValue(), queryTokens);
                    return new BM25SearchEngine.ScoredResult(entry.getKey(), score);
                })
                .filter(r -> r.score() > 0.0)
                .sorted(Comparator.comparingDouble(BM25SearchEngine.ScoredResult::score).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Clears all indexed documents.
     */
    public void clear() {
        documentTokens.clear();
    }

    private double computeFuzzyScore(@NotNull List<String> docTokens,
                                      @NotNull List<String> queryTokens) {
        if (docTokens.isEmpty()) return 0.0;

        // Build a set of unique doc tokens for faster iteration
        Set<String> uniqueDocTokens = new HashSet<>(docTokens);

        double totalScore = 0.0;
        for (String queryToken : queryTokens) {
            double bestSimilarity = 0.0;
            for (String docToken : uniqueDocTokens) {
                double sim = similarity(queryToken, docToken);
                if (sim > bestSimilarity) {
                    bestSimilarity = sim;
                }
            }
            if (bestSimilarity >= MIN_SIMILARITY) {
                totalScore += bestSimilarity;
            }
        }

        return totalScore;
    }

    /**
     * Computes normalized similarity between two strings (1.0 = identical, 0.0 = completely different).
     * Uses Levenshtein edit distance.
     */
    static double similarity(@NotNull String a, @NotNull String b) {
        if (a.equals(b)) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        int distance = levenshteinDistance(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Computes the Levenshtein edit distance between two strings.
     * Uses the standard dynamic programming approach with O(min(m,n)) space.
     */
    static int levenshteinDistance(@NotNull String a, @NotNull String b) {
        // Optimize: ensure a is the shorter string for space efficiency
        if (a.length() > b.length()) {
            String tmp = a;
            a = b;
            b = tmp;
        }

        int[] prev = new int[a.length() + 1];
        int[] curr = new int[a.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            prev[i] = i;
        }

        for (int j = 1; j <= b.length(); j++) {
            curr[0] = j;
            for (int i = 1; i <= a.length(); i++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[i] = Math.min(
                        Math.min(curr[i - 1] + 1, prev[i] + 1),
                        prev[i - 1] + cost
                );
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }

        return prev[a.length()];
    }
}
