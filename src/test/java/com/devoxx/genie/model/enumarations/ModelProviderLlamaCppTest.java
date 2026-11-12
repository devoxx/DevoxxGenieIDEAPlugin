package com.devoxx.genie.model.enumarations;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelProviderLlamaCppTest {

    @Test
    void llamaCppUsesItsOfficialName() {
        assertThat(ModelProvider.LLaMA.getName()).isEqualTo("llama.cpp");
        assertThat(ModelProvider.LLaMA.toString()).isEqualTo("llama.cpp");
        assertThat(ModelProvider.fromString("llama.cpp")).isEqualTo(ModelProvider.LLaMA);
    }

    @Test
    void legacyLlamaCppNameStillResolves() {
        // Saved settings (selected provider, sub-agent configs) store the old display name.
        assertThat(ModelProvider.fromString("LLaMA.c++")).isEqualTo(ModelProvider.LLaMA);
        assertThat(ModelProvider.LLaMA.hasName("LLaMA.c++")).isTrue();
        assertThat(ModelProvider.LLaMA.hasName("llama.cpp")).isTrue();
        assertThat(ModelProvider.Ollama.hasName("LLaMA.c++")).isFalse();
    }
}
