package com.devoxx.genie.service.rag.rerank;

import com.devoxx.genie.service.rag.SearchResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * No-op reranker — returns the supplied candidates unchanged, truncated to {@code topN}.
 * Used when the "Rerank results" toggle is OFF, when wiring tests, and as the fallback
 * inside {@link OllamaReranker} when the model call cannot complete in time.
 */
public final class NoOpReranker implements Reranker {

    @Override
    public @NotNull List<SearchResult> rerank(@NotNull String query,
                                              @NotNull List<SearchResult> candidates,
                                              int topN,
                                              long timeoutMs) {
        if (candidates.isEmpty() || topN <= 0) {
            return List.of();
        }
        int n = Math.min(topN, candidates.size());
        return new ArrayList<>(candidates.subList(0, n));
    }
}
