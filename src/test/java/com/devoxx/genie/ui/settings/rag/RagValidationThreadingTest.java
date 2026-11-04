package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.service.chromadb.ChromaDBManager;
import com.devoxx.genie.service.rag.RagValidatorService;
import com.devoxx.genie.service.rag.validator.ValidationResult;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TASK-267: validators make network and Docker calls, so the Settings dialog must hand them to a
 * pooled thread. The application is mocked, so nothing runs until the test runs the captured task:
 * validation can only have happened inside the pooled task if it hasn't happened before it.
 */
class RagValidationThreadingTest {

    private final Application application = mock(Application.class);
    private final ModalityState anyModality = mock(ModalityState.class);
    private final RagValidatorService validatorService = mock(RagValidatorService.class);
    private final RAGSettingsComponent settingsComponent = mock(RAGSettingsComponent.class);
    private final JPanel validationPanel = new JPanel();

    private MockedStatic<ApplicationManager> applicationManager;
    private MockedStatic<RagValidatorService> validatorServiceStatic;
    private MockedStatic<ChromaDBManager> chromaDBManager;

    private RAGSettingsHandler handler;

    @BeforeEach
    void setUp() {
        applicationManager = mockStatic(ApplicationManager.class);
        applicationManager.when(ApplicationManager::getApplication).thenReturn(application);
        when(application.getAnyModalityState()).thenReturn(anyModality);
        validatorServiceStatic = mockStatic(RagValidatorService.class);
        validatorServiceStatic.when(RagValidatorService::getInstance).thenReturn(validatorService);
        chromaDBManager = mockStatic(ChromaDBManager.class);
        chromaDBManager.when(() -> ChromaDBManager.getInstance(any())).thenReturn(mock(ChromaDBManager.class));
        when(validatorService.validate()).thenReturn(new ValidationResult(true, List.of()));

        handler = new RAGSettingsHandler(mock(Project.class), validationPanel, settingsComponent);
    }

    @AfterEach
    void tearDown() {
        chromaDBManager.close();
        validatorServiceStatic.close();
        applicationManager.close();
    }

    @Test
    void validationIsHandedToAPooledThread() {
        handler.performValidation();

        verify(validatorService, never()).validate();

        ArgumentCaptor<Runnable> pooledTask = ArgumentCaptor.forClass(Runnable.class);
        verify(application).executeOnPooledThread(pooledTask.capture());
        pooledTask.getValue().run();

        verify(validatorService).validate();
    }

    @Test
    void theResultIsAppliedOnceThroughAModalityAnyUpdate() {
        handler.performValidation();
        ArgumentCaptor<Runnable> pooledTask = ArgumentCaptor.forClass(Runnable.class);
        verify(application).executeOnPooledThread(pooledTask.capture());
        pooledTask.getValue().run();

        verify(settingsComponent, never()).applyValidationResult(any());

        ArgumentCaptor<Runnable> update = ArgumentCaptor.forClass(Runnable.class);
        verify(application).invokeLater(update.capture(), eq(anyModality));
        update.getValue().run();

        verify(settingsComponent).applyValidationResult(any());
        verify(validatorService, times(1)).validate();
    }

    @Test
    void theEmptySectionSaysItIsCheckingWhileValidationRuns() {
        handler.performValidation();

        assertThat(validationPanel.getComponentCount()).isEqualTo(1);
    }

    @Test
    void validatingOnTheEdtIsRejected() throws Exception {
        RagValidatorService realService = new RagValidatorService();

        SwingUtilities.invokeAndWait(() ->
                assertThatThrownBy(realService::validate).hasMessageContaining("EDT"));
    }
}
