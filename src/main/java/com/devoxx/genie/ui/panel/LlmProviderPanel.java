package com.devoxx.genie.ui.panel;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.LLMProviderService;
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.RefreshIcon;

@Slf4j
public class LlmProviderPanel extends JBPanel<LlmProviderPanel> implements LLMSettingsChangeListener {

    private final transient Project project;

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
     * The conversation panel constructor.
     *
     * @param project the project instance
     */
    public LlmProviderPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;

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

        lastSelectedProvider = DevoxxGenieStateService.getInstance().getSelectedProvider(project.getLocationHash());
        lastSelectedLanguageModel = DevoxxGenieStateService.getInstance().getSelectedLanguageModel(project.getLocationHash());

        restoreLastSelectedProvider();
        restoreLastSelectedLanguageModel();

        modelProviderComboBox.addActionListener(this::handleModelProviderSelectionChange);

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

        if (selectedProvider == ModelProvider.LMStudio ||
            selectedProvider == ModelProvider.Ollama ||
            selectedProvider == ModelProvider.Jan ||
            selectedProvider == ModelProvider.GPT4All ||
            selectedProvider == ModelProvider.OpenRouter ||
            selectedProvider == ModelProvider.Bedrock) {
            ApplicationManager.getApplication().invokeLater(() -> {
                refreshButton.setEnabled(false);

                ChatModelFactory factory = ChatModelFactoryProvider.getFactoryByProvider(selectedProvider.name())
                        .orElseThrow(() -> new IllegalArgumentException("No factory for provider: " + selectedProvider));
                factory.resetModels();

                updateModelNamesComboBox(selectedProvider.getName());
                modelNameComboBox.setRenderer(new ModelInfoRenderer());
                modelNameComboBox.setFont(DevoxxGenieFontsUtil.getDropdownFont());
                modelNameComboBox.revalidate();
                modelNameComboBox.repaint();
                refreshButton.setEnabled(true);

            });
        } else {
            NotificationUtil.sendNotification(project,
                    "Model refresh is only available for LMStudio, Ollama, GPT4All and Jan providers.");
        }
    }

    /**
     * Update the model names combobox.
     */
    public void updateModelNamesComboBox(String modelProvider) {
        Optional.ofNullable(modelProvider).ifPresent(provider -> {
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
            for (int i = 0; i < modelNameComboBox.getItemCount(); i++) {
                LanguageModel model = modelNameComboBox.getItemAt(i);
                if (model.getModelName().equals(lastSelectedLanguageModel)) {
                    modelNameComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Set the last selected LLM provider or show default.
     */
    public void setLastSelectedProvider() {
        ModelProvider modelProvider = modelProviderComboBox.getItemAt(0);
        if (modelProvider != null) {
            DevoxxGenieStateService.getInstance().setSelectedProvider(project.getLocationHash(), modelProvider.getName());
            updateModelNamesComboBox(modelProvider.getName());
        }
    }

    @Override
    public void llmSettingsChanged() {
        updateModelNamesComboBox(
                DevoxxGenieStateService.getInstance().getSelectedProvider(project.getLocationHash())
        );
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
                stateInstance.setSelectedProvider(project.getLocationHash(), modelProvider.getName());

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
