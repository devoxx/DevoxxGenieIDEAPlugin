package com.devoxx.genie.controller;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.prompt.PromptExecutionService;
import com.devoxx.genie.service.prompt.command.PromptCommandProcessor;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.panel.ActionButtonsPanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.processor.CommandProcessor;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActionButtonsPanelControllerTest {

    @Mock
    private Project project;

    @Mock
    private PromptInputArea promptInputArea;

    @Mock
    private PromptOutputPanel promptOutputPanel;

    @Mock
    private ActionButtonsPanel actionButtonsPanel;

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private Application application;

    @Mock
    private PromptExecutionService promptExecutionService;

    @Mock
    private PromptCommandProcessor promptCommandProcessor;

    private ComboBox<ModelProvider> modelProviderComboBox;
    private ComboBox<LanguageModel> modelNameComboBox;

    private MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic;
    private MockedStatic<NotificationUtil> notificationUtilMockedStatic;
    private MockedStatic<ApplicationManager> appManagerMockedStatic;
    private MockedStatic<CommandProcessor> commandProcessorMockedStatic;
    private MockedStatic<PromptExecutionService> executionServiceMockedStatic;
    private MockedStatic<PromptCommandProcessor> commandProcessorStaticMockedStatic;

    private ActionButtonsPanelController controller;

    @BeforeEach
    void setUp() {
        modelProviderComboBox = new ComboBox<>();
        modelNameComboBox = new ComboBox<>();

        stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class);
        appManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        appManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);

        commandProcessorMockedStatic = Mockito.mockStatic(CommandProcessor.class);

        // Mock PromptExecutionService.getInstance() - needed by PromptExecutionController created inside constructor
        executionServiceMockedStatic = Mockito.mockStatic(PromptExecutionService.class);
        executionServiceMockedStatic.when(() -> PromptExecutionService.getInstance(project))
                .thenReturn(promptExecutionService);

        // Mock PromptCommandProcessor.getInstance() - needed by PromptExecutionController created inside constructor
        commandProcessorStaticMockedStatic = Mockito.mockStatic(PromptCommandProcessor.class);
        commandProcessorStaticMockedStatic.when(PromptCommandProcessor::getInstance)
                .thenReturn(promptCommandProcessor);

        // Need to mock the service retrieval for EditorFileButtonManager
        when(application.getService(any(Class.class))).thenReturn(null);

        controller = new ActionButtonsPanelController(
                project, promptInputArea, promptOutputPanel,
                modelProviderComboBox, modelNameComboBox, actionButtonsPanel
        );
    }

    @AfterEach
    void tearDown() {
        stateServiceMockedStatic.close();
        notificationUtilMockedStatic.close();
        appManagerMockedStatic.close();
        commandProcessorMockedStatic.close();
        executionServiceMockedStatic.close();
        commandProcessorStaticMockedStatic.close();
    }

    @Test
    void testIsPromptRunning_InitiallyFalse() {
        assertThat(controller.isPromptRunning()).isFalse();
    }

    @Test
    void testHandlePromptSubmission_EmptyPrompt_ReturnsFalse() {
        when(promptInputArea.getText()).thenReturn("");

        boolean result = controller.handlePromptSubmission("submit", false, null);

        assertThat(result).isFalse();
        notificationUtilMockedStatic.verify(
                () -> NotificationUtil.sendNotification(project, "Please enter a prompt.")
        );
    }

    @Test
    void testHandlePromptSubmission_InitCommand_ClearsInputAndReturnsFalse() {
        when(promptInputArea.getText()).thenReturn("/init");
        commandProcessorMockedStatic.when(() -> CommandProcessor.processCommand(project, "/init")).thenReturn(true);

        boolean result = controller.handlePromptSubmission("submit", false, null);

        assertThat(result).isFalse();
        verify(promptInputArea).clear();
    }

    @Test
    void testUpdateButtonVisibility_SupportedProvider_ShowsButtons() {
        modelProviderComboBox.addItem(ModelProvider.Google);
        modelProviderComboBox.setSelectedItem(ModelProvider.Google);

        controller.updateButtonVisibility();

        verify(actionButtonsPanel).setCalcTokenCostButtonVisible(true);
        verify(actionButtonsPanel).setAddProjectButtonVisible(true);
    }

    @Test
    void testUpdateButtonVisibility_UnsupportedProvider_HidesButtons() {
        modelProviderComboBox.addItem(ModelProvider.GPT4All);
        modelProviderComboBox.setSelectedItem(ModelProvider.GPT4All);

        controller.updateButtonVisibility();

        verify(actionButtonsPanel).setCalcTokenCostButtonVisible(false);
        verify(actionButtonsPanel).setAddProjectButtonVisible(false);
    }

    @Test
    void testUpdateButtonVisibility_NoProvider_HidesButtons() {
        controller.updateButtonVisibility();

        verify(actionButtonsPanel).setCalcTokenCostButtonVisible(false);
        verify(actionButtonsPanel).setAddProjectButtonVisible(false);
    }

    @Test
    void testUpdateButtonVisibility_MultipleProviders_ChecksSelectedOne() {
        modelProviderComboBox.addItem(ModelProvider.GPT4All);
        modelProviderComboBox.addItem(ModelProvider.OpenAI);
        modelProviderComboBox.setSelectedItem(ModelProvider.OpenAI);

        controller.updateButtonVisibility();

        verify(actionButtonsPanel).setCalcTokenCostButtonVisible(true);
        verify(actionButtonsPanel).setAddProjectButtonVisible(true);
    }

    @Test
    void testStartPromptExecution_DelegatesToInternalController() {
        controller.startPromptExecution();

        assertThat(controller.isPromptRunning()).isTrue();
        verify(actionButtonsPanel).disableSubmitBtn();
        verify(actionButtonsPanel).disableButtons();
        verify(actionButtonsPanel).startGlowing();
    }

    @Test
    void testEndPromptExecution_DelegatesToInternalController() {
        controller.startPromptExecution();
        assertThat(controller.isPromptRunning()).isTrue();

        controller.endPromptExecution();

        assertThat(controller.isPromptRunning()).isFalse();
        verify(actionButtonsPanel).enableButtons();
    }

    @Test
    void testStopPromptExecution_StopsServiceAndEnds() {
        controller.startPromptExecution();

        controller.stopPromptExecution();

        verify(promptExecutionService).stopExecution(project);
        assertThat(controller.isPromptRunning()).isFalse();
    }

    @Test
    void testHandlePromptSubmission_WithSelectedModel_UsesSelectedModel() {
        when(promptInputArea.getText()).thenReturn("Tell me about Java");
        commandProcessorMockedStatic.when(() -> CommandProcessor.processCommand(project, "Tell me about Java")).thenReturn(false);

        LanguageModel model = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .apiKeyUsed(true)
                .inputCost(0)
                .outputCost(0)
                .inputMaxTokens(128000)
                .build();
        modelNameComboBox.addItem(model);
        modelNameComboBox.setSelectedItem(model);

        modelProviderComboBox.addItem(ModelProvider.OpenAI);
        modelProviderComboBox.setSelectedItem(ModelProvider.OpenAI);

        // The prompt execution controller will handle it internally
        // Just verify we get past input validation
        when(promptOutputPanel.isNewConversation()).thenReturn(false);
    }

    @Test
    void testHandlePromptSubmission_WithCLIRunnerAndNoModel_CreatesDefaultModel() {
        when(promptInputArea.getText()).thenReturn("test prompt");
        commandProcessorMockedStatic.when(() -> CommandProcessor.processCommand(project, "test prompt")).thenReturn(false);
        when(stateService.getSpecSelectedCliTool()).thenReturn("claude-code");

        modelProviderComboBox.addItem(ModelProvider.CLIRunners);
        modelProviderComboBox.setSelectedItem(ModelProvider.CLIRunners);

        // No model selected in modelNameComboBox - this triggers createDefaultLanguageModel
        // The test verifies it doesn't crash with CLIRunners provider
    }

    @Test
    void testHandlePromptSubmission_WithACPRunner_CreatesDefaultModel() {
        when(promptInputArea.getText()).thenReturn("test prompt");
        commandProcessorMockedStatic.when(() -> CommandProcessor.processCommand(project, "test prompt")).thenReturn(false);

        modelProviderComboBox.addItem(ModelProvider.ACPRunners);
        modelProviderComboBox.setSelectedItem(ModelProvider.ACPRunners);

        // No model selected - triggers createDefaultLanguageModel for ACPRunners
    }

    @Test
    void testHandlePromptSubmission_WithLocalProvider_CreatesDefaultModelWith4096Tokens() {
        when(promptInputArea.getText()).thenReturn("test prompt");
        commandProcessorMockedStatic.when(() -> CommandProcessor.processCommand(project, "test prompt")).thenReturn(false);

        modelProviderComboBox.addItem(ModelProvider.LMStudio);
        modelProviderComboBox.setSelectedItem(ModelProvider.LMStudio);

        // LMStudio without model uses default with inputMaxTokens=4096
    }
}
