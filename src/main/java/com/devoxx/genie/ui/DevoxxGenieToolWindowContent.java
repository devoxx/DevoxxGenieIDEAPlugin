package com.devoxx.genie.ui;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.ChatMemoryService;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.LLMProviderService;
import com.devoxx.genie.ui.component.PromptInputArea;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.listener.LLMSettingsChangeListener;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.devoxx.genie.ui.panel.ActionButtonsPanel;
import com.devoxx.genie.ui.panel.ConversationPanel;
import com.devoxx.genie.ui.panel.PromptContextFileListPanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.renderer.ModelInfoRenderer;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
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
import java.util.*;
import java.util.stream.Stream;

import static com.devoxx.genie.model.Constant.MESSAGES;

/**
 * The Devoxx Genie Tool Window Content.
 */
public class DevoxxGenieToolWindowContent implements SettingsChangeListener,
                                                     LLMSettingsChangeListener,
                                                     ConversationStarter,
                                                     CustomPromptChangeListener {

    private static final Logger LOG = Logger.getInstance(DevoxxGenieToolWindowContent.class);

    private static final float SPLITTER_PROPORTION = 0.8f;
    public static final int MIN_INPUT_HEIGHT = 100;

    private final Project project;
    private final ResourceBundle resourceBundle = ResourceBundle.getBundle(MESSAGES);

    @Getter
    private final JPanel contentPanel = new JPanel();
    private final ComboBox<ModelProvider> modelProviderComboBox = new ComboBox<>();
    private final ComboBox<LanguageModel> modelNameComboBox = new ComboBox<>();

    private ConversationPanel conversationPanel;
    private PromptInputArea promptInputArea;
    private PromptOutputPanel promptOutputPanel;
    private PromptContextFileListPanel promptContextFileListPanel;
    private ActionButtonsPanel actionButtonsPanel;

    private boolean isInitializationComplete = false;
    private boolean isUpdatingModelNames = false;

    private String lastSelectedProvider = null;
    private String lastSelectedLanguageModel = null;

    /**
     * The Devoxx Genie Tool Window Content constructor.
     *
     * @param toolWindow the tool window
     */
    public DevoxxGenieToolWindowContent(@NotNull ToolWindow toolWindow) {
        project = toolWindow.getProject();

        DevoxxGenieStateService.getInstance().addLoadListener(this::onStateLoaded);
        DevoxxGenieStateService.getInstance().loadState(DevoxxGenieStateService.getInstance());

        setupMessageBusConnection(toolWindow);
    }

    private void onStateLoaded() {

        if (!isInitializationComplete) {
            lastSelectedProvider = DevoxxGenieStateService.getInstance().getSelectedProvider(project.getLocationHash());
            lastSelectedLanguageModel = DevoxxGenieStateService.getInstance().getSelectedLanguageModel(project.getLocationHash());

            setupUI();
            restoreLastSelectedProvider();
            restoreLastSelectedLanguageModel();
            isInitializationComplete = true;
        }
    }

    /**
     * Restore the last selected provider from persistent storage
     */
    private void restoreLastSelectedProvider() {
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
    private void restoreLastSelectedLanguageModel() {
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
     * Set up the message bus connection.
     * @param toolWindow the tool window
     */
    private void setupMessageBusConnection(@NotNull ToolWindow toolWindow) {
        MessageBusConnection messageBusConnection = project.getMessageBus().connect();
        messageBusConnection.subscribe(AppTopics.LLM_SETTINGS_CHANGED_TOPIC, this);
        messageBusConnection.subscribe(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC, this);
        Disposer.register(toolWindow.getDisposable(), messageBusConnection);
    }

    /**
     * Set the last selected LLM provider or show default.
     */
    private void setLastSelectedProvider() {
        ModelProvider modelProvider = modelProviderComboBox.getItemAt(0);
        if (modelProvider != null) {
            DevoxxGenieStateService.getInstance().setSelectedProvider(project.getLocationHash(), modelProvider.getName());
            updateModelNamesComboBox(modelProvider.getName());
        }
    }

    /**
     * Set up the UI Components: top panel and splitter.
     */
    private void setupUI() {
        initializeComponents();
        setupLayout();
        setupListeners();
    }

    private void initializeComponents() {
        modelNameComboBox.setRenderer(new ModelInfoRenderer());

        promptInputArea = new PromptInputArea(resourceBundle);
        promptOutputPanel = new PromptOutputPanel(resourceBundle);
        promptContextFileListPanel = new PromptContextFileListPanel(project);
        conversationPanel = new ConversationPanel(project, this);
    }

    private void setupLayout() {
        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(createTopPanel(), BorderLayout.NORTH);
        contentPanel.add(createSplitter(), BorderLayout.CENTER);
    }

    private void setupListeners() {
        modelNameComboBox.addActionListener(this::processModelNameSelection);
        modelProviderComboBox.addActionListener(this::handleModelProviderSelectionChange);
    }

    /**
     * Create the top panel.
     * @return the top panel
     */
    private @NotNull JPanel createTopPanel() {
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
        Splitter splitter = new Splitter(true, SPLITTER_PROPORTION);
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
        ModelProvider currentProvider = (ModelProvider) modelProviderComboBox.getSelectedItem();
        LanguageModel currentModel = (LanguageModel)modelNameComboBox.getSelectedItem();

        modelProviderComboBox.removeAllItems();
        modelNameComboBox.removeAllItems();
        addModelProvidersToComboBox();

        if (currentProvider != null) {
            modelProviderComboBox.setSelectedItem(currentProvider);
            updateModelNamesComboBox(currentProvider.getName());

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
    private void addModelProvidersToComboBox() {
        LLMProviderService providerService = LLMProviderService.getInstance();

        Stream.concat(
            providerService.getModelProvidersWithApiKeyConfigured().stream(),
            providerService.getLocalModelProviders().stream()
        )
        .distinct()
        .sorted(Comparator.comparing(ModelProvider::getName))
        .forEach(modelProviderComboBox::addItem);
    }

    /**
     * Create the LLM and model name selection panel.
     * @return the selection panel
     */
    private @NotNull JPanel createSelectionPanel() {
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));

        addModelProvidersToComboBox();

        JPanel providerPanel = createProviderPanel();
        toolPanel.add(providerPanel);

        toolPanel.add(Box.createVerticalStrut(5));

        JPanel modelPanel = new JPanel(new BorderLayout());
        modelPanel.add(modelNameComboBox, BorderLayout.CENTER);
        toolPanel.add(modelPanel);

        // Set preferred and maximum sizes for both combo boxes
        Dimension comboBoxSize = new Dimension(Integer.MAX_VALUE, modelProviderComboBox.getPreferredSize().height);
        modelProviderComboBox.setMaximumSize(comboBoxSize);
        modelNameComboBox.setMaximumSize(comboBoxSize);

        return toolPanel;
    }

    /**
     * Create the LLM provider panel.
     * @return the provider panel
     */
    private @NotNull JPanel createProviderPanel() {
        JPanel providerPanel = new JPanel(new BorderLayout());
        providerPanel.add(modelProviderComboBox, BorderLayout.CENTER);
        return providerPanel;
    }

    /**
     * Create the Submit panel.
     * @return the Submit panel
     */
    private @NotNull JPanel createInputPanel() {
        JPanel submitPanel = new JPanel(new BorderLayout());
        submitPanel.setMinimumSize(new Dimension(Integer.MAX_VALUE, MIN_INPUT_HEIGHT));
        submitPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, MIN_INPUT_HEIGHT));
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
            modelProviderComboBox,
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
        FileListManager.getInstance().clear();
        ChatMemoryService.getInstance().clear();

        SwingUtilities.invokeLater(() -> {
            conversationPanel.updateNewConversationLabel();
            promptInputArea.clear();
            promptOutputPanel.clear();
            actionButtonsPanel.resetProjectContext();
            actionButtonsPanel.enableButtons();
            actionButtonsPanel.resetTokenUsageBar();
        });
    }

    /**
     * Process the model name selection.
     */
    private void processModelNameSelection(@NotNull ActionEvent e) {

        if (e.getActionCommand().equals(Constant.COMBO_BOX_CHANGED) && isInitializationComplete) {
            LanguageModel selectedModel = (LanguageModel) modelNameComboBox.getSelectedItem();
            if (selectedModel != null) {
                DevoxxGenieStateService.getInstance().setSelectedLanguageModel(project.getLocationHash(), selectedModel.getModelName());
                actionButtonsPanel.updateTokenUsage(selectedModel.getContextWindow());
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

    /**
     * Update the model names combobox.
     */
    private void updateModelNamesComboBox(String modelProvider) {
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

    @Override
    public void settingsChanged() {
        updateModelNamesComboBox(DevoxxGenieStateService.getInstance().getSelectedProvider(project.getLocationHash()));
    }

    @Override
    public void onCustomPromptsChanged() {
        SwingUtilities.invokeLater(() -> {
            // Update the help panel or any other UI components that display custom prompts
            if (promptOutputPanel != null) {
                promptOutputPanel.updateHelpText();
            }
        });
    }
}
