package com.devoxx.genie.service.spec.search;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.TaskSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpecSearchServiceTest {

    @Test
    void buildSearchPayloadShouldIncludeTitle() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Add authentication")
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        assertThat(payload).contains("Add authentication");
    }

    @Test
    void buildSearchPayloadShouldIncludeDescription() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Auth")
                .description("Implement JWT-based authentication for REST API")
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        assertThat(payload).contains("JWT-based authentication");
    }

    @Test
    void buildSearchPayloadShouldIncludeLabels() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Auth")
                .labels(List.of("security", "api"))
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        assertThat(payload).contains("security");
        assertThat(payload).contains("api");
    }

    @Test
    void buildSearchPayloadShouldIncludeAcceptanceCriteria() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Auth")
                .acceptanceCriteria(List.of(
                        AcceptanceCriterion.builder().index(0).text("POST /auth/login works").checked(false).build(),
                        AcceptanceCriterion.builder().index(1).text("JWT tokens expire after 24h").checked(false).build()
                ))
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        assertThat(payload).contains("POST /auth/login works");
        assertThat(payload).contains("JWT tokens expire after 24h");
    }

    @Test
    void buildSearchPayloadShouldIncludeMilestone() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Auth")
                .milestone("v2.0")
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        assertThat(payload).contains("v2.0");
    }

    @Test
    void buildSearchPayloadShouldWeightTitleByRepeating() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Authentication")
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        // Title should appear twice for weighting
        int firstIdx = payload.indexOf("Authentication");
        int secondIdx = payload.indexOf("Authentication", firstIdx + 1);
        assertThat(secondIdx).isGreaterThan(firstIdx);
    }

    @Test
    void buildSearchPayloadShouldHandleNullFields() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        // Should not throw, may be empty
        assertThat(payload).isNotNull();
    }

    @Test
    void buildSearchPayloadShouldIncludeImplementationPlan() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Auth")
                .implementationPlan("1. Add spring security dependency\n2. Create auth controller")
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        assertThat(payload).contains("spring security");
    }

    // === Fuzzy fallback integration tests (via BM25SearchEngine + FuzzySearchEngine directly) ===

    @Test
    void fuzzyEngineShouldFindTypoMatches() {
        // Verify the fuzzy engine catches typos that BM25 misses entirely
        BM25SearchEngine bm25 = new BM25SearchEngine();
        FuzzySearchEngine fuzzy = new FuzzySearchEngine();

        bm25.index("task-1", "implement authentication module");
        fuzzy.index("task-1", "implement authentication module");

        // BM25 requires exact token match — "authentcation" won't match "authentication"
        List<BM25SearchEngine.ScoredResult> bm25Results = bm25.search("authentcation", 3);
        assertThat(bm25Results).isEmpty();

        // Fuzzy catches it
        List<BM25SearchEngine.ScoredResult> fuzzyResults = fuzzy.search("authentcation", 3);
        assertThat(fuzzyResults).isNotEmpty();
        assertThat(fuzzyResults.get(0).docId()).isEqualTo("task-1");
    }

    @Test
    void bm25AndFuzzyScoresCanBeMerged() {
        BM25SearchEngine bm25 = new BM25SearchEngine();
        FuzzySearchEngine fuzzy = new FuzzySearchEngine();

        // Two docs: one with exact match, one with only fuzzy match
        bm25.index("task-exact", "implement authentication service");
        bm25.index("task-fuzzy-only", "implement authorization service");
        fuzzy.index("task-exact", "implement authentication service");
        fuzzy.index("task-fuzzy-only", "implement authorization service");

        // BM25 finds only the exact match
        List<BM25SearchEngine.ScoredResult> bm25Results = bm25.search("authentication", 3);
        assertThat(bm25Results).hasSize(1);
        assertThat(bm25Results.get(0).docId()).isEqualTo("task-exact");

        // Fuzzy finds both (authorization is similar to authentication)
        List<BM25SearchEngine.ScoredResult> fuzzyResults = fuzzy.search("authentication", 3);
        assertThat(fuzzyResults).hasSizeGreaterThanOrEqualTo(2);

        // Merged: both should appear, with exact match ranked higher
        java.util.Map<String, Double> merged = new java.util.LinkedHashMap<>();
        for (BM25SearchEngine.ScoredResult r : bm25Results) {
            merged.put(r.docId(), r.score());
        }
        for (BM25SearchEngine.ScoredResult r : fuzzyResults) {
            merged.merge(r.docId(), r.score() * 0.3, Double::sum);
        }

        assertThat(merged).containsKey("task-exact");
        assertThat(merged).containsKey("task-fuzzy-only");
        assertThat(merged.get("task-exact")).isGreaterThan(merged.get("task-fuzzy-only"));
    }
}
