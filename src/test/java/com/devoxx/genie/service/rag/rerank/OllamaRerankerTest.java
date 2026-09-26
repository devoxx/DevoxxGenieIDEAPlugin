package com.devoxx.genie.service.rag.rerank;

import com.devoxx.genie.service.rag.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OllamaReranker} that bypass the network by overriding
 * {@link OllamaReranker#scoreCandidate} with a deterministic score map. The HTTP path
 * is exercised separately by {@link OllamaRerankerNormalizeScoreTest}.
 */
class OllamaRerankerTest {

    /** Returns scores from a content→score map; missing entries score 0. */
    private static class StubbedReranker extends OllamaReranker {
        final Map<String, Double> scores;
        StubbedReranker(Map<String, Double> scores) {
            this.scores = scores;
        }
        @Override
        protected double scoreCandidate(String query, SearchResult candidate) {
            return scores.getOrDefault(candidate.content(), 0.0);
        }
    }

    @Test
    void reordersCandidatesByRerankerScore() {
        List<SearchResult> input = List.of(
                new SearchResult("a", 0.95, "alpha"),  // retrieval rank 1, reranker thinks it's bad
                new SearchResult("b", 0.90, "beta"),   // retrieval rank 2, reranker thinks it's best
                new SearchResult("c", 0.85, "gamma")); // retrieval rank 3

        Map<String, Double> scores = new HashMap<>();
        scores.put("alpha", 0.2);
        scores.put("beta", 0.9);
        scores.put("gamma", 0.6);

        List<SearchResult> out = new StubbedReranker(scores).rerank("q", input, 3, 5000);

        assertThat(out).extracting(SearchResult::content)
                .as("reranker should reorder by score descending")
                .containsExactly("beta", "gamma", "alpha");
        // beta was retrieval rank 2, so preRerankRank should record that.
        assertThat(out.get(0).preRerankRank()).isEqualTo(2);
        assertThat(out.get(0).rerankerScore()).isEqualTo(0.9);
        assertThat(out.get(1).preRerankRank()).isEqualTo(3);
        assertThat(out.get(2).preRerankRank()).isEqualTo(1);
    }

    @Test
    void truncatesToTopN() {
        List<SearchResult> input = List.of(
                new SearchResult("a", 0.9, "alpha"),
                new SearchResult("b", 0.8, "beta"),
                new SearchResult("c", 0.7, "gamma"),
                new SearchResult("d", 0.6, "delta"));
        Map<String, Double> scores = Map.of("alpha", 0.1, "beta", 0.9, "gamma", 0.5, "delta", 0.7);

        List<SearchResult> out = new StubbedReranker(scores).rerank("q", input, 2, 5000);

        assertThat(out).hasSize(2);
        assertThat(out).extracting(SearchResult::content)
                .as("top-2 after reranking should be beta (0.9) and delta (0.7)")
                .containsExactly("beta", "delta");
    }

    @Test
    void preservesPreRerankRankOnSurvivors() {
        List<SearchResult> input = List.of(
                new SearchResult("a", 0.9, "alpha"),
                new SearchResult("b", 0.8, "beta"));

        Map<String, Double> scores = Map.of("alpha", 0.4, "beta", 0.6);

        List<SearchResult> out = new StubbedReranker(scores).rerank("q", input, 2, 5000);

        assertThat(out.get(0).preRerankRank()).as("beta was rank 2 in retrieval").isEqualTo(2);
        assertThat(out.get(0).rerankerScore()).isEqualTo(0.6);
        assertThat(out.get(1).preRerankRank()).as("alpha was rank 1 in retrieval").isEqualTo(1);
        assertThat(out.get(1).rerankerScore()).isEqualTo(0.4);
    }

    @Test
    void partialProgressOnTimeoutBlendsScoredAndUnscored() {
        // First two candidates score instantly; the rest sleep past the deadline. The reranker
        // must (a) keep the scored ones (sorted by reranker score), then (b) pad the remainder
        // with the unscored candidates in retrieval order — NOT discard everything. See M3.
        OllamaReranker mixed = new OllamaReranker() {
            @Override
            protected double scoreCandidate(String query, SearchResult candidate) {
                String c = candidate.content();
                if ("fast-a".equals(c)) return 0.9;
                if ("fast-b".equals(c)) return 0.4;
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return 0.99;
            }
        };

        List<SearchResult> input = List.of(
                new SearchResult("a", 0.95, "fast-a"),
                new SearchResult("b", 0.85, "fast-b"),
                new SearchResult("c", 0.75, "slow-c"),
                new SearchResult("d", 0.65, "slow-d"));

        List<SearchResult> out = mixed.rerank("q", input, 4, 150);

        // First two slots are the two scored hits, sorted by reranker score (fast-a > fast-b).
        assertThat(out).hasSize(4);
        assertThat(out.get(0).content()).isEqualTo("fast-a");
        assertThat(out.get(1).content()).isEqualTo("fast-b");
        // Remaining slots filled with the timed-out candidates in retrieval order.
        assertThat(out.get(2).content()).isEqualTo("slow-c");
        assertThat(out.get(3).content()).isEqualTo("slow-d");
        // Padded entries do NOT get fabricated reranker annotations.
        assertThat(out.get(2).rerankerScore()).isNull();
        assertThat(out.get(3).rerankerScore()).isNull();
    }

    @Test
    void timeoutFallsBackToOriginalOrder() {
        // Stub that sleeps longer than the configured timeout. The reranker must give up and
        // return the original (truncated) order rather than blocking.
        OllamaReranker slow = new OllamaReranker() {
            @Override
            protected double scoreCandidate(String query, SearchResult candidate) {
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return 0.9;
            }
        };

        List<SearchResult> input = List.of(
                new SearchResult("a", 0.95, "alpha"),
                new SearchResult("b", 0.85, "beta"),
                new SearchResult("c", 0.75, "gamma"));

        long start = System.currentTimeMillis();
        List<SearchResult> out = slow.rerank("q", input, 2, 50);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed)
                .as("the reranker must honor the timeout budget — observed elapsed = %d ms", elapsed)
                .isLessThan(900);
        assertThat(out)
                .as("on timeout, original retrieval order is returned, truncated to topN")
                .extracting(SearchResult::content)
                .containsExactly("alpha", "beta");
        assertThat(out.get(0).preRerankRank())
                .as("fallback path must not have applied reranker annotations")
                .isNull();
        assertThat(out.get(0).rerankerScore()).isNull();
    }

    @Test
    void handlesEmptyInputCleanly() {
        assertThat(new StubbedReranker(Map.of()).rerank("q", List.of(), 5, 1000)).isEmpty();
    }
}
