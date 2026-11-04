package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RagIndexStaleNotifierTest {

    private static final EmbeddingFingerprint NOMIC = EmbeddingFingerprint.DEFAULT;
    private static final EmbeddingFingerprint BGE = EmbeddingFingerprint.ollama("bge-m3");

    @Test
    void indexBuiltWithAnotherModelIsStale() {
        assertThat(RagIndexStaleNotifier.staleIndexModel(Optional.of(NOMIC.id()), BGE))
                .contains("nomic-embed-text");
    }

    @Test
    void indexBuiltWithTheConfiguredModelIsNotStale() {
        assertThat(RagIndexStaleNotifier.staleIndexModel(Optional.of(BGE.id()), BGE)).isEmpty();
    }

    @Test
    void switchingBackToTheModelThatBuiltTheIndexNeedsNoReindex() {
        // nomic → bge-m3 (not applied as a re-index) → nomic again: the index is fine.
        assertThat(RagIndexStaleNotifier.staleIndexModel(Optional.of(NOMIC.id()),
                EmbeddingFingerprint.ollama("nomic-embed-text:latest"))).isEmpty();
    }

    @Test
    void projectWithoutAnIndexIsNeverStale() {
        assertThat(RagIndexStaleNotifier.staleIndexModel(Optional.empty(), BGE)).isEmpty();
    }

    @Test
    void messageNamesBothModelsAndSaysSearchStillWorks() {
        String message = RagIndexStaleNotifier.message("nomic-embed-text", "bge-m3");

        assertThat(message)
                .contains("nomic-embed-text")
                .contains("bge-m3")
                .contains("Re-index");
        assertThat(message).as("users must know nothing broke in the meantime")
                .contains("keeps using nomic-embed-text");
    }
}
