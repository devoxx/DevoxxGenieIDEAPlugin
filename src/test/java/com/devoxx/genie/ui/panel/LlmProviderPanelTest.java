package com.devoxx.genie.ui.panel;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.chatmodel.local.customopenai.CustomOpenAIContextWindow;
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
import static org.mockito.ArgumentMatchers.any;
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

        // Model loading is now dispatched off the EDT via executeOnPooledThread and applied back
        // via invokeLater. In tests we run both inline so the (otherwise asynchronous) combo
        // population completes synchronously and the assertions below remain deterministic.
        lenient().when(application.executeOnPooledThread(any(Runnable.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        });
        lenient().doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(application).invokeLater(any(Runnable.class));

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
        lenient().when(stateService.isExoEnabled()).thenReturn(false);
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
    void isUpdatingModelNames_isTrueDuringUpdateAndFalseAfter() {
        // Regression for task-206: model_selected analytics must be suppressible during
        // programmatic combo repopulation. The flag must be true while updateModelNamesComboBox
        // runs and must reset to false on completion.
        List<ModelProvider> providers = List.of(ModelProvider.Ollama, ModelProvider.OpenAI);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        ChatModelFactory factory = mock(ChatModelFactory.class);
        when(factory.getModels()).thenReturn(List.of(
                LanguageModel.builder().provider(ModelProvider.OpenAI)
                        .modelName("gpt-4").displayName("GPT-4").build()));

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("OpenAI"))
                .thenReturn(Optional.of(factory));
        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("Ollama"))
                .thenReturn(Optional.empty());

        LlmProviderPanel panel = new LlmProviderPanel(project);

        // The network fetch now runs off the EDT (without the flag); the flag must be true while
        // the combo items are added on the EDT — that is when the selection ActionEvents (which
        // drive model_selected analytics) fire and must be suppressed. Capture the flag at that point.
        boolean[] flagDuringPopulate = new boolean[]{false};
        panel.getModelNameComboBox().addActionListener(e ->
                flagDuringPopulate[0] = flagDuringPopulate[0] || panel.isUpdatingModelNames());

        assertThat(panel.isUpdatingModelNames())
                .as("Flag is false before any programmatic update")
                .isFalse();

        panel.updateModelNamesComboBox("OpenAI");

        assertThat(flagDuringPopulate[0])
                .as("Flag must be true while combo is being repopulated")
                .isTrue();
        assertThat(panel.isUpdatingModelNames())
                .as("Flag must reset to false after updateModelNamesComboBox completes")
                .isFalse();
    }

    @Test
    void updateModelNamesComboBox_fetchesModelsOffTheEdt() {
        // Regression for the CustomOpenAI freeze: getModels() can perform blocking network I/O,
        // so it must be dispatched to a background thread (executeOnPooledThread) rather than
        // called directly on the EDT while the provider combo selection is being handled.
        List<ModelProvider> providers = List.of(ModelProvider.OpenAI);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        ChatModelFactory factory = mock(ChatModelFactory.class);
        boolean[] fetchedViaPool = new boolean[]{false};
        // The pooled-thread stub records that the fetch runnable actually ran through it.
        when(application.executeOnPooledThread(any(Runnable.class))).thenAnswer(invocation -> {
            fetchedViaPool[0] = true;
            invocation.getArgument(0, Runnable.class).run();
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        });
        when(factory.getModels()).thenReturn(List.of(
                LanguageModel.builder().provider(ModelProvider.OpenAI)
                        .modelName("gpt-4").displayName("GPT-4").build()));

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("OpenAI"))
                .thenReturn(Optional.of(factory));

        LlmProviderPanel panel = new LlmProviderPanel(project);
        fetchedViaPool[0] = false; // ignore any construction-time loading

        panel.updateModelNamesComboBox("OpenAI");

        assertThat(fetchedViaPool[0])
                .as("Model fetch must be dispatched off the EDT")
                .isTrue();
        verify(factory, atLeastOnce()).getModels();
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
    void restoresCustomOpenAiModelAfterRestart() {
        // Reproduction for "CustomOpenAI selected model not restored after restart":
        // when the provider and model were persisted and the /models probe returns the
        // saved model, it must be re-selected on construction.
        when(stateService.isCustomOpenAIUrlEnabled()).thenReturn(true);
        when(stateService.getSelectedProvider("test-hash")).thenReturn("CustomOpenAI");
        when(stateService.getSelectedLanguageModel("test-hash")).thenReturn("meta/llama-3.1-70b");

        List<ModelProvider> providers = List.of(ModelProvider.CustomOpenAI);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        ChatModelFactory factory = mock(ChatModelFactory.class);
        when(factory.getModels()).thenReturn(List.of(
                LanguageModel.builder().provider(ModelProvider.CustomOpenAI)
                        .modelName("meta/llama-3.1-8b").displayName("meta/llama-3.1-8b").build(),
                LanguageModel.builder().provider(ModelProvider.CustomOpenAI)
                        .modelName("meta/llama-3.1-70b").displayName("meta/llama-3.1-70b").build()));

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("CustomOpenAI"))
                .thenReturn(Optional.of(factory));

        LlmProviderPanel panel = new LlmProviderPanel(project);

        LanguageModel selected = (LanguageModel) panel.getModelNameComboBox().getSelectedItem();
        assertThat(selected).isNotNull();
        assertThat(selected.getModelName()).isEqualTo("meta/llama-3.1-70b");
    }

    @Test
    void restoresCustomOpenAiModelEvenWhenNotInFetchedList() {
        // The /models endpoint may be slow/unavailable at startup or may not enumerate the
        // chosen model. The persisted selection must still be restored (re-added and selected).
        when(stateService.isCustomOpenAIUrlEnabled()).thenReturn(true);
        when(stateService.getSelectedProvider("test-hash")).thenReturn("CustomOpenAI");
        when(stateService.getSelectedLanguageModel("test-hash")).thenReturn("meta/llama-3.1-70b");

        List<ModelProvider> providers = List.of(ModelProvider.CustomOpenAI);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        ChatModelFactory factory = mock(ChatModelFactory.class);
        // Endpoint returns nothing (cold start / unreachable).
        when(factory.getModels()).thenReturn(Collections.emptyList());

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("CustomOpenAI"))
                .thenReturn(Optional.of(factory));

        LlmProviderPanel panel = new LlmProviderPanel(project);

        LanguageModel selected = (LanguageModel) panel.getModelNameComboBox().getSelectedItem();
        assertThat(selected).isNotNull();
        assertThat(selected.getModelName()).isEqualTo("meta/llama-3.1-70b");
        assertThat(selected.getProvider()).isEqualTo(ModelProvider.CustomOpenAI);
    }

    @Test
    void doesNotRestoreForeignProviderModelWhenProviderListIsHealthy() {
        // Reproduction for "Ollama provider restored with an Anthropic model selected":
        // corrupted/stale state can persist a model name that belongs to another provider
        // (e.g. a Claude model under the Ollama key). When the provider returns a healthy,
        // non-empty model list that does not contain the saved name, the panel must NOT
        // synthesise the foreign model but fall back to the first available model.
        when(stateService.getSelectedProvider("test-hash")).thenReturn("Ollama");
        when(stateService.getSelectedLanguageModel("test-hash")).thenReturn("claude-sonnet-4-20250514");

        List<ModelProvider> providers = List.of(ModelProvider.Ollama);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        ChatModelFactory factory = mock(ChatModelFactory.class);
        when(factory.getModels()).thenReturn(List.of(
                LanguageModel.builder().provider(ModelProvider.Ollama)
                        .modelName("llama3.2:latest").displayName("llama3.2:latest").build(),
                LanguageModel.builder().provider(ModelProvider.Ollama)
                        .modelName("qwen2.5-coder").displayName("qwen2.5-coder").build()));

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("Ollama"))
                .thenReturn(Optional.of(factory));

        LlmProviderPanel panel = new LlmProviderPanel(project);

        LanguageModel selected = (LanguageModel) panel.getModelNameComboBox().getSelectedItem();
        assertThat(selected).isNotNull();
        assertThat(selected.getModelName())
                .as("A model from the fetched Ollama list must be selected, not the foreign persisted one")
                .isIn("llama3.2:latest", "qwen2.5-coder");

        for (int i = 0; i < panel.getModelNameComboBox().getItemCount(); i++) {
            assertThat(panel.getModelNameComboBox().getItemAt(i).getModelName())
                    .as("The foreign model must not be synthesised into the combo")
                    .isNotEqualTo("claude-sonnet-4-20250514");
        }

        // The corrected selection must be persisted so the corrupted state heals itself.
        verify(stateService).setSelectedLanguageModel(eq("test-hash"), eq(selected.getModelName()));
    }

    @Test
    void restoresCustomOpenAiModelNotInNonEmptyFetchedList() {
        // The CustomOpenAI /models endpoint may not enumerate the chosen model even when it
        // returns other models. For this provider the persisted selection must still be
        // synthesised and selected (original fix for the CustomOpenAI restore bug).
        when(stateService.isCustomOpenAIUrlEnabled()).thenReturn(true);
        when(stateService.getSelectedProvider("test-hash")).thenReturn("CustomOpenAI");
        when(stateService.getSelectedLanguageModel("test-hash")).thenReturn("meta/llama-3.1-70b");

        List<ModelProvider> providers = List.of(ModelProvider.CustomOpenAI);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        ChatModelFactory factory = mock(ChatModelFactory.class);
        when(factory.getModels()).thenReturn(List.of(
                LanguageModel.builder().provider(ModelProvider.CustomOpenAI)
                        .modelName("meta/llama-3.1-8b").displayName("meta/llama-3.1-8b").build()));

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("CustomOpenAI"))
                .thenReturn(Optional.of(factory));

        LlmProviderPanel panel = new LlmProviderPanel(project);

        LanguageModel selected = (LanguageModel) panel.getModelNameComboBox().getSelectedItem();
        assertThat(selected).isNotNull();
        assertThat(selected.getModelName()).isEqualTo("meta/llama-3.1-70b");
    }

    @Test
    void providerSwitchPersistsProviderAndModelAsPair() {
        // Switching provider must persist the provider AND the resulting model selection
        // together. Persisting only the provider leaves the previous provider's model in
        // state, which later restores a foreign model (Anthropic model under Ollama).
        List<ModelProvider> providers = List.of(ModelProvider.Ollama, ModelProvider.OpenAI);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        ChatModelFactory openAiFactory = mock(ChatModelFactory.class);
        when(openAiFactory.getModels()).thenReturn(List.of(
                LanguageModel.builder().provider(ModelProvider.OpenAI)
                        .modelName("gpt-4").displayName("GPT-4").build()));

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("Ollama"))
                .thenReturn(Optional.empty());
        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("OpenAI"))
                .thenReturn(Optional.of(openAiFactory));

        LlmProviderPanel panel = new LlmProviderPanel(project);

        panel.getModelProviderComboBox().setSelectedItem(ModelProvider.OpenAI);

        verify(stateService).setSelectedProvider("test-hash", "OpenAI");
        verify(stateService).setSelectedLanguageModel("test-hash", "gpt-4");
    }

    @Test
    void providerSwitchWithoutModelsClearsPersistedModel() {
        // When the newly selected provider yields no models, the stale model of the previous
        // provider must be cleared from state instead of surviving under the new provider.
        List<ModelProvider> providers = List.of(ModelProvider.Ollama, ModelProvider.OpenAI);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        ChatModelFactory ollamaFactory = mock(ChatModelFactory.class);
        when(ollamaFactory.getModels()).thenReturn(Collections.emptyList());

        ChatModelFactory openAiFactory = mock(ChatModelFactory.class);
        when(openAiFactory.getModels()).thenReturn(List.of(
                LanguageModel.builder().provider(ModelProvider.OpenAI)
                        .modelName("gpt-4").displayName("GPT-4").build()));

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("Ollama"))
                .thenReturn(Optional.of(ollamaFactory));
        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("OpenAI"))
                .thenReturn(Optional.of(openAiFactory));

        // Start on OpenAI, then switch to Ollama which is unreachable (no models).
        when(stateService.getSelectedProvider("test-hash")).thenReturn("OpenAI");
        when(stateService.getSelectedLanguageModel("test-hash")).thenReturn("gpt-4");

        LlmProviderPanel panel = new LlmProviderPanel(project);

        panel.getModelProviderComboBox().setSelectedItem(ModelProvider.Ollama);

        verify(stateService).setSelectedProvider("test-hash", "Ollama");
        verify(stateService).setSelectedLanguageModel("test-hash", "");
    }

    @Test
    void staleModelFetchDoesNotOverwriteNewerSelection() {
        // Two concurrent updateModelNamesComboBox calls for different providers may complete
        // out of order (e.g. a slow Ollama fetch vs. an instant cloud list). Only the models
        // of the LATEST request may be applied to the combo; a stale response must be dropped.
        List<ModelProvider> providers = List.of(ModelProvider.Ollama, ModelProvider.OpenAI);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(providers);

        ChatModelFactory openAiFactory = mock(ChatModelFactory.class);
        when(openAiFactory.getModels()).thenReturn(List.of(
                LanguageModel.builder().provider(ModelProvider.OpenAI)
                        .modelName("gpt-4").displayName("GPT-4").build()));
        ChatModelFactory ollamaFactory = mock(ChatModelFactory.class);
        when(ollamaFactory.getModels()).thenReturn(List.of(
                LanguageModel.builder().provider(ModelProvider.Ollama)
                        .modelName("llama3.2:latest").displayName("llama3.2:latest").build()));

        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("OpenAI"))
                .thenReturn(Optional.of(openAiFactory));
        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("Ollama"))
                .thenReturn(Optional.of(ollamaFactory));

        LlmProviderPanel panel = new LlmProviderPanel(project);

        // From here on, queue background work instead of running it inline so the two
        // fetches can be completed out of order.
        List<Runnable> queued = new java.util.ArrayList<>();
        when(application.executeOnPooledThread(any(Runnable.class))).thenAnswer(invocation -> {
            queued.add(invocation.getArgument(0, Runnable.class));
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        });

        panel.updateModelNamesComboBox("OpenAI");   // request #1 (stale)
        panel.updateModelNamesComboBox("Ollama");   // request #2 (latest)

        assertThat(queued).hasSize(2);
        queued.get(1).run();  // latest completes first
        queued.get(0).run();  // stale completes last — must be dropped

        assertThat(panel.getModelNameComboBox().getItemCount()).isEqualTo(1);
        assertThat(panel.getModelNameComboBox().getItemAt(0).getModelName())
                .as("The combo must show the models of the latest request")
                .isEqualTo("llama3.2:latest");
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

    @Test
    void reselectModelKeepsTheUserSelectionAfterAContextWindowChange() {
        // End-to-end shape of the reported bug: the user selected the second model, then raised the
        // Custom OpenAI context window. Applying settings repopulates the combo with refreshed
        // models; the previously selected instance still carries the old 4096 window, so it no
        // longer equals its counterpart. Selection must stay on the user's model, with 262000.
        when(stateService.isCustomOpenAIUrlEnabled()).thenReturn(true);
        when(stateService.getSelectedProvider("test-hash")).thenReturn("CustomOpenAI");
        when(llmProviderService.getAvailableModelProviders()).thenReturn(List.of(ModelProvider.CustomOpenAI));

        ChatModelFactory factory = mock(ChatModelFactory.class);
        when(factory.getModels()).thenReturn(List.of(
                LanguageModel.builder().provider(ModelProvider.CustomOpenAI)
                        .modelName("model-a").displayName("model-a").inputMaxTokens(262_000).build(),
                LanguageModel.builder().provider(ModelProvider.CustomOpenAI)
                        .modelName("model-b").displayName("model-b").inputMaxTokens(262_000).build()));
        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("CustomOpenAI"))
                .thenReturn(Optional.of(factory));

        LlmProviderPanel panel = new LlmProviderPanel(project);

        LanguageModel staleSelection = LanguageModel.builder().provider(ModelProvider.CustomOpenAI)
                .modelName("model-b").displayName("model-b").inputMaxTokens(4096).build();

        panel.reselectModel(staleSelection);

        LanguageModel selected = (LanguageModel) panel.getModelNameComboBox().getSelectedItem();
        assertThat(selected).isNotNull();
        assertThat(selected.getModelName()).isEqualTo("model-b");
        assertThat(selected.getInputMaxTokens()).isEqualTo(262_000);
    }

    @Test
    void reselectModelReAddsACustomOpenAIModelTheEndpointDoesNotEnumerate() {
        when(stateService.isCustomOpenAIUrlEnabled()).thenReturn(true);
        when(stateService.getSelectedProvider("test-hash")).thenReturn("CustomOpenAI");
        when(stateService.getCustomOpenAIContextWindow()).thenReturn(262_000);
        when(llmProviderService.getAvailableModelProviders()).thenReturn(List.of(ModelProvider.CustomOpenAI));

        ChatModelFactory factory = mock(ChatModelFactory.class);
        when(factory.getModels()).thenReturn(List.of(
                LanguageModel.builder().provider(ModelProvider.CustomOpenAI)
                        .modelName("model-a").displayName("model-a").inputMaxTokens(262_000).build()));
        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("CustomOpenAI"))
                .thenReturn(Optional.of(factory));

        LlmProviderPanel panel = new LlmProviderPanel(project);

        LanguageModel privateModel = LanguageModel.builder().provider(ModelProvider.CustomOpenAI)
                .modelName("private-model").displayName("private-model").inputMaxTokens(4096).build();

        panel.reselectModel(privateModel);

        LanguageModel selected = (LanguageModel) panel.getModelNameComboBox().getSelectedItem();
        assertThat(selected).isNotNull();
        assertThat(selected.getModelName()).isEqualTo("private-model");
        assertThat(selected.getInputMaxTokens()).isEqualTo(262_000);
    }

    @Test
    void synthesizedCustomOpenAIModelCarriesTheConfiguredContextWindow() {
        // A Custom OpenAI endpoint need not enumerate the configured model, in which case the
        // persisted selection is synthesised. It previously defaulted inputMaxTokens to 0, which
        // hid the conversation context indicator instead of using the configured window.
        when(stateService.getCustomOpenAIContextWindow()).thenReturn(262_000);
        when(stateService.getCustomOpenAIInputCost()).thenReturn(1.5);
        when(stateService.getCustomOpenAIOutputCost()).thenReturn(2.5);

        LanguageModel restored =
                LlmProviderPanel.synthesizePersistedModel(ModelProvider.CustomOpenAI, "private-model");

        assertThat(restored.getModelName()).isEqualTo("private-model");
        assertThat(restored.getInputMaxTokens()).isEqualTo(262_000);
        assertThat(restored.getInputCost()).isEqualTo(1.5);
        assertThat(restored.getOutputCost()).isEqualTo(2.5);
    }

    @Test
    void synthesizedCustomOpenAIModelFallsBackToTheDefaultContextWindow() {
        when(stateService.getCustomOpenAIContextWindow()).thenReturn(null);

        LanguageModel restored =
                LlmProviderPanel.synthesizePersistedModel(ModelProvider.CustomOpenAI, "private-model");

        assertThat(restored.getInputMaxTokens()).isEqualTo(CustomOpenAIContextWindow.DEFAULT_CONTEXT_WINDOW);
    }

    @Test
    void synthesizedModelForOtherProvidersIsUnchanged() {
        // Only Custom OpenAI derives its window from settings; other providers keep the minimal
        // stand-in so no bogus window is invented for them.
        LanguageModel restored =
                LlmProviderPanel.synthesizePersistedModel(ModelProvider.Ollama, "llama3");

        assertThat(restored.getProvider()).isEqualTo(ModelProvider.Ollama);
        assertThat(restored.getModelName()).isEqualTo("llama3");
        assertThat(restored.getInputMaxTokens()).isZero();
    }
}
