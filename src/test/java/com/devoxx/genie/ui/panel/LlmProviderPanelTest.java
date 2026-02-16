package com.devoxx.genie.ui.panel;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.LLMProviderService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for the logic in LlmProviderPanel.
 * <p>
 * NOTE: We do NOT use @ExtendWith(MockitoExtension.class) because the LLMProviderService
 * class has a static initializer that calls DevoxxGenieStateService.getInstance(),
 * which requires ApplicationManager to be mocked first. Mockito's @Mock annotation
 * triggers class instrumentation BEFORE @BeforeEach, causing initialization errors.
 * Instead, all mocks are created manually in setUp().
 */
class LlmProviderPanelTest {

    private Project project;
    private DevoxxGenieStateService stateService;
    private LLMProviderService llmProviderService;
    private Application application;

    private MockedStatic<ApplicationManager> appManagerMockedStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic;
    private MockedStatic<LLMProviderService> llmProviderServiceMockedStatic;
    private MockedStatic<ChatModelFactoryProvider> factoryProviderMockedStatic;
    private MockedStatic<NotificationUtil> notificationUtilMockedStatic;

    @BeforeEach
    void setUp() {
        // Create simple mocks first (no static initializer issues)
        project = mock(Project.class);
        stateService = mock(DevoxxGenieStateService.class);
        application = mock(Application.class);

        // Static mocks must be set up BEFORE mocking LLMProviderService (instance),
        // because LLMProviderService has a static initializer that calls
        // DevoxxGenieStateService.getInstance() which needs to be mocked.
        appManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        appManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
        lenient().when(application.getService(DevoxxGenieStateService.class)).thenReturn(stateService);

        stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        // NOW it's safe to create the LLMProviderService mock (static initializer will succeed)
        llmProviderService = mock(LLMProviderService.class);

        llmProviderServiceMockedStatic = Mockito.mockStatic(LLMProviderService.class);
        llmProviderServiceMockedStatic.when(LLMProviderService::getInstance).thenReturn(llmProviderService);

        factoryProviderMockedStatic = Mockito.mockStatic(ChatModelFactoryProvider.class);

        notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class);

        lenient().when(project.getLocationHash()).thenReturn("test-hash");

        // Default state service values for provider filtering
        lenient().when(stateService.isOllamaEnabled()).thenReturn(true);
        lenient().when(stateService.isLmStudioEnabled()).thenReturn(false);
        lenient().when(stateService.isGpt4AllEnabled()).thenReturn(false);
        lenient().when(stateService.isJanEnabled()).thenReturn(false);
        lenient().when(stateService.isLlamaCPPEnabled()).thenReturn(false);
        lenient().when(stateService.isCustomOpenAIUrlEnabled()).thenReturn(false);
        lenient().when(stateService.isOpenAIEnabled()).thenReturn(true);
        lenient().when(stateService.isMistralEnabled()).thenReturn(false);
        lenient().when(stateService.isAnthropicEnabled()).thenReturn(true);
        lenient().when(stateService.isGroqEnabled()).thenReturn(false);
        lenient().when(stateService.isDeepInfraEnabled()).thenReturn(false);
        lenient().when(stateService.isGoogleEnabled()).thenReturn(false);
        lenient().when(stateService.isDeepSeekEnabled()).thenReturn(false);
        lenient().when(stateService.isOpenRouterEnabled()).thenReturn(false);
        lenient().when(stateService.isGrokEnabled()).thenReturn(false);
        lenient().when(stateService.isKimiEnabled()).thenReturn(false);
        lenient().when(stateService.isGlmEnabled()).thenReturn(false);
        lenient().when(stateService.isAzureOpenAIEnabled()).thenReturn(false);
        lenient().when(stateService.isAwsEnabled()).thenReturn(false);

