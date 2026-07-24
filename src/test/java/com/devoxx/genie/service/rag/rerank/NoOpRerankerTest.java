package com.devoxx.genie.service.rag.rerank;

import com.devoxx.genie.service.rag.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpRerankerTest {

    private final NoOpReranker reranker = new NoOpReranker();

    @Test
    void returnsCandidatesUnchangedAndTruncated() {
        List<SearchResult> input = List.of(
                new SearchResult("a", 0.9, "alpha"),
                new SearchResult("b", 0.8, "beta"),
                new SearchResult("c", 0.7, "gamma"),
                new SearchResult("d", 0.6, "delta"));

        List<SearchResult> out = reranker.rerank("q", input, 2, 1000);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).filePath()).isEqualTo("a");
        assertThat(out.get(1).filePath()).isEqualTo("b");
        // No reranker annotations are added by the no-op.
        assertThat(out.get(0).preRerankRank()).isNull();
        assertThat(out.get(0).rerankerScore()).isNull();
    }

    @Test
    void returnsEmptyListForEmptyInput() {
        assertThat(reranker.rerank("q", List.of(), 10, 1000)).isEmpty();
    }

    @Test
    void returnsEmptyListForZeroTopN() {
        assertThat(reranker.rerank("q",
                List.of(new SearchResult("a", 1.0, "x")), 0, 1000)).isEmpty();
    }
}
