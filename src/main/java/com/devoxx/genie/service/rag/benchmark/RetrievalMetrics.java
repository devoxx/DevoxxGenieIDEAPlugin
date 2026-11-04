package com.devoxx.genie.service.rag.benchmark;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Retrieval-quality metrics for the RAG benchmark, scored per <em>file</em>.
 *
 * <p>The vector store returns chunks; several chunks of one file may rank high. A question
 * is about finding the right file, so chunk results are first collapsed to distinct files in
 * order of each file's best-ranked chunk ({@link #rankFiles}). A question's rank is the
 * 1-based position of the first expected file in that list, or {@code 0} if none appears.
 *
 * <ul>
 *   <li><b>recall@k</b>: fraction of questions whose rank is between 1 and k (a hit rate:
 *       did any expected file make the top k?)</li>
 *   <li><b>MRR</b>: mean of {@code 1/rank} over all questions, with misses counting 0</li>
 * </ul>
 */
public final class RetrievalMetrics {

    private RetrievalMetrics() {}

    /** Aggregate scores over a benchmark run. */
    public record Summary(int questions, double recallAt1, double recallAt5, double recallAt10, double mrr) {}

    /** Distinct file paths in order of first appearance in the chunk ranking. */
    public static @NotNull List<String> rankFiles(@NotNull List<String> chunkFilePaths) {
        return new ArrayList<>(new LinkedHashSet<>(chunkFilePaths));
    }

    /** 1-based rank of the first file in {@code rankedFiles} that is expected; 0 if none. */
    public static int firstRelevantRank(@NotNull List<String> rankedFiles, @NotNull Collection<String> expected) {
        for (int i = 0; i < rankedFiles.size(); i++) {
            if (expected.contains(rankedFiles.get(i))) {
                return i + 1;
            }
        }
        return 0;
    }

    /** Summarise per-question ranks (0 = expected file not retrieved). */
    public static @NotNull Summary summarize(@NotNull List<Integer> ranks) {
        if (ranks.isEmpty()) {
            throw new IllegalArgumentException("Cannot summarise a benchmark with no questions");
        }
        int hits1 = 0;
        int hits5 = 0;
        int hits10 = 0;
        double reciprocalSum = 0;
        for (int rank : ranks) {
            if (rank < 0) {
                throw new IllegalArgumentException("Rank must be >= 0 (0 = miss), was " + rank);
            }
            if (rank == 0) continue;
            if (rank <= 1) hits1++;
            if (rank <= 5) hits5++;
            if (rank <= 10) hits10++;
            reciprocalSum += 1.0 / rank;
        }
        double n = ranks.size();
        return new Summary(ranks.size(), hits1 / n, hits5 / n, hits10 / n, reciprocalSum / n);
    }
}
