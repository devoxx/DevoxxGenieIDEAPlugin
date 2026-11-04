package com.devoxx.genie.service.rag.benchmark;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class RetrievalMetricsTest {

    // ---------------- file-level ranking ----------------

    @Test
    void chunksCollapseToFilesInOrderOfFirstAppearance() {
        List<String> chunkFiles = List.of("a.java", "b.java", "a.java", "c.md", "b.java", "d.md");

        assertThat(RetrievalMetrics.rankFiles(chunkFiles))
                .containsExactly("a.java", "b.java", "c.md", "d.md");
    }

    @Test
    void firstRelevantRankIsOneBased() {
        List<String> ranked = List.of("x", "y", "z");

        assertThat(RetrievalMetrics.firstRelevantRank(ranked, Set.of("x"))).isEqualTo(1);
        assertThat(RetrievalMetrics.firstRelevantRank(ranked, Set.of("z"))).isEqualTo(3);
    }

    @Test
    void firstRelevantRankUsesTheBestOfSeveralExpectedFiles() {
        List<String> ranked = List.of("x", "y", "z");

        assertThat(RetrievalMetrics.firstRelevantRank(ranked, Set.of("z", "y"))).isEqualTo(2);
    }

    @Test
    void missingExpectedFileHasRankZero() {
        assertThat(RetrievalMetrics.firstRelevantRank(List.of("x", "y"), Set.of("nope"))).isZero();
        assertThat(RetrievalMetrics.firstRelevantRank(List.of(), Set.of("x"))).isZero();
    }

    // ---------------- summary ----------------

    @Test
    void summaryOfFixedRanks() {
        // ranks: 1, 3, 7, miss  -> hits@1 = 1, hits@5 = 2, hits@10 = 3
        // MRR = (1 + 1/3 + 1/7 + 0) / 4
        RetrievalMetrics.Summary s = RetrievalMetrics.summarize(List.of(1, 3, 7, 0));

        assertThat(s.questions()).isEqualTo(4);
        assertThat(s.recallAt1()).isCloseTo(0.25, within(1e-9));
        assertThat(s.recallAt5()).isCloseTo(0.50, within(1e-9));
        assertThat(s.recallAt10()).isCloseTo(0.75, within(1e-9));
        assertThat(s.mrr()).isCloseTo((1 + 1.0 / 3 + 1.0 / 7) / 4, within(1e-9));
    }

    @Test
    void rankExactlyAtTheCutoffCounts() {
        RetrievalMetrics.Summary s = RetrievalMetrics.summarize(List.of(5, 10));

        assertThat(s.recallAt5()).isCloseTo(0.5, within(1e-9));
        assertThat(s.recallAt10()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void rankBeyondTenIsAMissForRecallButStillCountsForMrr() {
        RetrievalMetrics.Summary s = RetrievalMetrics.summarize(List.of(20));

        assertThat(s.recallAt10()).isZero();
        assertThat(s.mrr()).isCloseTo(0.05, within(1e-9));
    }

    @Test
    void perfectAndEmptyRetrieval() {
        RetrievalMetrics.Summary perfect = RetrievalMetrics.summarize(List.of(1, 1, 1));
        assertThat(perfect.recallAt1()).isEqualTo(1.0);
        assertThat(perfect.mrr()).isEqualTo(1.0);

        RetrievalMetrics.Summary none = RetrievalMetrics.summarize(List.of(0, 0));
        assertThat(none.recallAt10()).isZero();
        assertThat(none.mrr()).isZero();
    }

    @Test
    void noQuestionsIsAnError() {
        assertThatThrownBy(() -> RetrievalMetrics.summarize(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeRankIsRejected() {
        assertThatThrownBy(() -> RetrievalMetrics.summarize(List.of(1, -1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
