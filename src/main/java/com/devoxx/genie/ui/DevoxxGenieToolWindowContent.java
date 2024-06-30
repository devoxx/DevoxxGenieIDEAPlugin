package com.devoxx.genie.ui;

import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.ChatMemoryService;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.LLMProviderService;
import com.devoxx.genie.ui.component.PromptInputArea;
import com.devoxx.genie.ui.listener.LLMSettingsChangeListener;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.devoxx.genie.ui.panel.ActionButtonsPanel;
import com.devoxx.genie.ui.panel.ConversationPanel;
import com.devoxx.genie.ui.panel.PromptContextFileListPanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.renderer.ModelInfoRenderer;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.messages.MessageBusConnection;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ResourceBundle;

import static com.devoxx.genie.model.Constant.MESSAGES;

/**
 * The Devoxx Genie Tool Window Content.
 */
public class DevoxxGenieToolWindowContent implements SettingsChangeListener, LLMSettingsChangeListener, ConversationStarter {

    private final Project project;
    private final ResourceBundle resourceBundle = ResourceBundle.getBundle(MESSAGES);

    @Getter
    private final JPanel contentPanel = new JPanel();
    private final ComboBox<String> llmProvidersComboBox = new ComboBox<>();
    private final ComboBox<LanguageModel> modelNameComboBox = new ComboBox<>();

    private ConversationPanel conversationPanel;
    private PromptInputArea promptInputArea;
    private PromptOutputPanel promptOutputPanel;
    private PromptContextFileListPanel promptContextFileListPanel;
    private ActionButtonsPanel actionButtonsPanel;

    private final LLMProviderService llmProviderService;

    private boolean isInitializationComplete = false;
    private boolean isUpdatingModelNames = false;

    private final MessageBusConnection messageBusConnection;

    /**
     * The Devoxx Genie Tool Window Content constructor.
     *
     * @param toolWindow the tool window
     */
    public DevoxxGenieToolWindowContent(@NotNull ToolWindow toolWindow) {
        project = toolWindow.getProject();

        this.llmProviderService = LLMProviderService.getInstance();

        setupUI();

        modelNameComboBox.setRenderer(new ModelInfoRenderer());
        modelNameComboBox.addActionListener(e -> updateTokenUsageBar(e));

        messageBusConnection = toolWindow.getProject().getMessageBus().connect();
        messageBusConnection.subscribe(AppTopics.LLM_SETTINGS_CHANGED_TOPIC, this);

        setLastSelectedProvider();
        isInitializationComplete = true;
    }

    /**
     * Set the last selected LLM provider or show default.
     */
    private void setLastSelectedProvider() {
        String lastSelectedProvider = DevoxxGenieStateService.getInstance().getLastSelectedProvider();
        if (lastSelectedProvider != null && !lastSelectedProvider.isEmpty()) {
            llmProvidersComboBox.setSelectedItem(lastSelectedProvider);
            updateModelNamesComboBox();
        } else {
            // If no last selected provider, select the first item in the combobox
            Object selectedItem = llmProvidersComboBox.getSelectedItem();
            if (selectedItem != null) {
                updateModelNamesComboBox();
            }
        }
    }

    private void updateTokenUsageBar(@NotNull ActionEvent e) {
        LanguageModel languageModel = (LanguageModel)((ComboBox)e.getSource()).getSelectedItem();
        if (languageModel != null) {
            actionButtonsPanel.updateTokenUsage(languageModel.getMaxTokens());
        }
    }

    /**
     * Set up the UI Components: top panel and splitter.
     */
    private void setupUI() {
        modelNameComboBox.setRenderer(new ModelInfoRenderer());

        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(createTopPanel(), BorderLayout.NORTH);
        contentPanel.add(createSplitter(), BorderLayout.CENTER);

        setLastSelectedProvider();
    }

    /**
     * Create the top panel.
     *
     * @return the top panel
     */
    private @NotNull JPanel createTopPanel() {
        promptInputArea = new PromptInputArea(resourceBundle);
        promptOutputPanel = new PromptOutputPanel(resourceBundle);
        promptContextFileListPanel = new PromptContextFileListPanel(project);
        conversationPanel = new ConversationPanel(project, this);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(createSelectionPanel(), BorderLayout.NORTH);
        topPanel.add(conversationPanel, BorderLayout.CENTER);
        return topPanel;
    }

    /**
     * Create the splitter.
     * @return the splitter
     */
    private @NotNull Splitter createSplitter() {
        Splitter splitter = new Splitter(true, 0.8f);
        splitter.setFirstComponent(promptOutputPanel);
        splitter.setSecondComponent(createInputPanel());
        splitter.setHonorComponentsMinimumSize(true);
        return splitter;
    }

    /**
     * Refresh the UI elements because the settings have changed.
     */
    @Override
    public void settingsChanged(boolean hasKey) {
        String currentProvider = (String) llmProvidersComboBox.getSelectedItem();
        LanguageModel currentModel = (LanguageModel)modelNameComboBox.getSelectedItem();

        llmProvidersComboBox.removeAllItems();
        addLLMProvidersToComboBox();

        if (currentProvider != null && !currentProvider.isEmpty()) {
            llmProvidersComboBox.setSelectedItem(currentProvider);
            updateModelNamesComboBox();
            if (currentModel != null) {
                modelNameComboBox.setSelectedItem(currentModel);
            }
        } else {
            setLastSelectedProvider();
        }

        actionButtonsPanel.configureSearchButtonsVisibility();
    }

    /**
     * Add the LLM providers to combobox.
     * Only show the cloud-based LLM providers for which we have an API Key.
     */
    private void addLLMProvidersToComboBox() {
        llmProviderService.getAvailableLLMProviders()
            .stream()
            .sorted()
            .forEach(llmProvidersComboBox::addItem);
    }

