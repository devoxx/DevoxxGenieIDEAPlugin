package com.devoxx.genie.ui.settings.rag;

import com.intellij.openapi.ui.ComboBox;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The RAG embedding-model field must accept any Ollama model name, not just the suggestions. */
class EmbeddingModelComboBoxTest {

    @Test
    void noSavedModelShowsTheDefaultEvenThoughTheListIsAlphabetical() {
        ComboBox<String> combo = RAGSettingsComponent.createEmbeddingModelComboBox(null);

        assertThat(combo.getItemAt(0)).isNotEqualTo("nomic-embed-text");
        assertThat(combo.getSelectedItem()).isEqualTo("nomic-embed-text");
    }

    @Test
    void customModelNameOutsideTheSuggestionsIsKept() {
        // A non-editable JComboBox silently ignores setSelectedItem for non-members, which
        // would make reset() show the wrong model.
        ComboBox<String> combo = RAGSettingsComponent.createEmbeddingModelComboBox(
                "hf.co/jinaai/jina-embeddings-v5-text-nano-GGUF:Q8_0");

        assertThat(RAGSettingsComponent.getEmbeddingModelName(combo))
                .isEqualTo("hf.co/jinaai/jina-embeddings-v5-text-nano-GGUF:Q8_0");
    }

    @Test
    void textTypedButNotYetCommittedIsWhatGetsSaved() {
        ComboBox<String> combo = RAGSettingsComponent.createEmbeddingModelComboBox("nomic-embed-text");

        combo.getEditor().setItem("  bge-m3:latest ");

        assertThat(RAGSettingsComponent.getEmbeddingModelName(combo)).isEqualTo("bge-m3");
    }

    @Test
    void blankMeansTheDefault() {
        ComboBox<String> combo = RAGSettingsComponent.createEmbeddingModelComboBox(null);
        assertThat(RAGSettingsComponent.getEmbeddingModelName(combo)).isEqualTo("nomic-embed-text");

        combo.getEditor().setItem("");
        assertThat(RAGSettingsComponent.getEmbeddingModelName(combo)).isEqualTo("nomic-embed-text");
    }
}
