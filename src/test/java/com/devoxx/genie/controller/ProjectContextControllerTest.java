package com.devoxx.genie.controller;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.ProjectContentService;
import com.devoxx.genie.ui.panel.ActionButtonsPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectContextControllerTest {

    @Mock
    private Project project;

    @Mock
    private ActionButtonsPanel actionButtonsPanel;

    @Mock
    private Application application;

    private ComboBox<ModelProvider> modelProviderComboBox;
    private ComboBox<LanguageModel> modelNameComboBox;

    private MockedStatic<NotificationUtil> notificationUtilMockedStatic;

    private ProjectContextController controller;

    @BeforeEach
    void setUp() {
        modelProviderComboBox = new ComboBox<>();
        modelNameComboBox = new ComboBox<>();
        notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class);

        controller = new ProjectContextController(project, modelProviderComboBox, modelNameComboBox, actionButtonsPanel);
    }

    @AfterEach
    void tearDown() {
        notificationUtilMockedStatic.close();
    }

    @Test
    void testInitialState_ProjectContextNotAdded() {
        assertThat(controller.isProjectContextAdded()).isFalse();
        assertThat(controller.getProjectContext()).isNull();
    }

    @Test
    void testResetProjectContext_ClearsStateAndNotifies() {
        controller.resetProjectContext();

        assertThat(controller.isProjectContextAdded()).isFalse();
        assertThat(controller.getProjectContext()).isNull();
        verify(actionButtonsPanel).updateAddProjectButton(false);
        verify(actionButtonsPanel).resetTokenUsageBar();
        notificationUtilMockedStatic.verify(
                () -> NotificationUtil.sendNotification(project, "Project context removed successfully")
        );
    }

    @Test
    void testAddProjectContext_NullProvider_SendsNotification() {
        // No provider selected in combo box
        controller.addProjectContext();

        notificationUtilMockedStatic.verify(
                () -> NotificationUtil.sendNotification(project, "Please select a provider first")
        );
    }

    @Test
    void testAddProjectContext_UnsupportedProvider_SendsNotification() {
        modelProviderComboBox.addItem(ModelProvider.GPT4All);
        modelProviderComboBox.setSelectedItem(ModelProvider.GPT4All);

        controller.addProjectContext();

        notificationUtilMockedStatic.verify(
                () -> NotificationUtil.sendNotification(eq(project), contains("only works for"))
        );
    }

    @Test
    void testIsProjectContextSupportedProvider_NullProvider_ReturnsFalse() {
        // No provider selected
        assertThat(controller.isProjectContextSupportedProvider()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ModelProvider.class, names = {
            "Google", "Anthropic", "OpenAI", "Mistral", "DeepSeek",
            "OpenRouter", "DeepInfra", "Ollama", "Jan", "Bedrock",
            "LMStudio", "AzureOpenAI"
    })
    void testIsProjectContextSupportedProvider_SupportedProviders_ReturnsTrue(ModelProvider provider) {
        modelProviderComboBox.addItem(provider);
        modelProviderComboBox.setSelectedItem(provider);

        assertThat(controller.isProjectContextSupportedProvider()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ModelProvider.class, names = {
            "GPT4All", "LLaMA", "CustomOpenAI", "CLIRunners", "ACPRunners",
            "Groq", "Grok", "Kimi", "GLM"
    })
    void testIsProjectContextSupportedProvider_UnsupportedProviders_ReturnsFalse(ModelProvider provider) {
        modelProviderComboBox.addItem(provider);
        modelProviderComboBox.setSelectedItem(provider);

        assertThat(controller.isProjectContextSupportedProvider()).isFalse();
    }

    @Test
    void testResetProjectContext_AfterReset_ProjectContextAddedIsFalse() {
        // Reset context and verify all state is cleared
        controller.resetProjectContext();

        assertThat(controller.isProjectContextAdded()).isFalse();
        assertThat(controller.getProjectContext()).isNull();
    }

    @Test
    void testAddProjectContext_SupportedProvider_DisablesButton() {
        // We need to mock ApplicationManager for the async part
        try (MockedStatic<ApplicationManager> appManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
             MockedStatic<ProjectContentService> projectContentMockedStatic = Mockito.mockStatic(ProjectContentService.class)) {

            appManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);

            ProjectContentService mockService = mock(ProjectContentService.class);
            projectContentMockedStatic.when(ProjectContentService::getInstance).thenReturn(mockService);

            // Set up supported provider
            modelProviderComboBox.addItem(ModelProvider.Google);
            modelProviderComboBox.setSelectedItem(ModelProvider.Google);

            // Set up language model with token limit
            LanguageModel model = LanguageModel.builder()
                    .provider(ModelProvider.Google)
                    .modelName("gemini-pro")
                    .inputMaxTokens(1000000)
                    .build();
            modelNameComboBox.addItem(model);
            modelNameComboBox.setSelectedItem(model);

            // Return a never-completing future to avoid NPE in thenAccept
            java.util.concurrent.CompletableFuture<com.devoxx.genie.model.ScanContentResult> future = new java.util.concurrent.CompletableFuture<>();
            when(mockService.getProjectContent(eq(project), anyInt(), eq(false))).thenReturn(future);

            controller.addProjectContext();

            verify(actionButtonsPanel).setAddProjectButtonEnabled(false);
            verify(actionButtonsPanel).setTokenUsageBarVisible(true);
            verify(actionButtonsPanel).resetTokenUsageBar();
        }
    }

    @Test
    void testGetWindowContext_DefaultValueWhenNoModelSelected() {
        // When no model is selected, window context should default to 4096
        modelProviderComboBox.addItem(ModelProvider.Google);
        modelProviderComboBox.setSelectedItem(ModelProvider.Google);

        // Don't add any model to the model name combo box - it will use default 4096

        try (MockedStatic<ApplicationManager> appManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
             MockedStatic<ProjectContentService> projectContentMockedStatic = Mockito.mockStatic(ProjectContentService.class)) {

            appManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);

            ProjectContentService mockService = mock(ProjectContentService.class);
            projectContentMockedStatic.when(ProjectContentService::getInstance).thenReturn(mockService);

            java.util.concurrent.CompletableFuture<com.devoxx.genie.model.ScanContentResult> future = new java.util.concurrent.CompletableFuture<>();
            when(mockService.getProjectContent(eq(project), eq(4096), eq(false))).thenReturn(future);

            controller.addProjectContext();

            // Verify getProjectContent was called with default tokenLimit of 4096
            verify(mockService).getProjectContent(project, 4096, false);
        }
    }

    @Test
    void testGetWindowContext_UsesModelMaxTokens() {
        modelProviderComboBox.addItem(ModelProvider.OpenAI);
        modelProviderComboBox.setSelectedItem(ModelProvider.OpenAI);

        LanguageModel model = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .inputMaxTokens(128000)
                .build();
        modelNameComboBox.addItem(model);
        modelNameComboBox.setSelectedItem(model);

        try (MockedStatic<ApplicationManager> appManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
             MockedStatic<ProjectContentService> projectContentMockedStatic = Mockito.mockStatic(ProjectContentService.class)) {

            appManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);

            ProjectContentService mockService = mock(ProjectContentService.class);
            projectContentMockedStatic.when(ProjectContentService::getInstance).thenReturn(mockService);

            java.util.concurrent.CompletableFuture<com.devoxx.genie.model.ScanContentResult> future = new java.util.concurrent.CompletableFuture<>();
            when(mockService.getProjectContent(eq(project), eq(128000), eq(false))).thenReturn(future);

            controller.addProjectContext();

            verify(mockService).getProjectContent(project, 128000, false);
        }
    }
}
