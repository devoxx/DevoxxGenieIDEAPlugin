package com.devoxx.genie.service.rag.embedding;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingProviderSettingsTest {

    @Test
    void llamaCpp_followsTheLlamaCppChatProviderSettings() {
        // RAG embeds through the same llama-server as chat, so it reads the chat provider's URL and enabled flag.
        DevoxxGenieStateService state = new DevoxxGenieStateService();
        assertThat(EmbeddingProviderSettings.baseUrl(EmbeddingProvider.LLAMA_CPP, state)).isEqualTo("http://localhost:8080");
        assertThat(EmbeddingProviderSettings.isEnabled(EmbeddingProvider.LLAMA_CPP, state)).isTrue();

        state.setLocalProviderUrl(ModelProvider.LLaMA, "http://gpu-box:8181");
        state.setLocalProviderEnabled(ModelProvider.LLaMA, false);

        assertThat(EmbeddingProviderSettings.baseUrl(EmbeddingProvider.LLAMA_CPP, state)).isEqualTo("http://gpu-box:8181");
        assertThat(EmbeddingProviderSettings.isEnabled(EmbeddingProvider.LLAMA_CPP, state)).isFalse();
    }
}
