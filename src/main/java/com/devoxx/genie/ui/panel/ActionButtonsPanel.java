package com.devoxx.genie.ui.panel;

import com.devoxx.genie.controller.ActionButtonsPanelController;
import com.devoxx.genie.controller.ProjectContextController;
import com.devoxx.genie.controller.listener.TokenCalculationListener;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.agent.AgentToggleManager;
import com.devoxx.genie.ui.mcp.MCPToolsManager;
import com.devoxx.genie.service.spec.SpecTaskRunnerService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.window.DevoxxGenieToolWindowContent;
import com.devoxx.genie.ui.component.button.EditorFileButtonManager;
import com.devoxx.genie.ui.component.ContextPopupMenu;
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
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.devoxx.genie.model.Constant.*;
import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.*;

public class ActionButtonsPanel extends JPanel
        implements SettingsChangeListener, PromptSubmissionListener, GlowingListener, TokenCalculationListener {

    private final transient Project project;

    @Getter
    private final transient EditorFileButtonManager editorFileButtonManager;

    private JButton addFileBtn;
    private JButton submitBtn;
    private JButton addProjectBtn;
    private JButton calcTokenCostBtn;

    private final SubmitPanel submitPanel;
    private final JPanel calcProjectPanel = createCalcProjectPanel();
    private final PromptInputArea promptInputArea;
    private final TokenUsageBar tokenUsageBar = createTokenUsageBar();
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
                llmProvidersComboBox, modelNameComboBox, this
        );
        
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
        addFileBtn = createActionButton(AddFileIcon, ADD_FILES_TO_CONTEXT_TOOLTIP, this::selectFilesForPromptContext);
        addProjectBtn = createActionButton(AddProjectIcon, ADD_ENTIRE_PROJECT_TO_PROMPT_CONTEXT, this::handleProjectContext);
        calcTokenCostBtn = createActionButton(CalculateIcon, CALCULATE_TOKEN_COST_TOOLTIP, e -> controller.calculateTokensAndCost());
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
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.add(tokenUsageBar, BorderLayout.CENTER);
        return progressPanel;
    }

    private void handleProjectContext(ActionEvent e) {
        if (projectContextController.isProjectContextAdded()) {
            confirmProjectContextRemoval();
        } else {
            projectContextController.addProjectContext();
        }
    }

    /**
     * Add files to the prompt context.
     */
    public void selectFilesForPromptContext() {
        selectFilesForPromptContext(null);
    }
    
    /**
     * Add files to the prompt context.
     */
    private void selectFilesForPromptContext(ActionEvent e) {
        java.util.List<VirtualFile> openFiles = editorFileButtonManager.getOpenFiles();
        List<VirtualFile> sortedFiles = new ArrayList<>(openFiles);
        sortedFiles.sort(Comparator.comparing(VirtualFile::getName, String.CASE_INSENSITIVE_ORDER));

        JPanel fileSelectionPanel = FileSelectionPanelFactory.createPanel(project, sortedFiles);
        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(fileSelectionPanel, null)
                .setTitle(FILTER_AND_DOUBLE_CLICK_TO_ADD_TO_PROMPT_CONTEXT)
                .setRequestFocus(true)
                .setResizable(true)
                .setMovable(false)
                .setMinSize(new Dimension(300, 350))
                .createPopup();

        if (addFileBtn.isShowing()) {
            new ContextPopupMenu().show(submitBtn,
                    popup,
                    devoxxGenieToolWindowContent.getContentPanel().getSize().width,
                    promptInputArea.getLocationOnScreen().y);
                    
            // Focus the filter field after the popup is shown
            SwingUtilities.invokeLater(() -> {
                Component focusableComponent = findFocusableComponent(fileSelectionPanel);
                if (focusableComponent != null) {
                    focusableComponent.requestFocusInWindow();
                }
            });
        }
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
                onPromptSubmitted(project, prompt));
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
    }

    @Override
    public void onPromptSubmitted(@NotNull Project projectPrompt, String prompt) {
        if (!this.project.getName().equals(projectPrompt.getName())) {
            return;
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
     * Find the first focusable component (text field) in the panel
     *
     * @param panel the panel to search in
     * @return the first focusable text field found, or null if none found
     */
    private Component findFocusableComponent(Container panel) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JTextField && component.isFocusable()) {
                return component;
            }
            if (component instanceof Container container) {
                Component found = findFocusableComponent(container);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
