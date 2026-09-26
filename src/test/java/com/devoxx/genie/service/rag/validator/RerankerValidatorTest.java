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
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RerankerValidatorTest {

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private OllamaModelService ollamaModelService;

    @Mock
    private Application application;

    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;
    private MockedStatic<OllamaModelService> ollamaModelServiceStatic;
    private MockedStatic<ApplicationManager> appManagerStatic;

    private RerankerValidator validator;

    @BeforeEach
    void setUp() {
        stateServiceStatic = mockStatic(DevoxxGenieStateService.class);
        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
        ollamaModelServiceStatic = mockStatic(OllamaModelService.class);
        ollamaModelServiceStatic.when(OllamaModelService::getInstance).thenReturn(ollamaModelService);
        appManagerStatic = mockStatic(ApplicationManager.class);
        appManagerStatic.when(ApplicationManager::getApplication).thenReturn(application);

        validator = new RerankerValidator();
    }

    @AfterEach
    void tearDown() {
        stateServiceStatic.close();
        ollamaModelServiceStatic.close();
        appManagerStatic.close();
    }

    @Test
    void isValid_whenRerankerDisabled_returnsTrueAsNoOp() {
        when(stateService.getRerankResults()).thenReturn(false);

        assertThat(validator.isValid())
                .as("when the feature toggle is off the validator should not surface a failure")
                .isTrue();
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.OK);
    }

    @Test
    void isValid_whenOllamaUrlUnsetButRerankerOn_returnsFalse() {
        when(stateService.getRerankResults()).thenReturn(true);
        when(stateService.getOllamaModelUrl()).thenReturn(null);

        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Ollama model URL is not set");
    }

    @Test
    void isValid_whenModelNameUnset_returnsFalse() {
        when(stateService.getRerankResults()).thenReturn(true);
        when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:11434");
        when(stateService.getRerankerModelName()).thenReturn("");

        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Reranker model name is not set");
    }

    @Test
    void isValid_whenModelPresent_returnsTrue() throws IOException {
        when(stateService.getRerankResults()).thenReturn(true);
        when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:11434");
        when(stateService.getRerankerModelName()).thenReturn("llama3.2:1b");
        OllamaModelEntryDTO present = new OllamaModelEntryDTO();
        present.setName("llama3.2:1b");
        when(ollamaModelService.getModels()).thenReturn(new OllamaModelEntryDTO[]{present});

        assertThat(validator.isValid()).isTrue();
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.OK);
    }

    @Test
    void isValid_whenModelMissing_returnsFalseWithPullAction() throws IOException {
        when(stateService.getRerankResults()).thenReturn(true);
        when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:11434");
        when(stateService.getRerankerModelName()).thenReturn("llama3.2:1b");
        OllamaModelEntryDTO other = new OllamaModelEntryDTO();
        other.setName("qwen2.5:0.5b");
        when(ollamaModelService.getModels()).thenReturn(new OllamaModelEntryDTO[]{other});

        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.PULL_RERANKER);
        assertThat(validator.getErrorMessage()).contains("llama3.2:1b", "not found");
    }

    @Test
    void isValid_whenOllamaCallThrows_returnsFalse() throws IOException {
        when(stateService.getRerankResults()).thenReturn(true);
        when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:11434");
        when(stateService.getRerankerModelName()).thenReturn("llama3.2:1b");
        when(ollamaModelService.getModels()).thenThrow(new IOException("Connection refused"));

        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Unable to check if reranker model is present");
    }

    @Test
    void getCommand_returnsRerankerType() {
        assertThat(validator.getCommand()).isEqualTo(ValidatorType.RERANKER);
    }
}
