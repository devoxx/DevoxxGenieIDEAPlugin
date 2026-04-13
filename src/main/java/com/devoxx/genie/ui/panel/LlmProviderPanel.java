package com.devoxx.genie.ui.panel;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.chatmodel.local.exo.ExoChatModelFactory;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.LLMProviderService;
import com.devoxx.genie.service.models.ModelConfigService;
import com.devoxx.genie.ui.listener.LLMSettingsChangeListener;
import com.devoxx.genie.ui.renderer.ModelInfoRenderer;
import com.devoxx.genie.ui.renderer.ModelProviderRenderer;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.DevoxxGenieFontsUtil;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.RefreshIcon;

@Slf4j
public class LlmProviderPanel extends JBPanel<LlmProviderPanel> implements LLMSettingsChangeListener {

    private final transient Project project;
    // Composite key for per-tab provider/model persistence (projectHash or projectHash-tabId)
    private final String stateKey;

    @Getter
    private final JPanel contentPanel = new JPanel();
    @Getter
    private final ComboBox<ModelProvider> modelProviderComboBox = new ComboBox<>();
    @Getter
    private final ComboBox<LanguageModel> modelNameComboBox = new ComboBox<>();

    private JButton refreshButton;

    private String lastSelectedProvider = null;
    private String lastSelectedLanguageModel = null;

    private boolean isInitializationComplete = false;
    private boolean isUpdatingModelNames = false;

    /**
     * @return {@code true} while the model name combo is being repopulated programmatically
     * (provider switch, settings refresh, restore). Listeners that distinguish user actions
     * from programmatic updates can read this to suppress side-effects (e.g. analytics).
     */
    public boolean isUpdatingModelNames() {
        return isUpdatingModelNames;
    }

    public LlmProviderPanel(@NotNull Project project) {
        this(project, null);
    }