        lenient().when(stateService.getSelectedProvider("test-hash")).thenReturn(null);
        lenient().when(stateService.getSelectedLanguageModel("test-hash")).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        notificationUtilMockedStatic.close();
        factoryProviderMockedStatic.close();
        llmProviderServiceMockedStatic.close();
        stateServiceMockedStatic.close();
        appManagerMockedStatic.close();
    }

    @Test
    void testUpdateModelNamesComboBox_WithModels_PopulatesComboBox() {
        List<ModelProvider> providers = List.of(ModelProvider.Ollama, ModelProvider.OpenAI);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        ChatModelFactory factory = mock(ChatModelFactory.class);
        List<LanguageModel> models = List.of(
                LanguageModel.builder().provider(ModelProvider.OpenAI).modelName("gpt-4").displayName("GPT-4").build(),
                LanguageModel.builder().provider(ModelProvider.OpenAI).modelName("gpt-3.5").displayName("GPT-3.5").build()
        );
        when(factory.getModels()).thenReturn(models);

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("OpenAI"))
                .thenReturn(Optional.of(factory));
        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("Ollama"))
                .thenReturn(Optional.empty());

        LlmProviderPanel panel = new LlmProviderPanel(project);

        panel.updateModelNamesComboBox("OpenAI");

        ComboBox<LanguageModel> modelComboBox = panel.getModelNameComboBox();
        assertThat(modelComboBox.getItemCount()).isEqualTo(2);
        assertThat(modelComboBox.isVisible()).isTrue();
    }

    @Test
    void testUpdateModelNamesComboBox_NoModels_HidesComboBox() {
        List<ModelProvider> providers = List.of(ModelProvider.Ollama);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        ChatModelFactory factory = mock(ChatModelFactory.class);
        when(factory.getModels()).thenReturn(Collections.emptyList());

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("Ollama"))
                .thenReturn(Optional.of(factory));

        LlmProviderPanel panel = new LlmProviderPanel(project);

        panel.updateModelNamesComboBox("Ollama");

        assertThat(panel.getModelNameComboBox().isVisible()).isFalse();
    }

    @Test
    void testUpdateModelNamesComboBox_NoFactory_HidesComboBox() {
        List<ModelProvider> providers = List.of(ModelProvider.Ollama);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("SomeProvider"))
                .thenReturn(Optional.empty());
        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("Ollama"))
                .thenReturn(Optional.empty());

        LlmProviderPanel panel = new LlmProviderPanel(project);

        panel.updateModelNamesComboBox("SomeProvider");

        assertThat(panel.getModelNameComboBox().isVisible()).isFalse();
    }

    @Test
    void testUpdateModelNamesComboBox_NullProvider_DoesNothing() {
        List<ModelProvider> providers = List.of(ModelProvider.Ollama);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("Ollama"))
                .thenReturn(Optional.empty());

        LlmProviderPanel panel = new LlmProviderPanel(project);

        // Should not throw
        panel.updateModelNamesComboBox(null);
    }

    @Test
    void testAddModelProvidersToComboBox_FiltersDisabledProviders() {
        List<ModelProvider> allProviders = Arrays.asList(
                ModelProvider.Ollama, ModelProvider.OpenAI, ModelProvider.Anthropic,
                ModelProvider.LMStudio, ModelProvider.GPT4All
        );
        when(llmProviderService.getAvailableModelProviders()).thenReturn(allProviders);

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider(anyString()))
                .thenReturn(Optional.empty());

        LlmProviderPanel panel = new LlmProviderPanel(project);

        ComboBox<ModelProvider> providerComboBox = panel.getModelProviderComboBox();

        boolean hasOllama = false, hasOpenAI = false, hasAnthropic = false;
        boolean hasLMStudio = false, hasGPT4All = false;

        for (int i = 0; i < providerComboBox.getItemCount(); i++) {
            ModelProvider item = providerComboBox.getItemAt(i);
            if (item == ModelProvider.Ollama) hasOllama = true;
            if (item == ModelProvider.OpenAI) hasOpenAI = true;
            if (item == ModelProvider.Anthropic) hasAnthropic = true;
            if (item == ModelProvider.LMStudio) hasLMStudio = true;
            if (item == ModelProvider.GPT4All) hasGPT4All = true;
        }

        assertThat(hasOllama).isTrue();
        assertThat(hasOpenAI).isTrue();
        assertThat(hasAnthropic).isTrue();
        assertThat(hasLMStudio).isFalse();
        assertThat(hasGPT4All).isFalse();
    }

    @Test
    void testAddModelProvidersToComboBox_ProvidersAreSorted() {
        List<ModelProvider> providers = Arrays.asList(
                ModelProvider.Ollama, ModelProvider.OpenAI, ModelProvider.Anthropic
        );
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider(anyString()))
                .thenReturn(Optional.empty());

        LlmProviderPanel panel = new LlmProviderPanel(project);

        ComboBox<ModelProvider> providerComboBox = panel.getModelProviderComboBox();

        String previousName = "";
        for (int i = 0; i < providerComboBox.getItemCount(); i++) {
            String currentName = providerComboBox.getItemAt(i).getName();
            assertThat(currentName.compareToIgnoreCase(previousName)).isGreaterThanOrEqualTo(0);
            previousName = currentName;
        }
    }

    @Test
    void testRestoreLastSelectedProvider_ValidProvider_SelectsIt() {
        when(stateService.getSelectedProvider("test-hash")).thenReturn("OpenAI");

        List<ModelProvider> providers = List.of(ModelProvider.Ollama, ModelProvider.OpenAI);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider(anyString()))
                .thenReturn(Optional.empty());

        LlmProviderPanel panel = new LlmProviderPanel(project);

        ModelProvider selected = (ModelProvider) panel.getModelProviderComboBox().getSelectedItem();
        assertThat(selected).isEqualTo(ModelProvider.OpenAI);
    }

    @Test
    void testRestoreLastSelectedLanguageModel_ValidModel_SelectsIt() {
        when(stateService.getSelectedProvider("test-hash")).thenReturn("OpenAI");
        when(stateService.getSelectedLanguageModel("test-hash")).thenReturn("gpt-4");

        List<ModelProvider> providers = List.of(ModelProvider.OpenAI);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        ChatModelFactory factory = mock(ChatModelFactory.class);
        List<LanguageModel> models = List.of(
                LanguageModel.builder().provider(ModelProvider.OpenAI).modelName("gpt-4").displayName("GPT-4").build(),
                LanguageModel.builder().provider(ModelProvider.OpenAI).modelName("gpt-3.5").displayName("GPT-3.5").build()
        );
        when(factory.getModels()).thenReturn(models);

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("OpenAI"))
                .thenReturn(Optional.of(factory));

        LlmProviderPanel panel = new LlmProviderPanel(project);

        LanguageModel selected = (LanguageModel) panel.getModelNameComboBox().getSelectedItem();
        assertThat(selected).isNotNull();
        assertThat(selected.getModelName()).isEqualTo("gpt-4");
    }

    @Test
    void testSetLastSelectedProvider_SavesFirstProvider() {
        List<ModelProvider> providers = List.of(ModelProvider.Ollama);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider(anyString()))
                .thenReturn(Optional.empty());

        LlmProviderPanel panel = new LlmProviderPanel(project);

        verify(stateService).setSelectedProvider("test-hash", "Ollama");
    }

    @Test
    void testLlmSettingsChanged_UpdatesModelNames() {
        when(stateService.getSelectedProvider("test-hash")).thenReturn("OpenAI");

        List<ModelProvider> providers = List.of(ModelProvider.OpenAI);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        ChatModelFactory factory = mock(ChatModelFactory.class);
        List<LanguageModel> models = List.of(
                LanguageModel.builder().provider(ModelProvider.OpenAI).modelName("gpt-4").displayName("GPT-4").build()
        );
        when(factory.getModels()).thenReturn(models);

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("OpenAI"))
                .thenReturn(Optional.of(factory));

        LlmProviderPanel panel = new LlmProviderPanel(project);

        List<LanguageModel> newModels = List.of(
                LanguageModel.builder().provider(ModelProvider.OpenAI).modelName("gpt-4o").displayName("GPT-4o").build()
        );
        when(factory.getModels()).thenReturn(newModels);

        panel.llmSettingsChanged();

        ComboBox<LanguageModel> modelComboBox = panel.getModelNameComboBox();
        assertThat(modelComboBox.getItemCount()).isEqualTo(1);
    }
}
