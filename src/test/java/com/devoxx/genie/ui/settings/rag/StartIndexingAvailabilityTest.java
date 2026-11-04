package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.service.rag.embedding.EmbeddingProvider;
import com.devoxx.genie.service.rag.validator.ValidationActionType;
import com.devoxx.genie.service.rag.validator.ValidatorStatus;
import com.devoxx.genie.service.rag.validator.ValidatorType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StartIndexingAvailabilityTest {

    private static final ValidatorStatus CHROMA_OK =
            new ValidatorStatus(null, ValidatorType.CHROMADB, ValidationActionType.OK, true, "ok");

    @Test
    void missingEmbeddingModelStillAllowsIndexingBecauseItOffersADownload() {
        ValidatorStatus missingModel = new ValidatorStatus(null, ValidatorType.EMBEDDING_MODEL,
                ValidationActionType.PULL_EMBEDDING_MODEL, false, "not found");

        assertThat(RAGSettingsComponent.canStartIndexing(List.of(CHROMA_OK, missingModel))).isTrue();
    }

    @Test
    void otherFailuresStillBlockIndexing() {
        ValidatorStatus chromaDown = new ValidatorStatus(null, ValidatorType.CHROMADB,
                ValidationActionType.START_CHROMA_DB, false, "not running");
        ValidatorStatus uncheckable = new ValidatorStatus(null, ValidatorType.EMBEDDING_MODEL,
                ValidationActionType.OK, false, "unable to check");

        assertThat(RAGSettingsComponent.canStartIndexing(List.of(chromaDown))).isFalse();
        assertThat(RAGSettingsComponent.canStartIndexing(List.of(uncheckable))).isFalse();
    }

    @Test
    void downloadQuestionNamesTheModelAndSaysItTakesTime() {
        assertThat(EmbeddingModelDownloadPrompt.question("embeddinggemma"))
                .contains("embeddinggemma")
                .contains("Download")
                .contains("several minutes");
    }

    @Test
    void blankModelMessageNamesTheProviderAndSaysTheIndexIsSafe() {
        assertThat(EmbeddingModelDownloadPrompt.missingModelMessage(EmbeddingProvider.LLAMA_CPP))
                .contains("llama.cpp")
                .contains("not changed")
                .doesNotContain("nomic");
    }

    @Test
    void suggestionsCoverOllamasEmbeddingModels() {
        assertThat(RAGSettingsComponent.SUGGESTED_EMBEDDING_MODELS)
                .contains("nomic-embed-text", "embeddinggemma-2", "qwen3-embedding", "snowflake-arctic-embed2", "all-minilm")
                .doesNotHaveDuplicates();
    }
}
