package com.devoxx.genie.service.rag.validator;

import com.devoxx.genie.chatmodel.local.ollama.OllamaModelService;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmbeddingModelValidatorTest {

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private OllamaModelService ollamaModelService;

    @Mock
    private Application application;

    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;
    private MockedStatic<OllamaModelService> ollamaModelServiceStatic;
    private MockedStatic<ApplicationManager> appManagerStatic;

    private EmbeddingModelValidator validator;

    @BeforeEach
    void setUp() {
        stateServiceStatic = mockStatic(DevoxxGenieStateService.class);
        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        ollamaModelServiceStatic = mockStatic(OllamaModelService.class);
        ollamaModelServiceStatic.when(OllamaModelService::getInstance).thenReturn(ollamaModelService);

        appManagerStatic = mockStatic(ApplicationManager.class);
        appManagerStatic.when(ApplicationManager::getApplication).thenReturn(application);

        when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:11434");
        when(stateService.getRagEmbeddingModelName()).thenReturn("nomic-embed-text");

        validator = new EmbeddingModelValidator();
    }

    @AfterEach
    void tearDown() {
        stateServiceStatic.close();
        ollamaModelServiceStatic.close();
        appManagerStatic.close();
    }

    private void installed(String... names) throws IOException {
        when(ollamaModelService.getModels()).thenReturn(Arrays.stream(names).map(n -> {
            OllamaModelEntryDTO dto = new OllamaModelEntryDTO();
            dto.setName(n);
            return dto;
        }).toArray(OllamaModelEntryDTO[]::new));
    }

    @Test
    void isValid_whenOllamaUrlIsNull_returnsFalse() {
        when(stateService.getOllamaModelUrl()).thenReturn(null);

        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Ollama model URL is not set");
    }

    @Test
    void isValid_whenOllamaUrlIsEmpty_returnsFalse() {
        when(stateService.getOllamaModelUrl()).thenReturn("");

        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Ollama model URL is not set");
    }

    @Test
    void isValid_whenModelsIsNull_returnsFalse() throws IOException {
        when(ollamaModelService.getModels()).thenReturn(null);

        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getErrorMessage())
                .isEqualTo("Unable to check if embedding model nomic-embed-text is present");
    }

    @Test
    void defaultModelIsFoundUnderItsLatestTag() throws IOException {
        installed("llama3:latest", "nomic-embed-text:latest");

        assertThat(validator.isValid()).isTrue();
        assertThat(validator.getMessage()).isEqualTo("Embedding model nomic-embed-text found");
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.OK);
    }

    @Test
    void blankSettingMeansTheDefaultModel() throws IOException {
        when(stateService.getRagEmbeddingModelName()).thenReturn("");
        installed("nomic-embed-text:latest");

        assertThat(validator.isValid()).isTrue();
    }

    @Test
    void nonNomicModelIsCheckedByItsConfiguredName() throws IOException {
        when(stateService.getRagEmbeddingModelName()).thenReturn("bge-m3");
        installed("nomic-embed-text:latest", "bge-m3:latest");

        assertThat(validator.isValid()).isTrue();
        assertThat(validator.getMessage()).isEqualTo("Embedding model bge-m3 found");
    }

    @Test
    void nomicAloneDoesNotSatisfyAnotherConfiguredModel() throws IOException {
        when(stateService.getRagEmbeddingModelName()).thenReturn("bge-m3");
        installed("nomic-embed-text:latest");

        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Embedding model bge-m3 not found");
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.PULL_EMBEDDING_MODEL);
    }

    @Test
    void explicitTagMustMatchExactly() throws IOException {
        when(stateService.getRagEmbeddingModelName()).thenReturn("nomic-embed-text:v1.5");
        installed("nomic-embed-text:latest");
        assertThat(validator.isValid()).isFalse();

        installed("nomic-embed-text:v1.5");
        assertThat(validator.isValid()).isTrue();
    }

    @Test
    void similarlyNamedModelIsNotAMatch() throws IOException {
        // The old check used startsWith("nomic-embed-text"); a different model must not pass.
        installed("nomic-embed-text-v2-moe:latest");

        assertThat(validator.isValid()).isFalse();
    }

    @Test
    void isValid_whenNotFound_returnsFalseWithPullAction() throws IOException {
        installed("llama3:latest");

        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Embedding model nomic-embed-text not found");
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.PULL_EMBEDDING_MODEL);
    }

    @Test
    void actionResetsToOkOnceTheModelIsPulled() throws IOException {
        installed();
        validator.isValid();
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.PULL_EMBEDDING_MODEL);

        installed("nomic-embed-text:latest");
        assertThat(validator.isValid()).isTrue();
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.OK);
    }

    @Test
    void isValid_whenExceptionThrown_returnsFalse() throws IOException {
        when(ollamaModelService.getModels()).thenThrow(new IOException("Connection refused"));

        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getErrorMessage())
                .isEqualTo("Unable to check if embedding model nomic-embed-text is present");
    }

    @Test
    void getCommand_returnsEmbeddingModelType() {
        assertThat(validator.getCommand()).isEqualTo(ValidatorType.EMBEDDING_MODEL);
        assertThat(ValidatorType.EMBEDDING_MODEL.getName()).isEqualTo("embedding model");
    }

    @Test
    void getAction_defaultIsOK() {
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.OK);
    }
}
