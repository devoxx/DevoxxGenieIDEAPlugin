package com.devoxx.genie.service.chromadb;

import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/** Which Ollama model {@link ChromaEmbeddingService} embeds with, and when it rebuilds it (TASK-212). */
class ChromaEmbeddingServiceModelTest {

    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;
    private DevoxxGenieStateService stateService;
    private ChromaEmbeddingService service;

    @BeforeEach
    void setUp() {
        stateService = mock(DevoxxGenieStateService.class);
        stateServiceStatic = mockStatic(DevoxxGenieStateService.class);
        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
        when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:11434");
        service = new ChromaEmbeddingService();
    }

    @AfterEach
    void tearDown() {
        stateServiceStatic.close();
    }

    @Test
    void defaultSettingsStillEmbedWithNomicEmbedText() {
        when(stateService.getRagEmbeddingModelName()).thenReturn(null);

        assertThat(service.configuredFingerprint()).isEqualTo(EmbeddingFingerprint.DEFAULT);
        assertThat(modelNameOf(service.getEmbeddingModel())).isEqualTo("nomic-embed-text");
    }

    @Test
    void configuredModelIsUsed() {
        when(stateService.getRagEmbeddingModelName()).thenReturn("bge-m3");

        assertThat(service.configuredFingerprint().id()).isEqualTo("ollama:bge-m3");
        assertThat(modelNameOf(service.getEmbeddingModel())).isEqualTo("bge-m3");
    }

    @Test
    void modelIsCachedAndRebuiltWhenTheNameOrUrlChanges() {
        when(stateService.getRagEmbeddingModelName()).thenReturn("nomic-embed-text");
        EmbeddingModel first = service.getEmbeddingModel();
        assertThat(service.getEmbeddingModel()).as("same settings reuse the client").isSameAs(first);

        when(stateService.getRagEmbeddingModelName()).thenReturn("bge-m3");
        EmbeddingModel second = service.getEmbeddingModel();
        assertThat(second).isNotSameAs(first);
        assertThat(modelNameOf(second)).isEqualTo("bge-m3");

        when(stateService.getOllamaModelUrl()).thenReturn("http://gpu-box:11434");
        assertThat(service.getEmbeddingModel()).as("URL change rebuilds the client").isNotSameAs(second);
    }

    @Test
    void anIndexCanStillBeQueriedWithTheModelThatBuiltIt() {
        // After switching settings to bge-m3, search keeps using the model the index was
        // built with until the user re-indexes.
        when(stateService.getRagEmbeddingModelName()).thenReturn("bge-m3");

        EmbeddingModel model = service.getEmbeddingModel(EmbeddingFingerprint.DEFAULT);

        assertThat(modelNameOf(model)).isEqualTo("nomic-embed-text");
    }

    @Test
    void testOverrideWinsForEveryFingerprint() {
        EmbeddingModel fake = mock(EmbeddingModel.class);
        service.setEmbeddingModelOverride(fake);

        assertThat(service.getEmbeddingModel()).isSameAs(fake);
        assertThat(service.getEmbeddingModel(EmbeddingFingerprint.ollama("bge-m3"))).isSameAs(fake);
    }

    private static String modelNameOf(EmbeddingModel model) {
        assertThat(model).isInstanceOf(OllamaEmbeddingModel.class);
        return ((OllamaEmbeddingModel) model).modelName();
    }
}
