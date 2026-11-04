package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RagIndexStaleNotifierTest {

    private static final EmbeddingFingerprint LEGACY = EmbeddingFingerprint.LEGACY;
    private static final EmbeddingFingerprint NOMIC_PREFIXED =
            new EmbeddingFingerprint("ollama", "nomic-embed-text", null, "search_document: ");
    private static final EmbeddingFingerprint BGE = EmbeddingFingerprint.ollama("bge-m3");

    @Test
    void indexBuiltWithAnotherSetupIsStale() {
        assertThat(RagIndexStaleNotifier.staleIndex(Optional.of(LEGACY.id()), BGE)).contains(LEGACY);
    }

    @Test
    void indexBuiltWithTheConfiguredSetupIsNotStale() {
        assertThat(RagIndexStaleNotifier.staleIndex(Optional.of(BGE.id()), BGE)).isEmpty();
    }

    @Test
    void upgradedInstallIsStaleBecauseOfTheNewDefaultPrefix() {
        assertThat(RagIndexStaleNotifier.staleIndex(Optional.of(LEGACY.id()), NOMIC_PREFIXED)).contains(LEGACY);
    }

    @Test
    void switchingBackToTheSetupThatBuiltTheIndexNeedsNoReindex() {
        assertThat(RagIndexStaleNotifier.staleIndex(Optional.of(BGE.id()),
                EmbeddingFingerprint.ollama("bge-m3:latest"))).isEmpty();
    }

    @Test
    void projectWithoutAnIndexIsNeverStale() {
        assertThat(RagIndexStaleNotifier.staleIndex(Optional.empty(), BGE)).isEmpty();
    }

    @Test
    void modelChangeMessageNamesBothModelsAndSaysSearchStillWorks() {
        assertThat(RagIndexStaleNotifier.message(LEGACY, BGE))
                .contains("nomic-embed-text")
                .contains("bge-m3")
                .contains("Re-index")
                .contains("keeps using nomic-embed-text");
    }

    @Test
    void upgradeMessageExplainsTheBenefit() {
        assertThat(RagIndexStaleNotifier.message(LEGACY, NOMIC_PREFIXED))
                .contains("recommended prefixes")
                .contains("keeps using your existing index");
    }

    @Test
    void sameModelWithOtherSettingsDoesNotClaimTheModelChanged() {
        EmbeddingFingerprint truncated = new EmbeddingFingerprint("ollama", "bge-m3", 256, "");

        assertThat(RagIndexStaleNotifier.message(BGE, truncated))
                .contains("different embedding settings")
                .doesNotContain("built with bge-m3");
    }

    @Test
    void notificationKeyChangesOnlyWithTheIndexOrTheSettings() {
        String key = RagIndexStaleNotifier.notificationKey(LEGACY, NOMIC_PREFIXED);

        assertThat(RagIndexStaleNotifier.notificationKey(LEGACY, NOMIC_PREFIXED)).isEqualTo(key);
        assertThat(RagIndexStaleNotifier.notificationKey(LEGACY, BGE)).isNotEqualTo(key);
        assertThat(RagIndexStaleNotifier.notificationKey(BGE, NOMIC_PREFIXED)).isNotEqualTo(key);
    }
}