    /**
     * Create the LLM and model name selection panel.
     * @return the selection panel
     */
    @NotNull
    private JPanel createSelectionPanel() {
        JPanel toolPanel = createToolPanel();
        addLLMProvidersToComboBox();

        modelNameComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, modelNameComboBox.getPreferredSize().height));
        modelNameComboBox.addActionListener(this::processModelNameSelection);

        return toolPanel;
    }

    /**
     * Create the tool panel.
     * @return the tool panel
     */
    private @NotNull JPanel createToolPanel() {
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));
        toolPanel.add(createProviderPanel());
        toolPanel.add(Box.createVerticalStrut(5));
        toolPanel.add(modelNameComboBox);
        return toolPanel;
    }

    /**
     * Create the LLM provider panel.
     * @return the provider panel
     */
    private @NotNull JPanel createProviderPanel() {
        JPanel providerPanel = new JPanel(new BorderLayout(), true);
        providerPanel.add(llmProvidersComboBox, BorderLayout.CENTER);
        llmProvidersComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, llmProvidersComboBox.getPreferredSize().height));
        llmProvidersComboBox.addActionListener(this::handleModelProviderSelectionChange);
        return providerPanel;
    }

    /**
     * Create the Submit panel.
     * @return the Submit panel
     */
    private @NotNull JPanel createInputPanel() {
        JPanel submitPanel = new JPanel(new BorderLayout());
        submitPanel.setMinimumSize(new Dimension(Integer.MAX_VALUE, 100));
        submitPanel.add(promptContextFileListPanel, BorderLayout.NORTH);
        submitPanel.add(new JBScrollPane(promptInputArea), BorderLayout.CENTER);
        submitPanel.add(createActionButtonsPanel(), BorderLayout.SOUTH);
        return submitPanel;
    }

    /**
     * The bottom action buttons panel (Submit, Search buttons and Add Files)
     * @return the action buttons panel
     */
    @Contract(" -> new")
    private @NotNull JPanel createActionButtonsPanel() {
        actionButtonsPanel = new ActionButtonsPanel(project,
            promptInputArea,
            promptOutputPanel,
            llmProvidersComboBox,
            modelNameComboBox,
            this);
        return actionButtonsPanel;
    }

    /**
     * Start a new conversation.
     * Clear the conversation panel, prompt input area, prompt output panel, file list and chat memory.
     */
    @Override
    public void startNewConversation() {
        conversationPanel.updateNewConversationLabel();
        promptInputArea.clear();
        promptOutputPanel.clear();
        FileListManager.getInstance().clear();
        ChatMemoryService.getInstance().clear();
        actionButtonsPanel.resetProjectContext();
        actionButtonsPanel.enableButtons();
    }

    /**
     * Process the model name selection.
     */
    private void processModelNameSelection(@NotNull ActionEvent e) {
        if (e.getActionCommand().equals(Constant.COMBO_BOX_CHANGED)) {
            // Reset the project context if the provider has been changed
            actionButtonsPanel.resetProjectContext();

            LanguageModel selectedModel = (LanguageModel) modelNameComboBox.getSelectedItem();
            if (selectedModel != null) {
                DevoxxGenieStateService.getInstance().setLastSelectedModel(selectedModel.getName());
            }
        }
    }

    /**
     * Process the model provider selection change.
     * Set the model provider and update the model names.
     */
    private void handleModelProviderSelectionChange(@NotNull ActionEvent e) {
        if (!e.getActionCommand().equals(Constant.COMBO_BOX_CHANGED) || !isInitializationComplete || isUpdatingModelNames) return;

        isUpdatingModelNames = true;

        try {
            JComboBox<?> comboBox = (JComboBox<?>) e.getSource();
            Object selectedItem = comboBox.getSelectedItem();

            if (selectedItem instanceof String selectedLLMProvider) {
                DevoxxGenieStateService.getInstance().setLastSelectedProvider(selectedLLMProvider);
                updateModelNamesComboBox();
            } else if (selectedItem instanceof LanguageModel selectedModel) {
                DevoxxGenieStateService.getInstance().setLastSelectedModel(selectedModel.getName());
            }
        } finally {
            isUpdatingModelNames = false;
        }
    }

    /**
     * Update the model names combobox.
     */
    private void updateModelNamesComboBox() {
        SwingUtilities.invokeLater(() -> {
            String selectedProvider = (String) llmProvidersComboBox.getSelectedItem();
            if (selectedProvider != null) {
                ModelProvider provider = ModelProvider.valueOf(selectedProvider);
                ChatModelFactoryProvider.getFactoryByProvider(provider).ifPresent(factory -> {
                    modelNameComboBox.removeAllItems();
                    List<LanguageModel> models = factory.getModelNames();
                    for (LanguageModel model : models) {
                        modelNameComboBox.addItem(model);
                    }
                });
            }
        });
    }
//
//    /**
//     * Populate the model names.
//     * @param chatModelFactory the chat model factory
//     */
//    private void populateModelNames(@NotNull ChatModelFactory chatModelFactory) {
//        List<LanguageModel> modelNames = chatModelFactory.getModelNames();
//        if (modelNames.isEmpty()) {
//            hideModelNameComboBox();
//        } else {
//            modelNames.stream()
//                .sorted()
//                .forEach(modelNameComboBox::addItem);
//        }
//    }

    /**
     * Hide the model name combobox.
     */
    private void hideModelNameComboBox() {
        modelNameComboBox.setVisible(false);
    }

    @Override
    public void settingsChanged() {
        updateModelNamesComboBox();
    }

    public void dispose() {
        messageBusConnection.disconnect();
    }
}
