package com.devoxx.genie.service.spec.search;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Pure-Java BM25 (Okapi BM25) implementation for ranking task specs by relevance.
 * Operates entirely in-memory over tokenized document text — no external dependencies.
 *
 * <p>BM25 parameters:
 * <ul>
 *   <li>{@code k1} — term frequency saturation (default 1.2)</li>
 *   <li>{@code b} — length normalization (default 0.75)</li>
 * </ul>
 */
public class BM25SearchEngine {

    private static final double K1 = 1.2;
    private static final double B = 0.75;
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s\\p{Punct}]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "shall", "can", "need", "must",
            "in", "on", "at", "to", "for", "of", "with", "by", "from", "as",
            "into", "about", "between", "through", "during", "before", "after",
            "and", "but", "or", "nor", "not", "so", "yet",
            "it", "its", "this", "that", "these", "those",
            "i", "we", "you", "he", "she", "they", "me", "us", "him", "her", "them"
    );

    private final Map<String, List<String>> documentTokens = new LinkedHashMap<>();
    private final Map<String, Integer> docFrequency = new HashMap<>();
    private double avgDocLength;

    /**
     * Indexes a document. Call this for each document before searching.
     *
     * @param docId unique identifier for the document
     * @param text  text content to index
     */
    public void index(@NotNull String docId, @NotNull String text) {
        List<String> tokens = tokenize(text);
        documentTokens.put(docId, tokens);

        // Track unique terms per document for document frequency
        Set<String> uniqueTerms = new HashSet<>(tokens);
        for (String term : uniqueTerms) {
            docFrequency.merge(term, 1, Integer::sum);
        }

        // Recalculate average document length
        avgDocLength = documentTokens.values().stream()
                .mapToInt(List::size)
                .average()
                .orElse(0.0);
    }

    /**
     * Searches for documents matching the query, ranked by BM25 score.
     *
     * @param query the search query
     * @param limit maximum number of results
     * @return list of scored results, highest score first
     */
    public @NotNull List<ScoredResult> search(@NotNull String query, int limit) {
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty() || documentTokens.isEmpty()) {
            return Collections.emptyList();
        }

        int totalDocs = documentTokens.size();

        return documentTokens.entrySet().stream()
                .map(entry -> {
                    double score = computeBM25Score(entry.getKey(), entry.getValue(), queryTokens, totalDocs);
                    return new ScoredResult(entry.getKey(), score);
                })
                .filter(r -> r.score() > 0.0)
                .sorted(Comparator.comparingDouble(ScoredResult::score).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Clears all indexed documents.
     */
    public void clear() {
        documentTokens.clear();
        docFrequency.clear();
        avgDocLength = 0.0;
    }

    private double computeBM25Score(@NotNull String docId,
                                     @NotNull List<String> docTokens,
                                     @NotNull List<String> queryTokens,
                                     int totalDocs) {
        double score = 0.0;
        int docLen = docTokens.size();

        // Build term frequency map for this document
        Map<String, Integer> termFreqs = new HashMap<>();
        for (String token : docTokens) {
            termFreqs.merge(token, 1, Integer::sum);
        }

        for (String queryTerm : queryTokens) {
            int tf = termFreqs.getOrDefault(queryTerm, 0);
            if (tf == 0) continue;

            int df = docFrequency.getOrDefault(queryTerm, 0);
            // IDF: log((N - df + 0.5) / (df + 0.5) + 1)
            double idf = Math.log((totalDocs - df + 0.5) / (df + 0.5) + 1.0);

            // TF component with length normalization
            double tfNorm = (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * (docLen / avgDocLength)));

            score += idf * tfNorm;
        }

        return score;
    }

    /**
     * Tokenizes text into lowercase terms, filtering stop words and short tokens.
     */
    static @NotNull List<String> tokenize(@NotNull String text) {
        if (text.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(TOKEN_SPLIT.split(text.toLowerCase()))
                .filter(t -> t.length() > 1)
                .filter(t -> !STOP_WORDS.contains(t))
                .collect(Collectors.toList());
    }

    /**
     * A document search result with its BM25 relevance score.
     */
    public record ScoredResult(@NotNull String docId, double score) {
    }
}
