package com.devoxx.genie.ui;

import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.conversations.ConversationStorageService;
import com.devoxx.genie.ui.component.InputSwitch;
import com.devoxx.genie.ui.component.border.AnimatedGlowingBorder;
import com.devoxx.genie.ui.listener.GlowingListener;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.devoxx.genie.ui.panel.ConversationPanel;
import com.devoxx.genie.ui.panel.LlmProviderPanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.panel.SubmitPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.util.MessageBusUtil;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.OnePixelSplitter;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import static com.devoxx.genie.model.Constant.MESSAGES;

/**
 * The Devoxx Genie Tool Window Content.
 */
public class DevoxxGenieToolWindowContent implements SettingsChangeListener, GlowingListener {

    private static final float SPLITTER_PROPORTION = 0.75f;
    private static final float MIN_PROPORTION = 0.3f;
    private static final float MAX_PROPORTION = 0.85f;

    @Getter
    private final Project project;
    @Getter
    private final ResourceBundle resourceBundle = ResourceBundle.getBundle(MESSAGES);
    @Getter
    private final JPanel contentPanel = new JPanel();
    @Getter
    private final ConversationStorageService storageService = ConversationStorageService.getInstance();
    private final AnimatedGlowingBorder animatedBorder;
    @Getter
    private LlmProviderPanel llmProviderPanel;
    @Getter
    private ConversationPanel conversationPanel;
    @Getter
    private SubmitPanel submitPanel;
    @Getter
    private PromptOutputPanel promptOutputPanel;
    private boolean isInitializationComplete = false;

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

        setupMessageBusConnection(toolWindow);

        animatedBorder = new AnimatedGlowingBorder(contentPanel);
    }

    private void onStateLoaded() {

        if (!isInitializationComplete) {
            setupUI();
            isInitializationComplete = true;
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
        llmProviderPanel = new LlmProviderPanel(project);
        promptOutputPanel = new PromptOutputPanel(project, resourceBundle);
        submitPanel = new SubmitPanel(this);
        conversationPanel = new ConversationPanel(this);
    }

    private void setupLayout() {
        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(createTopPanel(), BorderLayout.NORTH);
        contentPanel.add(createSplitter(), BorderLayout.CENTER);
    }

    private void setupListeners() {
        llmProviderPanel.getModelNameComboBox().addActionListener(this::processModelNameSelection);
    }

    @Override
    public void startGlowing() {
        animatedBorder.startGlowing();
    }

    @Override
    public void stopGlowing() {
        animatedBorder.stopGlowing();
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
        OnePixelSplitter splitter =
                new OnePixelSplitter(true, SPLITTER_PROPORTION, MIN_PROPORTION, MAX_PROPORTION);
        splitter.setFirstComponent(promptOutputPanel);
        splitter.setSecondComponent(submitPanel);
        splitter.setHonorComponentsMinimumSize(true);

        return splitter;
    }

    /**
     * Set up the message bus connection.
     *
     * @param toolWindow the tool window
     */
    private void setupMessageBusConnection(@NotNull ToolWindow toolWindow) {
        MessageBusUtil.connect(project, connection -> {
            MessageBusUtil.subscribe(connection, AppTopics.LLM_SETTINGS_CHANGED_TOPIC, llmProviderPanel);
            MessageBusUtil.subscribe(connection, AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC, promptOutputPanel);

            MessageBusUtil.subscribe(connection, AppTopics.CONVERSATION_TOPIC, conversationPanel);
            MessageBusUtil.subscribe(connection, LafManagerListener.TOPIC, source -> conversationPanel.updateFontSize());

            MessageBusUtil.subscribe(connection, AppTopics.SETTINGS_CHANGED_TOPIC, submitPanel.getActionButtonsPanel());
            MessageBusUtil.subscribe(connection, AppTopics.PROMPT_SUBMISSION_TOPIC, submitPanel.getActionButtonsPanel());
            MessageBusUtil.subscribe(connection, FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                @Override
                public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    JButton addFileBtn = submitPanel.getActionButtonsPanel().getEditorFileButtonManager().getAddFileBtn();
                    if (addFileBtn == null) {
                        return;
                    }
                    addFileBtn.setEnabled(true);
                    addFileBtn.setToolTipText("Select file(s) for prompt context");
                }
            });

            MessageBusUtil.subscribe(connection, AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC, promptOutputPanel.getWelcomePanel());
            MessageBusUtil.subscribe(connection, LafManagerListener.TOPIC, source -> promptOutputPanel.getWelcomePanel().updateFontSize());
            MessageBusUtil.subscribe(connection, AppTopics.SHORTCUT_CHANGED_TOPIC, submitPanel.getPromptInputArea());

            // Search options panel : Set up message bus listeners for visibility changes
            MessageBusUtil.subscribe(connection, AppTopics.RAG_STATE_TOPIC, enabled -> {
                InputSwitch ragSwitch = submitPanel.getPromptInputArea().getSearchOptionsPanel().getSwitches().get(0);
                ragSwitch.setVisible(enabled);
                ragSwitch.setSelected(enabled);
                submitPanel.getPromptInputArea().getSearchOptionsPanel().updatePanelVisibility();
            });
            MessageBusUtil.subscribe(connection, AppTopics.GITDIFF_STATE_TOPIC, enabled -> {
                InputSwitch gitDiffSwitch = submitPanel.getPromptInputArea().getSearchOptionsPanel().getSwitches().get(1);
                gitDiffSwitch.setVisible(enabled);
                gitDiffSwitch.setSelected(enabled);
                submitPanel.getPromptInputArea().getSearchOptionsPanel().updatePanelVisibility();
            });
            MessageBusUtil.subscribe(connection, AppTopics.WEB_SEARCH_STATE_TOPIC, enabled -> {
                InputSwitch webSearchSwitch = submitPanel.getPromptInputArea().getSearchOptionsPanel().getSwitches().get(2);
                webSearchSwitch.setVisible(enabled);
                webSearchSwitch.setSelected(enabled);
                submitPanel.getPromptInputArea().getSearchOptionsPanel().updatePanelVisibility();
            });

            Disposer.register(toolWindow.getDisposable(), connection);
        });
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
    }

    /**
     * Process the model name selection.
     */
    private void processModelNameSelection(@NotNull ActionEvent e) {

        if (e.getActionCommand().equals(Constant.COMBO_BOX_CHANGED) && isInitializationComplete) {
            LanguageModel selectedModel = (LanguageModel) llmProviderPanel.getModelNameComboBox().getSelectedItem();
            if (selectedModel != null) {
                DevoxxGenieStateService.getInstance().setSelectedLanguageModel(project.getLocationHash(), selectedModel.getModelName());
                submitPanel.getActionButtonsPanel().updateTokenUsage(selectedModel.getInputMaxTokens());
            }
        }
    }
}
