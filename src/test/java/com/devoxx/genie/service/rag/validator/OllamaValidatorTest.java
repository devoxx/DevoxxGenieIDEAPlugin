package com.devoxx.genie.service.rag.validator;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OllamaValidatorTest {

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private Application application;

    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;
    private MockedStatic<ApplicationManager> appManagerStatic;

    private OllamaValidator validator;

    @BeforeEach
    void setUp() {
        stateServiceStatic = mockStatic(DevoxxGenieStateService.class);
        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        appManagerStatic = mockStatic(ApplicationManager.class);
        appManagerStatic.when(ApplicationManager::getApplication).thenReturn(application);

        validator = new OllamaValidator();
    }

    @AfterEach
    void tearDown() {
        stateServiceStatic.close();
        appManagerStatic.close();
    }

    @Test
    void isValid_whenOllamaUrlIsNull_returnsFalse() {
        when(stateService.getOllamaModelUrl()).thenReturn(null);

        boolean result = validator.isValid();

        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenOllamaUrlIsEmpty_returnsFalse() {
        when(stateService.getOllamaModelUrl()).thenReturn("");

        boolean result = validator.isValid();

        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenOllamaUrlIsInvalid_returnsFalse() {
        when(stateService.getOllamaModelUrl()).thenReturn("not-a-valid-url");

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Ollama is not running, please start Ollama");
    }

    @Test
    void isValid_whenConnectionRefused_returnsFalseWithNotRunningMessage() {
        when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:99999");

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Ollama is not running, please start Ollama");
    }

    @Test
    void getMessage_returnsOllamaRunningMessage() {
        assertThat(validator.getMessage()).isEqualTo("Ollama is running");
    }

    @Test
    void getCommand_returnsOllamaType() {
        assertThat(validator.getCommand()).isEqualTo(ValidatorType.OLLAMA);
    }

    @Test
    void getErrorMessage_beforeValidation_returnsNull() {
        assertThat(validator.getErrorMessage()).isNull();
    }

    @Test
    void getAction_returnsDefaultOK() {
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.OK);
    }
}
