package com.devoxx.genie.service.spec.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FuzzySearchEngineTest {

    private FuzzySearchEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FuzzySearchEngine();
    }

    // === Levenshtein distance tests ===

    @Test
    void identicalStringsShouldHaveZeroDistance() {
        assertThat(FuzzySearchEngine.levenshteinDistance("hello", "hello")).isEqualTo(0);
    }

    @Test
    void singleInsertionShouldBeDistanceOne() {
        assertThat(FuzzySearchEngine.levenshteinDistance("cat", "cats")).isEqualTo(1);
    }

    @Test
    void singleDeletionShouldBeDistanceOne() {
        assertThat(FuzzySearchEngine.levenshteinDistance("cats", "cat")).isEqualTo(1);
    }

    @Test
    void singleSubstitutionShouldBeDistanceOne() {
        assertThat(FuzzySearchEngine.levenshteinDistance("cat", "car")).isEqualTo(1);
    }

    @Test
    void completelyDifferentStringsShouldHaveHighDistance() {
        assertThat(FuzzySearchEngine.levenshteinDistance("abc", "xyz")).isEqualTo(3);
    }

    @Test
    void emptyStringShouldHaveDistanceEqualToOtherLength() {
        assertThat(FuzzySearchEngine.levenshteinDistance("", "hello")).isEqualTo(5);
        assertThat(FuzzySearchEngine.levenshteinDistance("hello", "")).isEqualTo(5);
    }

    @Test
    void bothEmptyStringsShouldHaveZeroDistance() {
        assertThat(FuzzySearchEngine.levenshteinDistance("", "")).isEqualTo(0);
    }

    // === Similarity tests ===

    @Test
    void identicalStringsShouldHaveSimilarityOne() {
        assertThat(FuzzySearchEngine.similarity("authentication", "authentication")).isEqualTo(1.0);
    }

    @Test
    void typoShouldHaveHighSimilarity() {
        // "authetication" (missing 'n') vs "authentication"
        double sim = FuzzySearchEngine.similarity("authetication", "authentication");
        assertThat(sim).isGreaterThan(0.9);
    }

    @Test
    void prefixShouldHaveModestSimilarity() {
        // "auth" vs "authentication" — 4/14 chars match in position
        double sim = FuzzySearchEngine.similarity("auth", "authentication");
        assertThat(sim).isGreaterThan(0.0);
        // But since auth is short and authentication is long, distance is 10, so sim = 1 - 10/14 ≈ 0.28
        assertThat(sim).isLessThan(0.6);
    }

    @Test
    void completelydifferentStringsShouldHaveLowSimilarity() {
        double sim = FuzzySearchEngine.similarity("zebra", "authentication");
        assertThat(sim).isLessThan(0.3);
    }

    // === Search tests ===

    @Test
    void shouldReturnEmptyForEmptyIndex() {
        List<BM25SearchEngine.ScoredResult> results = engine.search("authentication", 5);
        assertThat(results).isEmpty();
    }

    @Test
    void shouldReturnEmptyForBlankQuery() {
        engine.index("task-1", "implement user authentication");
        List<BM25SearchEngine.ScoredResult> results = engine.search("   ", 5);
        assertThat(results).isEmpty();
    }

    @Test
    void shouldMatchExactTerms() {
        engine.index("task-1", "implement authentication module");
        engine.index("task-2", "add dark mode toggle");

        List<BM25SearchEngine.ScoredResult> results = engine.search("authentication", 5);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).docId()).isEqualTo("task-1");
    }

    @Test
    void shouldMatchTypos() {
        engine.index("task-1", "implement authentication module");
        engine.index("task-2", "add dark mode toggle");

        // "authentcation" is a typo for "authentication"
        List<BM25SearchEngine.ScoredResult> results = engine.search("authentcation", 5);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).docId()).isEqualTo("task-1");
    }

    @Test
    void shouldMatchSimilarTerms() {
        engine.index("task-1", "implement authorization checks");
        engine.index("task-2", "add color palette");

        // "authorization" is similar to "authentication" (both start with "auth")
        // but edit distance may be too high for the 0.6 threshold with these terms
        List<BM25SearchEngine.ScoredResult> results = engine.search("authentication", 5);

        // Whether this matches depends on the similarity threshold
        // "authorization" (13 chars) vs "authentication" (14 chars): distance = 5, sim ≈ 0.64
        // This should be above the 0.6 threshold
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).docId()).isEqualTo("task-1");
    }

    @Test
    void shouldNotMatchCompletelyUnrelatedTerms() {
        engine.index("task-1", "zebra crossing infrastructure");

        List<BM25SearchEngine.ScoredResult> results = engine.search("authentication", 5);

        assertThat(results).isEmpty();
    }

    @Test
    void shouldRespectLimit() {
        engine.index("task-1", "authentication module");
        engine.index("task-2", "authorization service");
        engine.index("task-3", "authenticator factory");

        List<BM25SearchEngine.ScoredResult> results = engine.search("authentication", 2);

        assertThat(results).hasSize(2);
    }

    @Test
    void shouldRankBetterMatchesHigher() {
        engine.index("task-exact", "authentication service");
        engine.index("task-typo", "authentcation service");
        engine.index("task-distant", "authorization service");

        List<BM25SearchEngine.ScoredResult> results = engine.search("authentication", 3);

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        // Exact match should score highest
        assertThat(results.get(0).docId()).isEqualTo("task-exact");
    }

    @Test
    void clearShouldResetIndex() {
        engine.index("task-1", "authentication module");
        assertThat(engine.search("authentication", 5)).isNotEmpty();

        engine.clear();
        assertThat(engine.search("authentication", 5)).isEmpty();
    }
}
