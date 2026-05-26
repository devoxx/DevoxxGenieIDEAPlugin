package com.devoxx.genie.service.rag;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Combines retrieval results from multiple paraphrased variants of a single user query into
 * one ranked list via Reciprocal Rank Fusion (RRF, k=60). Pure logic, no I/O, no langchain4j
 * coupling — so it can be unit-tested deterministically.
 *
 * <p>The fix this targets: meta-style user queries ("where do we discuss X?", "list me all
 * content about Y") don't embed near content-style chunks, so single-query retrieval surfaces
 * topically-adjacent boilerplate. Running N paraphrases in parallel and RRF-fusing pushes
 * results that score well across several variants above noise that only scored well on the
 * original phrasing.
 *
 * <p>RRF formula: each item's score is the sum over input lists of {@code 1 / (k + rank)},
 * where rank is 1-based and k=60 (the value used in the standard RRF paper and by
 * {@code dev.langchain4j.rag.content.aggregator.ReciprocalRankFuser}).
 *
 * @see <a href="https://learn.microsoft.com/en-us/azure/search/hybrid-search-ranking">RRF
 *      ranking</a>
 */
public final class QueryExpansionFuser {

    /** RRF k-constant; 60 is the standard value used across the literature. */
    static final int RRF_K = 60;

    private QueryExpansionFuser() {} // utility

    /**
     * Issue one retrieval per query variant and fuse the results.
     *
     * @param variants    the queries to issue (must be non-empty; the user's original query
     *                    must be one of them so the unexpanded baseline is preserved)
     * @param retrieve    function that performs the actual per-variant retrieval. Called once
     *                    per variant; ordering of returned results is treated as rank order.
     * @param maxResults  upper bound on the fused list size; the {@code maxResults} highest
     *                    RRF-scored unique results are returned
     * @return RRF-ranked, deduplicated list of {@link SearchResult}s, descending by fused score
     */
    public static @NotNull List<SearchResult> expandAndFuse(
            @NotNull Collection<String> variants,
            @NotNull Function<String, List<SearchResult>> retrieve,
            int maxResults) {
        if (variants.isEmpty() || maxResults <= 0) return List.of();

        // Two parallel maps keyed by (filePath, content) so chunks that match across variants
        // accumulate score. Using LinkedHashMap preserves insertion order which gives us a
        // stable tiebreaker when scores tie.
        Map<DedupKey, Double> scores = new LinkedHashMap<>();
        Map<DedupKey, SearchResult> chosen = new LinkedHashMap<>();

        for (String variant : variants) {
            List<SearchResult> hits = retrieve.apply(variant);
            if (hits == null) continue;
            for (int i = 0; i < hits.size(); i++) {
                SearchResult hit = hits.get(i);
                DedupKey key = DedupKey.of(hit);
                int rank = i + 1;
                scores.merge(key, 1.0 / (RRF_K + rank), Double::sum);
                chosen.putIfAbsent(key, hit);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<DedupKey, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(e -> chosen.get(e.getKey()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    /**
     * Deduplication key for {@link SearchResult}. {@code filePath} alone is too coarse — a
     * file's distinct chunks all match — and the original {@code score} is variant-specific
     * so it can't be part of the key. Pair {@code filePath} with the chunk text.
     */
    private record DedupKey(String filePath, String content) {
        static DedupKey of(@NotNull SearchResult r) {
            return new DedupKey(r.filePath(), r.content());
        }
    }
}
