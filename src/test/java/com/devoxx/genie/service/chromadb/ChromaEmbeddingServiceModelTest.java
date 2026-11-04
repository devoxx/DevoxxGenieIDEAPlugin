package com.devoxx.genie.service.chromadb;

import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import com.devoxx.genie.service.rag.embedding.EmbeddingProvider;
import com.devoxx.genie.service.rag.embedding.EmbeddingProviderSettings;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
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
    void defaultSettingsEmbedWithNomicEmbedTextAndItsPrefixes() {
        when(stateService.getRagEmbeddingModelName()).thenReturn(null);

        EmbeddingFingerprint configured = service.configuredFingerprint();

        assertThat(configured.modelName()).isEqualTo("nomic-embed-text");
        assertThat(configured.documentPrefix()).isEqualTo("search_document: ");
        assertThat(service.configuredQueryPrefix()).isEqualTo("search_query: ");
        assertThat(configured).as("this difference is what makes an upgraded install's index stale")
                .isNotEqualTo(EmbeddingFingerprint.LEGACY);
        assertThat(modelNameOf(service.getEmbeddingModel())).isEqualTo("nomic-embed-text");
    }

    @Test
    void emptyPrefixTheUserChoseIsKept() {
        when(stateService.getRagEmbeddingModelName()).thenReturn("nomic-embed-text");
        when(stateService.getRagDocumentPrefix()).thenReturn("");
        when(stateService.getRagQueryPrefix()).thenReturn("");

        assertThat(service.configuredFingerprint().documentPrefix()).isEmpty();
        assertThat(service.configuredQueryPrefix()).isEmpty();
    }

    @Test
    void zeroDimensionMeansTheModelsOwnSize() {
        when(stateService.getRagEmbeddingModelName()).thenReturn("bge-m3");

        when(stateService.getRagOutputDimension()).thenReturn(0);
        assertThat(service.configuredFingerprint().outputDimension()).isNull();

        when(stateService.getRagOutputDimension()).thenReturn(256);
        assertThat(service.configuredFingerprint().outputDimension()).isEqualTo(256);
    }

    @Test
    void selectedProviderSuppliesTheClientAndItsSettings() {
        when(stateService.getRagEmbeddingProvider()).thenReturn(EmbeddingProvider.CUSTOM_OPENAI.id());
        when(stateService.getRagEmbeddingModelName()).thenReturn("my-model");
        when(stateService.getCustomOpenAIUrl()).thenReturn("http://localhost:8001/v1");

        assertThat(service.configuredFingerprint().provider()).isEqualTo("custom-openai");
        assertThat(service.getEmbeddingModel()).isInstanceOf(OpenAiEmbeddingModel.class);
    }

    @Test
    void currentIndexUsesTheConfiguredQueryPrefixAndAStaleOneTheRecordedOne() {
        EmbeddingFingerprint current = new EmbeddingFingerprint("ollama", "bge-m3");

        assertThat(ChromaEmbeddingService.queryPrefixFor(current, current, "new: ", "old: "))
                .as("changing only the query prefix applies straight away").isEqualTo("new: ");
        assertThat(ChromaEmbeddingService.queryPrefixFor(EmbeddingFingerprint.LEGACY, current, "new: ", ""))
                .as("an upgraded install's index was built without prefixes").isEmpty();
    }

    @Test
    void dropdownListsEnabledProvidersOllamaAlwaysAndTheSelectedOne() {
        when(stateService.isOpenAIEnabled()).thenReturn(true);

        assertThat(EmbeddingProviderSettings.selectable(stateService, EmbeddingProvider.OLLAMA))
                .containsExactly(EmbeddingProvider.OLLAMA, EmbeddingProvider.OPENAI);
        assertThat(EmbeddingProviderSettings.selectable(stateService, EmbeddingProvider.LLAMA_CPP))
                .as("a selected provider that was later disabled stays visible, so it can be changed")
                .containsExactly(EmbeddingProvider.OLLAMA, EmbeddingProvider.OPENAI, EmbeddingProvider.LLAMA_CPP);
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

        EmbeddingModel model = service.getEmbeddingModel(EmbeddingFingerprint.LEGACY);

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
