package com.devoxx.genie.service.rag;

import com.devoxx.genie.service.rag.embedding.EmbeddingProvider;
import com.devoxx.genie.service.rag.validator.EmbeddingModelValidator;
import com.devoxx.genie.service.rag.validator.EmbeddingProviderValidator;
import com.devoxx.genie.service.rag.validator.OllamaValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class RagValidatorChainTest {

    @Test
    void ollamaUsesTheOllamaChecks() {
        assertThat(RagValidatorService.embeddingValidators(EmbeddingProvider.OLLAMA))
                .hasExactlyElementsOfTypes(OllamaValidator.class, EmbeddingModelValidator.class);
    }

    @ParameterizedTest
    @EnumSource(value = EmbeddingProvider.class, names = "OLLAMA", mode = EnumSource.Mode.EXCLUDE)
    void otherProvidersAreNotBlockedByTheOllamaChecks(EmbeddingProvider provider) {
        assertThat(RagValidatorService.embeddingValidators(provider))
                .hasExactlyElementsOfTypes(EmbeddingProviderValidator.class);
    }
}
