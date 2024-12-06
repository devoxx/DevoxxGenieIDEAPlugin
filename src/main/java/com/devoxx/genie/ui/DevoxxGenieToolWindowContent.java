package com.devoxx.genie.ui;

import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.*;
import com.devoxx.genie.ui.component.PromptInputArea;
import com.devoxx.genie.ui.listener.ConversationEventListener;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.devoxx.genie.ui.panel.*;
import com.devoxx.genie.ui.renderer.ModelInfoRenderer;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.messages.MessageBusConnection;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;

import static com.devoxx.genie.model.Constant.MESSAGES;

/**
 * The Devoxx Genie Tool Window Content.
 */
public class DevoxxGenieToolWindowContent implements SettingsChangeListener,
                                                     ConversationStarter,
                                                     CustomPromptChangeListener,
                                                     ConversationEventListener {

    private static final Logger LOG = Logger.getInstance(DevoxxGenieToolWindowContent.class);

    private static final float SPLITTER_PROPORTION = 0.8f;
    public static final int MIN_INPUT_HEIGHT = 200;

    private final Project project;
    private final ResourceBundle resourceBundle = ResourceBundle.getBundle(MESSAGES);

    @Getter
    private final JPanel contentPanel = new JPanel();

    private LlmProviderPanel llmProviderPanel;
    private ConversationPanel conversationPanel;
    private PromptInputArea promptInputArea;
    private PromptOutputPanel promptOutputPanel;
    private PromptContextFileListPanel promptContextFileListPanel;
    private ActionButtonsPanel actionButtonsPanel;

    private boolean isInitializationComplete = false;

    private final ChatService chatService;
    private final ConversationStorageService storageService = ConversationStorageService.getInstance();

    /**
     * The Devoxx Genie Tool Window Content constructor.
     *
     * @param toolWindow the tool window
     */
    public DevoxxGenieToolWindowContent(@NotNull ToolWindow toolWindow) {
        project = toolWindow.getProject();

        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        stateService.addLoadListener(this::onStateLoaded);
        stateService.loadState(DevoxxGenieStateService.getInstance());

        chatService = new ChatService(storageService, project);

        setupMessageBusConnection(toolWindow);
    }

    private void onStateLoaded() {

        if (!isInitializationComplete) {
            setupUI();
            isInitializationComplete = true;
        }
    }

    /**
     * Set up the message bus connection.
     *
     * @param toolWindow the tool window
     */
    private void setupMessageBusConnection(@NotNull ToolWindow toolWindow) {
        MessageBusConnection messageBusConnection = project.getMessageBus().connect();
        messageBusConnection.subscribe(AppTopics.LLM_SETTINGS_CHANGED_TOPIC, this.llmProviderPanel);
        messageBusConnection.subscribe(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC, this);
        messageBusConnection.subscribe(AppTopics.CONVERSATION_TOPIC, this);
        Disposer.register(toolWindow.getDisposable(), messageBusConnection);
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
        llmProviderPanel = new LlmProviderPanel(project);
        llmProviderPanel.getModelNameComboBox().setRenderer(new ModelInfoRenderer());
        promptInputArea = new PromptInputArea(resourceBundle, project);
        promptOutputPanel = new PromptOutputPanel(resourceBundle);
        promptContextFileListPanel = new PromptContextFileListPanel(project);
        conversationPanel = new ConversationPanel(project, this, storageService, promptOutputPanel);
    }

    private void setupLayout() {
        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(createTopPanel(), BorderLayout.NORTH);
        contentPanel.add(createSplitter(), BorderLayout.CENTER);
    }

    private void setupListeners() {
        llmProviderPanel.getModelNameComboBox().addActionListener(this::processModelNameSelection);
    }

    /**
     * Create the top panel.
     *
     * @return the top panel
     */
    private @NotNull JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(llmProviderPanel, BorderLayout.NORTH);
        topPanel.add(conversationPanel, BorderLayout.CENTER);
        return topPanel;
    }

    /**
     * Create the splitter.
     *
     * @return the splitter
     */
    private @NotNull Splitter createSplitter() {
        OnePixelSplitter splitter = new OnePixelSplitter(true, SPLITTER_PROPORTION);
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
        ModelProvider currentProvider = (ModelProvider) llmProviderPanel.getModelProviderComboBox().getSelectedItem();
        LanguageModel currentModel = (LanguageModel) llmProviderPanel.getModelNameComboBox().getSelectedItem();

        llmProviderPanel.getModelProviderComboBox().removeAllItems();
        llmProviderPanel.getModelNameComboBox().removeAllItems();
        llmProviderPanel.addModelProvidersToComboBox();

        if (currentProvider != null) {
            llmProviderPanel.getModelProviderComboBox().setSelectedItem(currentProvider);
            llmProviderPanel.updateModelNamesComboBox(currentProvider.getName());

            if (currentModel != null) {
                llmProviderPanel.getModelNameComboBox().setSelectedItem(currentModel);
            }
        } else {
            llmProviderPanel.setLastSelectedProvider();
        }

        actionButtonsPanel.configureSearchButtonsVisibility();
    }

    /**
     * Create the Submit panel.
     *
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
     *
     * @return the action buttons panel
     */
    @Contract(" -> new")
    private @NotNull JPanel createActionButtonsPanel() {
        actionButtonsPanel = new ActionButtonsPanel(project,
            promptInputArea,
            promptOutputPanel,
                llmProviderPanel.getModelProviderComboBox(),
                llmProviderPanel.getModelNameComboBox(),
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
        ChatMemoryService.getInstance().clear(project);

        chatService.startNewConversation("");

        ApplicationManager.getApplication().invokeLater(() -> {
            conversationPanel.updateNewConversationLabel();
            promptInputArea.clear();
            promptOutputPanel.clear();
            actionButtonsPanel.resetProjectContext();
            actionButtonsPanel.enableButtons();
            actionButtonsPanel.resetTokenUsageBar();
            promptInputArea.requestFocusInWindow();
        });
    }

    /**
     * Process the model name selection.
     */
    private void processModelNameSelection(@NotNull ActionEvent e) {

        if (e.getActionCommand().equals(Constant.COMBO_BOX_CHANGED) && isInitializationComplete) {
            LanguageModel selectedModel = (LanguageModel) llmProviderPanel.getModelNameComboBox().getSelectedItem();
            if (selectedModel != null) {
                DevoxxGenieStateService.getInstance().setSelectedLanguageModel(project.getLocationHash(), selectedModel.getModelName());
                actionButtonsPanel.updateTokenUsage(selectedModel.getContextWindow());
            }
        }
    }

    @Override
    public void onCustomPromptsChanged() {
        ApplicationManager.getApplication().invokeLater(() -> {
            // Update the help panel or any other UI components that display custom prompts
            if (promptOutputPanel != null) {
                promptOutputPanel.updateHelpText();
            }
        });
    }

    @Override
    public void onNewConversation(ChatMessageContext chatMessageContext) {
        conversationPanel.loadConversationHistory();
    }
}
