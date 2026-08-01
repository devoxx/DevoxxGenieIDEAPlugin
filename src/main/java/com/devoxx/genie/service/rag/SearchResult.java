package com.devoxx.genie.service.rag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single semantic-search hit: which chunk matched the query, where it came from, and how well.
 *
 * @param filePath        absolute path of the source file the chunk was extracted from
 * @param score           similarity score (0.0–1.0) returned by the vector store
 * @param content         the chunk text as it was embedded and stored (NOT the full file contents)
 * @param preRerankRank   1-based rank of this hit in the retrieval shortlist before the reranker
 *                        ran ({@code null} when reranking was disabled or not applied)
 * @param rerankerScore   relevance score produced by the reranker (typically 0.0–1.0 after
 *                        normalization; {@code null} when reranking was disabled or not applied)
 */
public record SearchResult(@Nullable String filePath,
                            @Nullable Double score,
                            @Nullable String content,
                            @Nullable Integer preRerankRank,
                            @Nullable Double rerankerScore) {

    /**
     * Backward-compatible 3-arg constructor used by retrieval before any reranking has happened.
     * Sets {@code preRerankRank} and {@code rerankerScore} to {@code null}.
     */
    public SearchResult(@Nullable String filePath, @Nullable Double score, @Nullable String content) {
        this(filePath, score, content, null, null);
    }

    /**
     * Return a copy of this result carrying the supplied reranker annotations.
     * Used by reranker implementations to record where a hit sat before reranking
     * and what score the reranker assigned, without mutating the original instance.
     */
    public @NotNull SearchResult withRerankerAnnotations(int preRerankRank, double rerankerScore) {
        return new SearchResult(filePath, score, content, preRerankRank, rerankerScore);
    }
}
