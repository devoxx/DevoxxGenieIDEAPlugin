package com.devoxx.genie.ui.panel;

import com.devoxx.genie.controller.ActionButtonsPanelController;
import com.devoxx.genie.controller.ProjectContextController;
import com.devoxx.genie.controller.listener.TokenCalculationListener;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.listener.ConversationEventListener;
import com.devoxx.genie.ui.agent.AgentToggleManager;
import com.devoxx.genie.ui.mcp.MCPToolsManager;
import com.devoxx.genie.service.spec.SpecTaskRunnerService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.window.DevoxxGenieToolWindowContent;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.component.button.AddFilesToContextButton;
import com.devoxx.genie.ui.component.button.EditorFileButtonManager;
import com.devoxx.genie.ui.component.ContextPopupMenu;
import com.devoxx.genie.ui.window.ConversationTabRegistry;
import com.devoxx.genie.ui.component.TokenUsageBar;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.listener.GlowingListener;
import com.devoxx.genie.ui.listener.PromptSubmissionListener;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.devoxx.genie.ui.util.WindowContextFormatterUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

import static com.devoxx.genie.model.Constant.*;
import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.*;

public class ActionButtonsPanel extends JPanel
        implements SettingsChangeListener, PromptSubmissionListener, GlowingListener,
        TokenCalculationListener, ConversationEventListener {

    private final transient Project project;

    @Getter
    private final transient EditorFileButtonManager editorFileButtonManager;

    private AddFilesToContextButton addFileBtn;
    private JButton submitBtn;
    private JButton addProjectBtn;
    private JButton calcTokenCostBtn;

    private final SubmitPanel submitPanel;
    private final JPanel calcProjectPanel = createCalcProjectPanel();
    private final PromptInputArea promptInputArea;
    private final TokenUsageBar tokenUsageBar = createTokenUsageBar();

    // Persistent indicator showing how much of the selected model's context window the
    // ongoing conversation occupies. Updated after every completed response (latest turn's
    // input + output tokens) and reset when a new conversation starts.
    private final JBLabel conversationContextLabel = createConversationContextLabel();
    private final TokenUsageBar conversationContextBar = createConversationContextBar();
    private final JPanel conversationContextPanel = new JPanel(new BorderLayout());
    private int conversationMaxTokens;
    private int conversationUsedTokens;

    private final transient DevoxxGenieToolWindowContent devoxxGenieToolWindowContent;

    private final transient ActionButtonsPanelController controller;
    private final transient ProjectContextController projectContextController;
    
    // The MCP tools manager
    private final transient MCPToolsManager mcpToolsManager;

    // The Agent toggle manager
    private final transient AgentToggleManager agentToggleManager;

    // Queued prompt from spec task runner — submitted when current execution finishes
    private String pendingSpecPrompt;

    public ActionButtonsPanel(Project project,
                              SubmitPanel submitPanel,
                              PromptInputArea promptInputArea,
                              PromptOutputPanel promptOutputPanel,
                              ComboBox<ModelProvider> llmProvidersComboBox,
                              ComboBox<LanguageModel> modelNameComboBox,
                              DevoxxGenieToolWindowContent devoxxGenieToolWindowContent) {
        setLayout(new BorderLayout());

        // Initialize fields and components
        this.project = project;
        this.submitPanel = submitPanel;
        this.promptInputArea = promptInputArea;
        this.editorFileButtonManager = new EditorFileButtonManager(project, addFileBtn);
        this.devoxxGenieToolWindowContent = devoxxGenieToolWindowContent;

        this.projectContextController = new ProjectContextController(
                project, llmProvidersComboBox, modelNameComboBox, this);

        this.controller = new ActionButtonsPanelController(
                project, promptInputArea, promptOutputPanel,
                llmProvidersComboBox, modelNameComboBox, this,
                devoxxGenieToolWindowContent.getTabId()
        );
        this.controller.setToolWindowContent(devoxxGenieToolWindowContent);
        
        // Initialize the MCP tools manager
        this.mcpToolsManager = new MCPToolsManager(project);

        // Initialize the Agent toggle manager
        this.agentToggleManager = new AgentToggleManager();

        llmProvidersComboBox.addActionListener(e -> controller.updateButtonVisibility());

        // Call setupUI which will create the buttons before creating the button panel
        setupUI();
        setupAccessibility();

        // Apply initial button visibility based on settings
        controller.updateButtonVisibility();
    }

    private void setupUI() {
        createButtons();
        
        add(createProgressPanel(), BorderLayout.NORTH);
        add(createButtonPanel(), BorderLayout.CENTER);
    }

    private void createButtons() {
        submitBtn = createActionButton(SubmitIcon, SUBMIT_PROMPT_TOOLTIP, this::onSubmitPrompt);
        addFileBtn = new AddFilesToContextButton(project, this::addFileToConversationContext, this::showFilePickerPopup);
        addProjectBtn = createActionButton(AddProjectIcon, ADD_ENTIRE_PROJECT_TO_PROMPT_CONTEXT, this::handleProjectContext);
        calcTokenCostBtn = createActionButton(CalculateIcon, CALCULATE_TOKEN_COST_TOOLTIP, e -> controller.calculateTokensAndCost());
    }

    /** Adds a picked file to the chat panel's persistent context for the active tab. */
    private void addFileToConversationContext(@NotNull VirtualFile file) {
        FileListManager fileListManager = FileListManager.getInstance();
        String tabId = ConversationTabRegistry.getInstance().getActiveTabId(project);
        if (!fileListManager.contains(project, tabId, file)) {
            fileListManager.addFile(project, tabId, file);
        }
    }

    /**
     * Custom popup positioning that the chat panel has always used: the picker hovers
     * just above the prompt input, spanning the tool window's width, anchored at the
     * submit button. Passed to the reusable {@link AddFilesToContextButton} so the AP
     * panel can use the default (showUnderneathOf) instead.
     */
    private void showFilePickerPopup(@NotNull JBPopup popup,
                                     @NotNull AddFilesToContextButton button) {
        new ContextPopupMenu().show(submitBtn,
                popup,
                devoxxGenieToolWindowContent.getContentPanel().getSize().width,
                promptInputArea.getLocationOnScreen().y);
    }

    private @NotNull JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new BorderLayout(0, 0));

        // Main buttons using FlowLayout so hidden buttons leave no gaps
        JPanel mainButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        mainButtons.add(submitBtn);
        mainButtons.add(calcTokenCostBtn);
        mainButtons.add(addProjectBtn);
        mainButtons.add(addFileBtn);
        buttonPanel.add(mainButtons, BorderLayout.CENTER);
        
        // Agent toggle and MCP Tools counter on the right, vertically centered
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        rightPanel.add(agentToggleManager.getAgentToggleLabel());
        rightPanel.add(mcpToolsManager.getMcpToolsCountLabel());

        // GridBagLayout centers its child vertically by default
        JPanel rightWrapper = new JPanel(new GridBagLayout());
        rightWrapper.add(rightPanel);
        buttonPanel.add(rightWrapper, BorderLayout.EAST);
        
        return buttonPanel;
    }

    private @NotNull TokenUsageBar createTokenUsageBar() {
        TokenUsageBar tokenUsageBarInstance = new TokenUsageBar();
        tokenUsageBarInstance.setVisible(false);
        tokenUsageBarInstance.setPreferredSize(new Dimension(Integer.MAX_VALUE, 3));
        return tokenUsageBarInstance;
    }

    private @NotNull JPanel createCalcProjectPanel() {
        return new JPanel(new GridLayout(1, 2));
    }

    private @NotNull JPanel createProgressPanel() {
        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));

        // Persistent conversation context indicator (label + thin colored bar).
        conversationContextPanel.add(conversationContextLabel, BorderLayout.CENTER);
        conversationContextPanel.add(conversationContextBar, BorderLayout.SOUTH);
        conversationContextPanel.setVisible(false);
        progressPanel.add(conversationContextPanel);

        // Transient project-context preview bar (shown only while adding project to context).
        progressPanel.add(tokenUsageBar);
        return progressPanel;
    }

    private @NotNull JBLabel createConversationContextLabel() {
        JBLabel label = new JBLabel();
        label.setBorder(JBUI.Borders.empty(0, 2));
        Font base = label.getFont();
        label.setFont(base.deriveFont(base.getSize2D() - 1f));
        label.setForeground(JBColor.GRAY);
        return label;
    }

    private @NotNull TokenUsageBar createConversationContextBar() {
        TokenUsageBar bar = new TokenUsageBar();
        bar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 3));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        return bar;
    }

    /**
     * Sets the model's context window (max input tokens) used as the denominator of the
     * conversation context indicator. Called when the selected model changes. The used-token
     * count is preserved — switching models does not clear the conversation memory.
     */
    public void setConversationContextMax(int maxTokens) {
        this.conversationMaxTokens = maxTokens;
        ApplicationManager.getApplication().invokeLater(this::refreshConversationContext);
    }

    /**
     * Records the context occupied by the latest completed exchange (input + output tokens)
     * and refreshes the indicator.
     */
    public void updateConversationContextUsage(int usedTokens, int maxTokens) {
        this.conversationUsedTokens = usedTokens;
        if (maxTokens > 0) {
            this.conversationMaxTokens = maxTokens;
        }
        ApplicationManager.getApplication().invokeLater(this::refreshConversationContext);
    }

    /** Resets the conversation context indicator (e.g. when a new conversation starts). */
    public void resetConversationContext() {
        this.conversationUsedTokens = 0;
        ApplicationManager.getApplication().invokeLater(this::refreshConversationContext);
    }

    private void refreshConversationContext() {
        if (conversationMaxTokens <= 0) {
            // No known window (e.g. CLI/ACP/local providers) — hide the indicator entirely.
            conversationContextPanel.setVisible(false);
            return;
        }
        conversationContextPanel.setVisible(true);
        conversationContextBar.setTokens(conversationUsedTokens, conversationMaxTokens);

        int pct = (int) Math.round(conversationUsedTokens * 100.0 / conversationMaxTokens);
        String usedStr = WindowContextFormatterUtil.format(conversationUsedTokens).trim();
        String maxStr = WindowContextFormatterUtil.format(conversationMaxTokens).trim();
        conversationContextLabel.setText(String.format("Context window: %s / %s (%d%%)", usedStr, maxStr, pct));
        conversationContextLabel.setToolTipText(String.format(
                "This conversation uses %,d of %,d tokens (%d%%) of the model's context window",
                conversationUsedTokens, conversationMaxTokens, pct));
    }

    /**
     * Conversation completion hook (CONVERSATION_TOPIC). Updates the persistent context
     * indicator for this tab only, using the latest exchange's total token usage.
     */
    @Override
    public void onNewConversation(ChatMessageContext chatMessageContext) {
        String myTabId = devoxxGenieToolWindowContent.getTabId();
        String eventTabId = chatMessageContext.getTabId();
        if (myTabId != null && eventTabId != null && !myTabId.equals(eventTabId)) {
            return;
        }
        var tokenUsage = chatMessageContext.getTokenUsage();
        if (tokenUsage == null) {
            return;
        }
        int input = tokenUsage.inputTokenCount() != null ? tokenUsage.inputTokenCount() : 0;
        int output = tokenUsage.outputTokenCount() != null ? tokenUsage.outputTokenCount() : 0;
        LanguageModel model = chatMessageContext.getLanguageModel();
        int max = model != null ? model.getInputMaxTokens() : conversationMaxTokens;
        updateConversationContextUsage(input + output, max);
    }

    private void handleProjectContext(ActionEvent e) {
        if (projectContextController.isProjectContextAdded()) {
            confirmProjectContextRemoval();
        } else {
            projectContextController.addProjectContext();
        }
    }

    /**
     * Add files to the prompt context. Public so external callbacks (drag-and-drop on the
     * prompt input area) can trigger the same picker the button click does.
     */
    public void selectFilesForPromptContext() {
        addFileBtn.openPicker();
    }

    /**
     * Submit the user prompt.
     */
    private void onSubmitPrompt(ActionEvent actionEvent) {
        if (controller.isPromptRunning()) {
            controller.stopPromptExecution();
            return;
        }

        boolean response = controller.handlePromptSubmission(actionEvent.getActionCommand(),
                projectContextController.isProjectContextAdded(),
                projectContextController.getProjectContext());

        if (!response) {
            controller.endPromptExecution();
        }
    }

    public void enableButtons() {
        ApplicationManager.getApplication().invokeLater(() -> {
            submitBtn.setIcon(SubmitIcon);
            promptInputArea.setEnabled(true);
            // Stop the submit glow (and its Swing timer) on every execution-end path:
            // enableButtons() is reached on completion, error and user stop via
            // PromptExecutionController.endPromptExecution (TASK-235).
            submitPanel.stopGlowing();
            submitPendingSpecPrompt();

            // Notify the spec task runner that prompt execution completed so it can
            // start a grace timer and advance if the task wasn't marked Done.
            // Skip in CLI mode — the CLI process exit handles notification instead.
            SpecTaskRunnerService runner = SpecTaskRunnerService.getInstance(project);
            if (runner.isRunning() && !runner.isCliMode()) {
                runner.notifyPromptExecutionCompleted();
            }
        });
    }

    /**
     * If the spec task runner queued a prompt while the previous execution was still
     * streaming, submit it now that execution has finished.
     */
    private void submitPendingSpecPrompt() {
        if (pendingSpecPrompt == null) {
            return;
        }
        String prompt = pendingSpecPrompt;
        pendingSpecPrompt = null;

        // Only submit if the spec task runner is still active (not cancelled)
        SpecTaskRunnerService runner = SpecTaskRunnerService.getInstance(project);
        if (!runner.isRunning()) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() ->
                onPromptSubmitted(project, prompt, devoxxGenieToolWindowContent.getTabId()));
    }

    public void disableButtons() {
        promptInputArea.setEnabled(false);
    }

    public void disableSubmitBtn() {
        ApplicationManager.getApplication().invokeLater(() -> {
            submitBtn.setIcon(StopIcon);
            submitBtn.setToolTipText(PROMPT_IS_RUNNING_PLEASE_BE_PATIENT);
        });
    }

    public void updateAddProjectButton(boolean isProjectContextAdded) {
        updateAddProjectButton(isProjectContextAdded, 0);
    }

    public void updateAddProjectButton(boolean isProjectContextAdded, int tokenCount) {
        if (isProjectContextAdded) {
            addProjectBtn.setIcon(DeleteIcon);
            if (tokenCount > 0) {
                addProjectBtn.setToolTipText(REMOVE_ENTIRE_PROJECT_FROM_PROMPT_CONTEXT +
                        " (" + WindowContextFormatterUtil.format(tokenCount, "tokens") + ")");
            } else {
                addProjectBtn.setToolTipText(REMOVE_ENTIRE_PROJECT_FROM_PROMPT_CONTEXT);
            }
        } else {
            addProjectBtn.setIcon(AddProjectIcon);
            addProjectBtn.setToolTipText(ADD_ENTIRE_PROJECT_TO_PROMPT_CONTEXT);
        }
    }

    public void setAddProjectButtonEnabled(boolean enabled) {
        addProjectBtn.setEnabled(enabled);
    }

    public void setTokenUsageBarVisible(boolean visible) {
        tokenUsageBar.setVisible(visible);
    }

    public void resetTokenUsageBar() {
        ApplicationManager.getApplication().invokeLater(tokenUsageBar::reset);
    }

    @Override
    public void settingsChanged(boolean hasKey) {
        calcProjectPanel.setVisible(hasKey && projectContextController.isProjectContextSupportedProvider());
        controller.updateButtonVisibility();
        mcpToolsManager.updateMCPToolsCounter();
        agentToggleManager.updateAgentToggle();
    }

    public void updateTokenUsage(int maxTokens) {
        ApplicationManager.getApplication().invokeLater(() -> tokenUsageBar.setMaxTokens(maxTokens));
        setConversationContextMax(maxTokens);
    }

    @Override
    public void onPromptSubmitted(@NotNull Project projectPrompt, String prompt, @org.jetbrains.annotations.Nullable String tabId) {
        if (!this.project.getName().equals(projectPrompt.getName())) {
            return;
        }
        // Tab filter: if a tabId is specified, only the matching tab handles it.
        // If tabId is null, route to the active/selected tab only.
        String myTabId = devoxxGenieToolWindowContent.getTabId();
        if (tabId != null) {
            if (!tabId.equals(myTabId)) {
                return;
            }
        } else {
            // Null tabId means "route to active tab" — check if this tab is the active one
            String activeTabId = com.devoxx.genie.ui.window.ConversationTabRegistry.getInstance().getActiveTabId(project);
            if (activeTabId != null && !activeTabId.equals(myTabId)) {
                return;
            }
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            if (controller.isPromptRunning()) {
                // Don't stop the current execution — queue this prompt so the
                // current response finishes streaming to the user.
                pendingSpecPrompt = prompt;
                return;
            }
            promptInputArea.setText(prompt);
            onSubmitPrompt(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Constant.SUBMIT_ACTION));
        });
    }

    private void setupAccessibility() {
        submitBtn.getAccessibleContext().setAccessibleDescription("Submit prompt to AI");
        addFileBtn.getAccessibleContext().setAccessibleDescription("Add files to context");

        // Add keyboard mnemonics
        submitBtn.setMnemonic('S');
        addFileBtn.setMnemonic('A');
    }

    private void confirmProjectContextRemoval() {
        int result = Messages.showYesNoDialog(project,
                "Are you sure you want to remove the project context?",
                "Confirm Removal",
                Messages.getQuestionIcon()
        );
        if (result == Messages.YES) {
            projectContextController.resetProjectContext();
        }
    }

    public void setCalcTokenCostButtonVisible(boolean visible) {
        calcTokenCostBtn.setVisible(visible);
    }

    public void setAddProjectButtonVisible(boolean visible) {
        addProjectBtn.setVisible(visible);
    }

    public void setAddFileButtonVisible(boolean visible) {
        addFileBtn.setVisible(visible);
    }

    @Override
    public void startGlowing() {
        submitPanel.startGlowing();
    }

    @Override
    public void stopGlowing() {
        submitPanel.stopGlowing();
    }

    public void updateTokenUsageBar(int tokenCount, int tokenLimit) {
        ApplicationManager.getApplication().invokeLater(() -> tokenUsageBar.setTokens(tokenCount, tokenLimit));
    }

    @Override
    public void onTokenCalculationComplete(String message) {
        NotificationUtil.sendNotification(project, message);
    }
    
    /**
     * Override setEnabled to properly handle disabling all child components
     * when JCEF is not available.
     * 
     * @param enabled whether the component should be enabled
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        
        // Disable individual components
        submitBtn.setEnabled(enabled);
        addFileBtn.setEnabled(enabled);
        addProjectBtn.setEnabled(enabled);
        calcTokenCostBtn.setEnabled(enabled);
        
        // Update visual state
        if (!enabled) {
            submitBtn.setToolTipText("Prompt submission is disabled because JCEF is not available");
            addFileBtn.setToolTipText("File selection is disabled because JCEF is not available");
            addProjectBtn.setToolTipText("Project context is disabled because JCEF is not available");
            calcTokenCostBtn.setToolTipText("Token calculation is disabled because JCEF is not available");
        } else {
            submitBtn.setToolTipText(SUBMIT_PROMPT_TOOLTIP);
            addFileBtn.setToolTipText(ADD_FILES_TO_CONTEXT_TOOLTIP);
            addProjectBtn.setToolTipText(ADD_ENTIRE_PROJECT_TO_PROMPT_CONTEXT);
            calcTokenCostBtn.setToolTipText(CALCULATE_TOKEN_COST_TOOLTIP);
        }
    }
}
