package com.devoxx.genie.service.rag.rerank;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link OllamaReranker#normalizeScore} — the parsing layer that
 * converts the LLM's freeform reply into a [0.0, 1.0] score. Network-free.
 */
class OllamaRerankerNormalizeScoreTest {

    @Test
    void parsesBareInteger() {
        assertThat(OllamaReranker.normalizeScore("7")).isEqualTo(0.7);
    }

    @Test
    void parsesScoreWithLabel() {
        assertThat(OllamaReranker.normalizeScore("Score: 8")).isEqualTo(0.8);
    }

    @Test
    void clampsAboveTen() {
        assertThat(OllamaReranker.normalizeScore("42")).isEqualTo(1.0);
    }

    @Test
    void clampsBelowZero() {
        // Negative scores are now captured and then clamped to 0 — see task-214 review item M4.
        assertThat(OllamaReranker.normalizeScore("-3")).isEqualTo(0.0);
    }

    @Test
    void returnsZeroWhenNoDigits() {
        assertThat(OllamaReranker.normalizeScore("I cannot decide")).isEqualTo(0.0);
    }

    @Test
    void returnsZeroForNull() {
        assertThat(OllamaReranker.normalizeScore(null)).isEqualTo(0.0);
    }

    @Test
    void parsesFirstNumberOnly() {
        // "9/10" should grab the 9.
        assertThat(OllamaReranker.normalizeScore("9/10")).isEqualTo(0.9);
    }
}
