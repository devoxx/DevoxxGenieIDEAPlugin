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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NomicEmbedTextValidatorTest {

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private OllamaModelService ollamaModelService;

    @Mock
    private Application application;

    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;
    private MockedStatic<OllamaModelService> ollamaModelServiceStatic;
    private MockedStatic<ApplicationManager> appManagerStatic;

    private NomicEmbedTextValidator validator;

    @BeforeEach
    void setUp() {
        stateServiceStatic = mockStatic(DevoxxGenieStateService.class);
        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        ollamaModelServiceStatic = mockStatic(OllamaModelService.class);
        ollamaModelServiceStatic.when(OllamaModelService::getInstance).thenReturn(ollamaModelService);

        appManagerStatic = mockStatic(ApplicationManager.class);
        appManagerStatic.when(ApplicationManager::getApplication).thenReturn(application);

        validator = new NomicEmbedTextValidator();
    }

    @AfterEach
    void tearDown() {
        stateServiceStatic.close();
        ollamaModelServiceStatic.close();
        appManagerStatic.close();
    }

    @Test
    void isValid_whenOllamaUrlIsNull_returnsFalse() {
        when(stateService.getOllamaModelUrl()).thenReturn(null);

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Ollama model URL is not set");
    }

    @Test
    void isValid_whenOllamaUrlIsEmpty_returnsFalse() {
        when(stateService.getOllamaModelUrl()).thenReturn("");

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Ollama model URL is not set");
    }

    @Test
    void isValid_whenModelsIsNull_returnsFalse() throws IOException {
        when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:11434");
        when(ollamaModelService.getModels()).thenReturn(null);

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Unable to check if Nomic Embed model is present");
    }

    @Test
    void isValid_whenNomicModelFound_returnsTrue() throws IOException {
        when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:11434");
        OllamaModelEntryDTO nomicModel = new OllamaModelEntryDTO();
        nomicModel.setName("nomic-embed-text:latest");
        when(ollamaModelService.getModels()).thenReturn(new OllamaModelEntryDTO[]{nomicModel});

        boolean result = validator.isValid();

        assertThat(result).isTrue();
        assertThat(validator.getMessage()).isEqualTo("Nomic Embed model found");
    }

    @Test
    void isValid_whenNomicModelNotFound_returnsFalseWithPullAction() throws IOException {
        when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:11434");
        OllamaModelEntryDTO otherModel = new OllamaModelEntryDTO();
        otherModel.setName("llama3:latest");
        when(ollamaModelService.getModels()).thenReturn(new OllamaModelEntryDTO[]{otherModel});

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Nomic Embed model not found");
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.PULL_NOMIC);
    }

    @Test
    void isValid_whenExceptionThrown_returnsFalse() throws IOException {
        when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:11434");
        when(ollamaModelService.getModels()).thenThrow(new IOException("Connection refused"));

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Unable to check if Nomic Embed model is present");
    }

    @Test
    void isValid_whenEmptyModelsArray_returnsFalseWithPullAction() throws IOException {
        when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:11434");
        when(ollamaModelService.getModels()).thenReturn(new OllamaModelEntryDTO[]{});

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Nomic Embed model not found");
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.PULL_NOMIC);
    }

    @Test
    void getCommand_returnsNomicType() {
        assertThat(validator.getCommand()).isEqualTo(ValidatorType.NOMIC);
    }

    @Test
    void getAction_defaultIsOK() {
        // Before validation, action should default to OK
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.OK);
    }

    @Test
    void isValid_whenNomicModelNameStartsWithPrefix_returnsTrue() throws IOException {
        when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:11434");
        OllamaModelEntryDTO nomicModel = new OllamaModelEntryDTO();
        nomicModel.setName("nomic-embed-text-v1.5");
        when(ollamaModelService.getModels()).thenReturn(new OllamaModelEntryDTO[]{nomicModel});

        boolean result = validator.isValid();

        assertThat(result).isTrue();
    }
}
