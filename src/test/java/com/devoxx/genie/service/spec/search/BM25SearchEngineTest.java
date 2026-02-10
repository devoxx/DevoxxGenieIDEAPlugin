package com.devoxx.genie.service.spec.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BM25SearchEngineTest {

    private BM25SearchEngine engine;

    @BeforeEach
    void setUp() {
        engine = new BM25SearchEngine();
    }

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
    void shouldRankExactMatchHighest() {
        engine.index("task-1", "implement JWT authentication for REST API");
        engine.index("task-2", "add dark mode toggle to settings page");
        engine.index("task-3", "refactor database connection pooling");

        List<BM25SearchEngine.ScoredResult> results = engine.search("authentication", 3);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).docId()).isEqualTo("task-1");
    }

    @Test
    void shouldRankByTermOverlap() {
        engine.index("task-1", "add user login with JWT tokens and password hashing");
        engine.index("task-2", "implement OAuth2 authentication login flow");
        engine.index("task-3", "style the navigation bar with CSS");

        List<BM25SearchEngine.ScoredResult> results = engine.search("login authentication", 3);

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        // task-2 has both "authentication" and "login"
        assertThat(results.get(0).docId()).isEqualTo("task-2");
    }

    @Test
    void shouldRespectLimit() {
        engine.index("task-1", "authentication module");
        engine.index("task-2", "authentication service");
        engine.index("task-3", "authentication controller");

        List<BM25SearchEngine.ScoredResult> results = engine.search("authentication", 2);

        assertThat(results).hasSize(2);
    }

    @Test
    void shouldNotReturnDocumentsWithNoMatchingTerms() {
        engine.index("task-1", "implement dark mode for the UI");
        engine.index("task-2", "add unit tests for payment processing");

        List<BM25SearchEngine.ScoredResult> results = engine.search("authentication", 5);

        assertThat(results).isEmpty();
    }

    @Test
    void shouldHandleMultipleTermQuery() {
        engine.index("task-1", "create REST API endpoints for user management");
        engine.index("task-2", "add error handling to REST API calls");
        engine.index("task-3", "write documentation for the codebase");

        List<BM25SearchEngine.ScoredResult> results = engine.search("REST API error handling", 3);

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        // task-2 has the most term overlap
        assertThat(results.get(0).docId()).isEqualTo("task-2");
    }

    @Test
    void shouldFilterStopWords() {
        List<String> tokens = BM25SearchEngine.tokenize("the user should be able to login");
        assertThat(tokens).doesNotContain("the", "be", "to");
        assertThat(tokens).contains("user", "should", "able", "login");
    }

    @Test
    void shouldTokenizeLowercase() {
        List<String> tokens = BM25SearchEngine.tokenize("JWT Authentication TOKEN");
        assertThat(tokens).contains("jwt", "authentication", "token");
    }

    @Test
    void shouldFilterShortTokens() {
        List<String> tokens = BM25SearchEngine.tokenize("a I x do be");
        // All are stop words or single chars
        assertThat(tokens).isEmpty();
    }

    @Test
    void clearShouldResetIndex() {
        engine.index("task-1", "authentication module");
        assertThat(engine.search("authentication", 5)).isNotEmpty();

        engine.clear();
        assertThat(engine.search("authentication", 5)).isEmpty();
    }

    @Test
    void shouldHandleLargeDocuments() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longText.append("word").append(i).append(" ");
        }
        longText.append("authentication");

        engine.index("task-long", longText.toString());
        engine.index("task-short", "authentication service");

        List<BM25SearchEngine.ScoredResult> results = engine.search("authentication", 2);

        assertThat(results).hasSize(2);
        // Shorter doc with higher TF density should score higher
        assertThat(results.get(0).docId()).isEqualTo("task-short");
    }

    @Test
    void shouldProducePositiveScores() {
        engine.index("task-1", "implement authentication");

        List<BM25SearchEngine.ScoredResult> results = engine.search("authentication", 1);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).score()).isGreaterThan(0.0);
    }

    @Test
    void shouldHandlePunctuation() {
        engine.index("task-1", "user-authentication: JWT-based, OAuth2.0");

        List<BM25SearchEngine.ScoredResult> results = engine.search("authentication jwt", 1);

        assertThat(results).hasSize(1);
    }
}
