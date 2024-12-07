package com.devoxx.genie.ui.panel;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.service.*;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.listener.LLMSettingsChangeListener;
import com.devoxx.genie.ui.renderer.ModelInfoRenderer;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.RefreshIcon;

public class LlmProviderPanel extends JBPanel<LlmProviderPanel> implements LLMSettingsChangeListener {

    private static final Logger LOG = Logger.getInstance(LlmProviderPanel.class);

    private final Project project;

    @Getter
    private final JPanel contentPanel = new JPanel();
    @Getter
    private final ComboBox<ModelProvider> modelProviderComboBox = new ComboBox<>();
    @Getter
    private final ComboBox<LanguageModel> modelNameComboBox = new ComboBox<>();

    private final JButton refreshButton = new JHoverButton(RefreshIcon, true);

    private String lastSelectedProvider = null;
    private String lastSelectedLanguageModel = null;

    private boolean isInitializationComplete = false;
    private boolean isUpdatingModelNames = false;

    /**
     * The conversation panel constructor.
     *
     * @param project             the project instance
     */
    public LlmProviderPanel(Project project) {
        super(new BorderLayout());
        this.project = project;

        modelNameComboBox.setRenderer(new ModelInfoRenderer());

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
        JPanel providerPanel = new JPanel(new BorderLayout());
        providerPanel.add(modelProviderComboBox, BorderLayout.CENTER);

        refreshButton.setToolTipText("Refresh models");
        refreshButton.addActionListener(e -> refreshModels());

        providerPanel.add(refreshButton, BorderLayout.EAST);
        return providerPanel;
    }

    /**
     * Add the LLM providers to combobox.
     * Only show the cloud-based LLM providers for which we have an API Key.
     */
    public void addModelProvidersToComboBox() {
        LLMProviderService providerService = LLMProviderService.getInstance();
        providerService.getAvailableModelProviders().stream()
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

        if (selectedProvider == ModelProvider.LMStudio || selectedProvider == ModelProvider.Ollama || selectedProvider == ModelProvider.Jan) {
            ApplicationManager.getApplication().invokeLater(() -> {
                refreshButton.setEnabled(false);

                ChatModelFactory factory = ChatModelFactoryProvider.getFactoryByProvider(selectedProvider.name())
                        .orElseThrow(() -> new IllegalArgumentException("No factory for provider: " + selectedProvider));
                factory.resetModels();

                updateModelNamesComboBox(selectedProvider.getName());
                modelNameComboBox.setRenderer(new ModelInfoRenderer());
                modelNameComboBox.revalidate();
                modelNameComboBox.repaint();
                refreshButton.setEnabled(true);

            });
        } else {
            NotificationUtil.sendNotification(project, "Model refresh is only available for LMStudio, Ollama and Jan providers.");
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
                LOG.error("Error updating model names", e);
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
        if (!e.getActionCommand().equals(Constant.COMBO_BOX_CHANGED) || !isInitializationComplete || isUpdatingModelNames)
            return;

        isUpdatingModelNames = true;

        try {
            JComboBox<?> comboBox = (JComboBox<?>) e.getSource();
            ModelProvider modelProvider = (ModelProvider) comboBox.getSelectedItem();
            if (modelProvider != null) {
                DevoxxGenieStateService.getInstance().setSelectedProvider(project.getLocationHash(), modelProvider.getName());

                updateModelNamesComboBox(modelProvider.getName());
                modelNameComboBox.setRenderer(new ModelInfoRenderer());
                modelNameComboBox.revalidate();
                modelNameComboBox.repaint();
            }
        } finally {
            isUpdatingModelNames = false;
        }
    }


}
