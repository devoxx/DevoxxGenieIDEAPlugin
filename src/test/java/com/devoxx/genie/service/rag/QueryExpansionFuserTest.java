package com.devoxx.genie.service.rag;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class QueryExpansionFuserTest {

    private static SearchResult hit(String path, double score, String content) {
        return new SearchResult(path, score, content);
    }

    // ---- Behavior ---------------------------------------------------------------------------

    @Test
    void expandAndFuse_singleVariant_returnsRetrievedOrder() {
        SearchResult a = hit("/a", 0.9, "alpha");
        SearchResult b = hit("/b", 0.8, "beta");
        SearchResult c = hit("/c", 0.7, "gamma");

        List<SearchResult> fused = QueryExpansionFuser.expandAndFuse(
                List.of("q"),
                q -> List.of(a, b, c),
                10);

        assertThat(fused).containsExactly(a, b, c);
    }

    @Test
    void expandAndFuse_resultAppearingInMultipleVariants_outranksSingletons() {
        // /shared appears mid-rank in two variants; /onlyA only top-ranked in variant 1.
        // RRF: /shared = 1/(60+2) + 1/(60+2) ≈ 0.0323; /onlyA = 1/(60+1) ≈ 0.0164.
        SearchResult shared = hit("/shared", 0.7, "common");
        SearchResult onlyA = hit("/onlyA", 0.95, "uniqueA");
        SearchResult onlyB = hit("/onlyB", 0.95, "uniqueB");

        List<SearchResult> fused = QueryExpansionFuser.expandAndFuse(
                List.of("q1", "q2"),
                q -> q.equals("q1") ? List.of(onlyA, shared) : List.of(onlyB, shared),
                10);

        assertThat(fused.get(0)).isEqualTo(shared);
        assertThat(fused).containsExactlyInAnyOrder(shared, onlyA, onlyB);
    }

    @Test
    void expandAndFuse_dedupOnFilePathAndContent_notFilePathAlone() {
        // Same filePath, different chunks — must NOT collapse: distinct chunks of the same
        // file are independently relevant evidence
        SearchResult chunkA = hit("/file.md", 0.9, "chunk A text");
        SearchResult chunkB = hit("/file.md", 0.9, "chunk B text");

        List<SearchResult> fused = QueryExpansionFuser.expandAndFuse(
                List.of("q"),
                q -> List.of(chunkA, chunkB),
                10);

        assertThat(fused).containsExactly(chunkA, chunkB);
    }

    @Test
    void expandAndFuse_dedupOnIdenticalHit_keepsFirstOccurrence() {
        // Same (filePath, content) returned across variants → one entry; score accumulates
        SearchResult dup = hit("/file.md", 0.9, "same chunk");

        List<SearchResult> fused = QueryExpansionFuser.expandAndFuse(
                List.of("q1", "q2", "q3"),
                q -> List.of(dup),
                10);

        assertThat(fused).hasSize(1);
        assertThat(fused.get(0)).isEqualTo(dup);
    }

    @Test
    void expandAndFuse_respectsMaxResults() {
        SearchResult a = hit("/a", 0.9, "alpha");
        SearchResult b = hit("/b", 0.8, "beta");
        SearchResult c = hit("/c", 0.7, "gamma");

        List<SearchResult> fused = QueryExpansionFuser.expandAndFuse(
                List.of("q"),
                q -> List.of(a, b, c),
                2);

        assertThat(fused).hasSize(2).containsExactly(a, b);
    }

    // ---- Guard clauses ---------------------------------------------------------------------

    @Test
    void expandAndFuse_emptyVariants_returnsEmpty() {
        AtomicInteger calls = new AtomicInteger();
        List<SearchResult> fused = QueryExpansionFuser.expandAndFuse(
                List.of(),
                q -> { calls.incrementAndGet(); return List.of(); },
                10);
        assertThat(fused).isEmpty();
        assertThat(calls.get()).isZero();
    }

    @Test
    void expandAndFuse_nonPositiveMaxResults_returnsEmpty() {
        assertThat(QueryExpansionFuser.expandAndFuse(List.of("q"), q -> List.of(hit("/a", 1.0, "x")), 0))
                .isEmpty();
    }

    @Test
    void expandAndFuse_retrieverReturnsNull_isTolerated() {
        SearchResult a = hit("/a", 0.9, "alpha");
        List<SearchResult> fused = QueryExpansionFuser.expandAndFuse(
                List.of("good", "bad"),
                q -> q.equals("good") ? List.of(a) : null,
                10);
        assertThat(fused).containsExactly(a);
    }
}