    /**
     * The conversation panel constructor.
     *
     * @param project the project instance
     * @param tabId   the tab identifier for per-tab persistence (null for legacy behavior)
     */
    public LlmProviderPanel(@NotNull Project project, String tabId) {
        super(new BorderLayout());
        this.project = project;
        this.stateKey = tabId != null
                ? project.getLocationHash() + "-" + tabId
                : project.getLocationHash();

        // Set consistent renderers and fonts for both combo boxes
        modelProviderComboBox.setRenderer(new ModelProviderRenderer());
        modelNameComboBox.setRenderer(new ModelInfoRenderer());
        
        // Set the font for the combo boxes themselves
        modelProviderComboBox.setFont(DevoxxGenieFontsUtil.getDropdownFont());
        modelNameComboBox.setFont(DevoxxGenieFontsUtil.getDropdownFont());

        addModelProvidersToComboBox();

        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));

        JPanel providerPanel = createProviderPanel();
        toolPanel.add(providerPanel);
        toolPanel.add(Box.createVerticalStrut(5));

        JPanel modelPanel = new JPanel(new BorderLayout());
        modelPanel.add(modelNameComboBox, BorderLayout.CENTER);
        toolPanel.add(modelPanel);

        Dimension comboBoxSize = new Dimension(Integer.MAX_VALUE, modelProviderComboBox.getPreferredSize().height);
        modelProviderComboBox.setMaximumSize(comboBoxSize);
        modelNameComboBox.setMaximumSize(comboBoxSize);

        lastSelectedProvider = DevoxxGenieStateService.getInstance().getSelectedProvider(stateKey);
        lastSelectedLanguageModel = DevoxxGenieStateService.getInstance().getSelectedLanguageModel(stateKey);

        restoreLastSelectedProvider();
        restoreLastSelectedLanguageModel();

        modelProviderComboBox.addActionListener(this::handleModelProviderSelectionChange);
        modelNameComboBox.addActionListener(this::handleModelNameSelectionChange);

        add(toolPanel, BorderLayout.CENTER);
        isInitializationComplete = true;
    }

    /**
     * Create the LLM provider panel.
     *
     * @return the provider panel
     */
    private @NotNull JPanel createProviderPanel() {
        refreshButton = createActionButton(RefreshIcon, "Refresh models", e-> refreshModels());
        JPanel providerPanel = new JPanel(new BorderLayout());
        providerPanel.add(modelProviderComboBox, BorderLayout.CENTER);
        providerPanel.add(refreshButton, BorderLayout.EAST);
        return providerPanel;
    }

    /**
     * Add the LLM providers to combobox.
     * Only show the enabled LLM providers.
     */
    public void addModelProvidersToComboBox() {
        LLMProviderService providerService = LLMProviderService.getInstance();
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        providerService.getAvailableModelProviders().stream()
                .filter(provider -> switch (provider) {
                    case Ollama -> stateService.isOllamaEnabled();
                    case LMStudio -> stateService.isLmStudioEnabled();
                    case GPT4All -> stateService.isGpt4AllEnabled();
                    case Jan -> stateService.isJanEnabled();
                    case LLaMA -> stateService.isLlamaCPPEnabled();
                    case Exo -> stateService.isExoEnabled();
                    case CustomOpenAI -> stateService.isCustomOpenAIUrlEnabled();
                    case OpenAI -> stateService.isOpenAIEnabled();
                    case Mistral -> stateService.isMistralEnabled();
                    case Anthropic -> stateService.isAnthropicEnabled();
                    case Groq -> stateService.isGroqEnabled();
                    case DeepInfra -> stateService.isDeepInfraEnabled();
                    case Google -> stateService.isGoogleEnabled();
                    case DeepSeek -> stateService.isDeepSeekEnabled();
                    case OpenRouter -> stateService.isOpenRouterEnabled();
                    case Grok -> stateService.isGrokEnabled();
                    case Kimi -> stateService.isKimiEnabled();
                    case GLM -> stateService.isGlmEnabled();
                    case AzureOpenAI -> stateService.isAzureOpenAIEnabled();
                    case Bedrock -> stateService.isAwsEnabled();
                    case CLIRunners -> true;
                    case ACPRunners -> true;
                })
                .distinct()
                .sorted(Comparator.comparing(ModelProvider::getName))
                .forEach(modelProviderComboBox::addItem);
    }

    /**
     * Refresh the list of local models
     */
    private void refreshModels() {
        ModelProvider selectedProvider = (ModelProvider) modelProviderComboBox.getSelectedItem();
        if (selectedProvider == null) {
            return;
        }

        if (isLocalProvider(selectedProvider)) {
            refreshLocalModels(selectedProvider);
        } else {
            refreshCloudModels(selectedProvider);
        }
    }

    private static boolean isLocalProvider(ModelProvider provider) {
        return provider == ModelProvider.LMStudio ||
               provider == ModelProvider.Ollama ||
               provider == ModelProvider.Jan ||
               provider == ModelProvider.GPT4All ||
               provider == ModelProvider.OpenRouter ||
               provider == ModelProvider.Bedrock;
    }

    private void refreshLocalModels(ModelProvider selectedProvider) {
        ApplicationManager.getApplication().invokeLater(() -> {
            refreshButton.setEnabled(false);

            ChatModelFactory factory = ChatModelFactoryProvider.getFactoryByProvider(selectedProvider.name())
                    .orElseThrow(() -> new IllegalArgumentException("No factory for provider: " + selectedProvider));
            factory.resetModels();

            refreshModelComboBox(selectedProvider);
            refreshButton.setEnabled(true);
        });
    }

    private void refreshCloudModels(@NonNull ModelProvider selectedProvider) {
        refreshButton.setEnabled(false);

        Set<String> beforeNames = ChatModelFactoryProvider.getFactoryByProvider(selectedProvider.name())
                .map(f -> f.getModels().stream()
                        .map(LanguageModel::getModelName)
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());

        ModelConfigService.getInstance().forceRefresh(() -> {
            ChatModelFactoryProvider.getFactoryByProvider(selectedProvider.name())
                    .ifPresent(ChatModelFactory::resetModels);
            refreshModelComboBox(selectedProvider);
            refreshButton.setEnabled(true);

            notifyModelChanges(selectedProvider, beforeNames);
        });
    }

    private void refreshModelComboBox(@NonNull ModelProvider selectedProvider) {
        updateModelNamesComboBox(selectedProvider.getName());
        modelNameComboBox.setRenderer(new ModelInfoRenderer());
        modelNameComboBox.setFont(DevoxxGenieFontsUtil.getDropdownFont());
        modelNameComboBox.revalidate();
        modelNameComboBox.repaint();
    }

    private void notifyModelChanges(@NonNull ModelProvider selectedProvider,
                                    @NonNull Set<String> beforeNames) {
        Set<String> afterNames = ChatModelFactoryProvider.getFactoryByProvider(selectedProvider.name())
                .map(f -> f.getModels().stream()
                        .map(LanguageModel::getModelName)
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());

        long added = afterNames.stream().filter(n -> !beforeNames.contains(n)).count();
        long removed = beforeNames.stream().filter(n -> !afterNames.contains(n)).count();

        String message;
        if (added == 0 && removed == 0) {
            message = selectedProvider.getName() + " models are up to date (" + afterNames.size() + " models).";
        } else {
            List<String> parts = new ArrayList<>();
            if (added > 0) parts.add(added + " new");
            if (removed > 0) parts.add(removed + " removed");
            message = selectedProvider.getName() + " models refreshed: " + String.join(", ", parts) + ".";
        }
        NotificationUtil.sendNotification(project, message);
    }

    /**
     * Update the model names combobox.
     */
    public void updateModelNamesComboBox(String modelProvider) {
        Optional.ofNullable(modelProvider).ifPresent(provider -> {
            boolean wasUpdating = isUpdatingModelNames;
            isUpdatingModelNames = true;
            try {
                modelNameComboBox.removeAllItems();
                modelNameComboBox.setVisible(true);
                // Ensure font consistency is maintained when updating
                modelNameComboBox.setFont(DevoxxGenieFontsUtil.getDropdownFont());

                ChatModelFactoryProvider
                        .getFactoryByProvider(provider)
                        .ifPresentOrElse(
                                factory -> {
                                    List<LanguageModel> models = factory.getModels();
                                    if (models.isEmpty()) {
                                        hideModelNameComboBox();
                                    } else {
                                        populateModelNames(factory);
                                    }
                                },
                                this::hideModelNameComboBox
                        );
            } catch (Exception e) {
                log.error("Error updating model names", e);
                Messages.showErrorDialog(project, "Failed to update model names: " + e.getMessage(), "Error");
            } finally {
                isUpdatingModelNames = wasUpdating;
            }
        });
    }

    /**
     * Populate the model names.
     *
     * @param chatModelFactory the chat model factory
     */
    private void populateModelNames(@NotNull ChatModelFactory chatModelFactory) {
        modelNameComboBox.removeAllItems();
        List<LanguageModel> modelNames = new ArrayList<>(chatModelFactory.getModels());
        if (modelNames.isEmpty()) {
            hideModelNameComboBox();
        } else {
            modelNames.sort(Comparator.naturalOrder());
            modelNames.forEach(modelNameComboBox::addItem);
        }
    }

    /**
     * Hide the model name combobox.
     */
    private void hideModelNameComboBox() {
        modelNameComboBox.setVisible(false);
    }

    /**
     * Restore the last selected provider from persistent storage
     */
    public void restoreLastSelectedProvider() {
        if (lastSelectedProvider != null) {
            for (int i = 0; i < modelProviderComboBox.getItemCount(); i++) {
                ModelProvider provider = modelProviderComboBox.getItemAt(i);
                if (provider.getName().equals(lastSelectedProvider)) {
                    modelProviderComboBox.setSelectedIndex(i);
                    updateModelNamesComboBox(lastSelectedProvider);
                    break;
                }
            }
        } else {
            setLastSelectedProvider();
        }
    }

    /**
     * Restore the last selected language model from persistent storage
     */
    public void restoreLastSelectedLanguageModel() {
        if (lastSelectedLanguageModel != null) {
            boolean wasUpdating = isUpdatingModelNames;
            isUpdatingModelNames = true;
            try {
                for (int i = 0; i < modelNameComboBox.getItemCount(); i++) {
                    LanguageModel model = modelNameComboBox.getItemAt(i);
                    if (model.getModelName().equals(lastSelectedLanguageModel)) {
                        modelNameComboBox.setSelectedIndex(i);
                        break;
                    }
                }
            } finally {
                isUpdatingModelNames = wasUpdating;
            }
        }
    }

    /**
     * Set the last selected LLM provider or show default.
     */
    public void setLastSelectedProvider() {
        ModelProvider modelProvider = modelProviderComboBox.getItemAt(0);
        if (modelProvider != null) {
            DevoxxGenieStateService.getInstance().setSelectedProvider(stateKey, modelProvider.getName());
            updateModelNamesComboBox(modelProvider.getName());
        }
    }

    @Override
    public void llmSettingsChanged() {
        updateModelNamesComboBox(
                DevoxxGenieStateService.getInstance().getSelectedProvider(stateKey)
        );
    }

    /**
     * When a model is selected for the Exo provider, start preparing the instance in the background.
     */
    private void handleModelNameSelectionChange(@NotNull ActionEvent e) {
        if (!e.getActionCommand().equals(Constant.COMBO_BOX_CHANGED) || !isInitializationComplete || isUpdatingModelNames) return;

        ModelProvider provider = (ModelProvider) modelProviderComboBox.getSelectedItem();
        if (provider != ModelProvider.Exo) return;

        LanguageModel selectedModel = (LanguageModel) modelNameComboBox.getSelectedItem();
        if (selectedModel != null) {
            ExoChatModelFactory.prepareInstanceAsync(selectedModel.getModelName(), project);
        }
    }

    /**
     * Process the model provider selection change.
     * Set the model provider and update the model names.
     */
    private void handleModelProviderSelectionChange(@NotNull ActionEvent e) {
        if (!e.getActionCommand().equals(Constant.COMBO_BOX_CHANGED) ||
            !isInitializationComplete ||
            isUpdatingModelNames)
            return;

        isUpdatingModelNames = true;

        try {
            DevoxxGenieStateService stateInstance = DevoxxGenieStateService.getInstance();
            JComboBox<?> comboBox = (JComboBox<?>) e.getSource();
            ModelProvider modelProvider = (ModelProvider) comboBox.getSelectedItem();
            if (modelProvider != null) {
                stateInstance.setSelectedProvider(stateKey, modelProvider.getName());

                updateModelNamesComboBox(modelProvider.getName());

                modelNameComboBox.setRenderer(new ModelInfoRenderer());
                modelNameComboBox.setFont(DevoxxGenieFontsUtil.getDropdownFont());
                modelNameComboBox.revalidate();
                modelNameComboBox.repaint();
            }
        } finally {
            isUpdatingModelNames = false;
        }
    }
}
