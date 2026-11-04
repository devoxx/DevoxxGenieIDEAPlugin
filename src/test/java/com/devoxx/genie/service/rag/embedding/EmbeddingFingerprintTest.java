package com.devoxx.genie.service.rag.embedding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingFingerprintTest {

    @Test
    void defaultIsOllamaNomicEmbedText() {
        assertThat(EmbeddingFingerprint.LEGACY.id()).isEqualTo("ollama:nomic-embed-text");
        assertThat(EmbeddingFingerprint.DEFAULT_MODEL_NAME).isEqualTo("nomic-embed-text");
    }

    @Test
    void blankOrNullModelNameMeansTheDefault() {
        assertThat(EmbeddingFingerprint.ollama(null)).isEqualTo(EmbeddingFingerprint.LEGACY);
        assertThat(EmbeddingFingerprint.ollama("  ")).isEqualTo(EmbeddingFingerprint.LEGACY);
    }

    @Test
    void latestTagIsTheSameModelAsNoTag() {
        // Ollama resolves "name" to "name:latest", so both must produce the same index binding.
        assertThat(EmbeddingFingerprint.ollama("nomic-embed-text:latest"))
                .isEqualTo(EmbeddingFingerprint.ollama(" nomic-embed-text "));
        assertThat(EmbeddingFingerprint.ollama("bge-m3:latest").modelName()).isEqualTo("bge-m3");
    }

    @Test
    void otherTagsAreDifferentModels() {
        assertThat(EmbeddingFingerprint.ollama("nomic-embed-text:v1.5"))
                .isNotEqualTo(EmbeddingFingerprint.LEGACY);
    }

    @Test
    void idRoundTripsThroughParseIncludingColonsInTheModelName() {
        EmbeddingFingerprint fp = EmbeddingFingerprint.ollama("hf.co/jinaai/jina-embeddings-v5-text-nano-GGUF:Q8_0");

        assertThat(fp.id()).isEqualTo("ollama:hf.co/jinaai/jina-embeddings-v5-text-nano-GGUF:Q8_0");
        assertThat(EmbeddingFingerprint.parse(fp.id())).isEqualTo(fp);
    }

    @Test
    void missingFingerprintIsTheLegacyDefault() {
        // Manifest entries written before TASK-212 carry no fingerprint; they were all built
        // with nomic-embed-text, so existing installs must not be forced to re-index.
        assertThat(EmbeddingFingerprint.parse(null)).isEqualTo(EmbeddingFingerprint.LEGACY);
        assertThat(EmbeddingFingerprint.parse("")).isEqualTo(EmbeddingFingerprint.LEGACY);
    }

    @Test
    void malformedIdIsRejected() {
        assertThatThrownBy(() -> EmbeddingFingerprint.parse("no-provider"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sameOllamaModelIgnoresLatestTagAndWhitespace() {
        assertThat(EmbeddingFingerprint.sameOllamaModel("bge-m3", "bge-m3:latest")).isTrue();
        assertThat(EmbeddingFingerprint.sameOllamaModel("nomic-embed-text", "nomic-embed-text-v1.5")).isFalse();
        assertThat(EmbeddingFingerprint.sameOllamaModel("nomic-embed-text", "nomic-embed-text:v1.5")).isFalse();
    }
}
