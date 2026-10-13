package com.devoxx.genie.service.rag.rerank;

import com.devoxx.genie.service.rag.SearchResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Cross-encoder style reranker stage that runs after the vector store (and any RRF fusion)
 * and reorders a retrieval shortlist by per-candidate relevance before the results reach
 * {@link com.devoxx.genie.service.MessageCreationService}.
 *
 * <p>The reranker MUST be best-effort: implementations are expected to honor the supplied
 * {@code timeoutMs} budget and, on timeout or transient failure, return the original
 * {@code candidates} list unchanged so the prompt still receives a valid context. The
 * caller is responsible for logging the fallback.
 */
public interface Reranker {

    /**
     * Reorder {@code candidates} by reranker-assigned relevance and return the top {@code topN}.
     *
     * @param query       the user query, used to score each candidate
     * @param candidates  retrieval shortlist, ordered by upstream score (best first); never null
     * @param topN        upper bound on the returned list size (the {@code IndexerMaxResults}
     *                    setting in production)
     * @param timeoutMs   wall-clock budget for the entire reranker call; implementations should
     *                    fall back to the original order rather than block longer than this
     * @return reranked, top-{@code topN} sublist. On any failure or timeout the original order
     *         (truncated to {@code topN}) is returned so the caller can always make progress.
     */
    @NotNull List<SearchResult> rerank(@NotNull String query,
                                       @NotNull List<SearchResult> candidates,
                                       int topN,
                                       long timeoutMs);
}
