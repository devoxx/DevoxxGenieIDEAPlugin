package com.devoxx.genie.controller;

import com.devoxx.genie.controller.listener.TokenCalculationListener;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.ProjectContentService;
import com.devoxx.genie.service.models.LLMModelRegistryService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenCalculationControllerTest {

    @Mock
    private Project project;

    @Mock
    private TokenCalculationListener listener;

    @Mock
    private Application application;

    @Mock
    private ProjectContentService projectContentService;

    @Mock
    private LLMModelRegistryService llmModelRegistryService;

    private ComboBox<ModelProvider> modelProviderComboBox;
    private ComboBox<LanguageModel> modelNameComboBox;

    private MockedStatic<NotificationUtil> notificationUtilMockedStatic;
    private MockedStatic<ApplicationManager> appManagerMockedStatic;
    private MockedStatic<ProjectContentService> projectContentServiceMockedStatic;
    private MockedStatic<LLMModelRegistryService> llmModelRegistryServiceMockedStatic;

    private TokenCalculationController controller;

    @BeforeEach
    void setUp() {
        modelProviderComboBox = new ComboBox<>();
        modelNameComboBox = new ComboBox<>();

        notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class);

        appManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        appManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);

        projectContentServiceMockedStatic = Mockito.mockStatic(ProjectContentService.class);
        projectContentServiceMockedStatic.when(ProjectContentService::getInstance).thenReturn(projectContentService);

        llmModelRegistryServiceMockedStatic = Mockito.mockStatic(LLMModelRegistryService.class);
        llmModelRegistryServiceMockedStatic.when(LLMModelRegistryService::getInstance).thenReturn(llmModelRegistryService);
        when(llmModelRegistryService.getModels()).thenReturn(java.util.Collections.emptyList());

        controller = new TokenCalculationController(project, modelProviderComboBox, modelNameComboBox, listener);
    }

    @AfterEach
    void tearDown() {
        notificationUtilMockedStatic.close();
        appManagerMockedStatic.close();
        projectContentServiceMockedStatic.close();
        llmModelRegistryServiceMockedStatic.close();
    }

    @Test
    void testCalculateTokensAndCost_NullModel_NotifiesUser() {
        // Add provider but no model
        modelProviderComboBox.addItem(ModelProvider.OpenAI);
        modelProviderComboBox.setSelectedItem(ModelProvider.OpenAI);

        controller.calculateTokensAndCost();

        notificationUtilMockedStatic.verify(
                () -> NotificationUtil.sendNotification(project, "Please select a model first")
        );
    }

    @Test
    void testCalculateTokensAndCost_NullProvider_NotifiesUser() {
        // Add model but no provider
        LanguageModel model = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .inputMaxTokens(128000)
                .build();
        modelNameComboBox.addItem(model);
        modelNameComboBox.setSelectedItem(model);

        controller.calculateTokensAndCost();

        notificationUtilMockedStatic.verify(
                () -> NotificationUtil.sendNotification(project, "Please select a provider first")
        );
    }

    @Test
    void testCalculateTokensAndCost_BothNull_NotifiesModel() {
        // Neither model nor provider selected
        controller.calculateTokensAndCost();

        // When both are null, model is checked first in the code
        notificationUtilMockedStatic.verify(
                () -> NotificationUtil.sendNotification(project, "Please select a model first")
        );
    }

    @Test
    void testCalculateTokensAndCost_BothSelected_ProceedsWithCalculation() {
        modelProviderComboBox.addItem(ModelProvider.OpenAI);
        modelProviderComboBox.setSelectedItem(ModelProvider.OpenAI);

        LanguageModel model = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .inputMaxTokens(128000)
                .build();
        modelNameComboBox.addItem(model);
        modelNameComboBox.setSelectedItem(model);

        controller.calculateTokensAndCost();

        // Should NOT send model/provider selection notifications - those are only for null selections
        notificationUtilMockedStatic.verify(
                () -> NotificationUtil.sendNotification(project, "Please select a model first"),
                never()
        );
        notificationUtilMockedStatic.verify(
                () -> NotificationUtil.sendNotification(project, "Please select a provider first"),
                never()
        );
    }
}
