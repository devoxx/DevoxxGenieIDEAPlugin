package com.devoxx.genie.ui.panel;

import com.devoxx.genie.controller.ActionButtonsPanelController;
import com.devoxx.genie.controller.ProjectContextController;
import com.devoxx.genie.controller.listener.TokenCalculationListener;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.DevoxxGenieToolWindowContent;
import com.devoxx.genie.ui.EditorFileButtonManager;
import com.devoxx.genie.ui.component.ContextPopupMenu;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.component.TokenUsageBar;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.listener.GlowingListener;
import com.devoxx.genie.ui.listener.PromptSubmissionListener;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.util.WindowContextFormatterUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.devoxx.genie.model.Constant.*;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.*;

public class ActionButtonsPanel extends JPanel
        implements SettingsChangeListener, PromptSubmissionListener, GlowingListener, TokenCalculationListener {

    private final transient Project project;

    private final transient EditorFileButtonManager editorFileButtonManager;

    private JButton addFileBtn;
    private JButton submitBtn;
    private JButton addProjectBtn;
    private JButton calcTokenCostBtn;

    private final SubmitPanel submitPanel;

    private final JPanel calcProjectPanel = createCalcProjectPanel();

    // Set minimum size for buttons to prevent them from becoming too small
    private final Dimension minSize = new Dimension(100, 30);
    private final Dimension maxSize = new Dimension(200, 30);

    private final PromptInputArea promptInputArea;
    private final TokenUsageBar tokenUsageBar = createTokenUsageBar();

    private final transient DevoxxGenieToolWindowContent devoxxGenieToolWindowContent;

    private final transient ActionButtonsPanelController controller;
    private final transient ProjectContextController projectContextController;

    public ActionButtonsPanel(Project project,
                              SubmitPanel submitPanel,
                              PromptInputArea promptInputArea,
                              PromptOutputPanel promptOutputPanel,
                              ComboBox<ModelProvider> llmProvidersComboBox,
                              ComboBox<LanguageModel> modelNameComboBox,
                              DevoxxGenieToolWindowContent devoxxGenieToolWindowContent) {
        setLayout(new BorderLayout());
        // setBorder(JBUI.Borders.empty(10));

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

        llmProvidersComboBox.addActionListener(e -> controller.updateButtonVisibility());

        // Call setupUI which will create the buttons before creating the button panel
        setupUI();
        setupAccessibility();
        setupMessageBus();
    }

    private void setupUI() {
        createButtons();

        add(createProgressPanel(), BorderLayout.NORTH);
        add(createButtonPanel(), BorderLayout.CENTER);
    }

    private void createButtons() {
        submitBtn = createSubmitButton();
        addFileBtn = createAddFileButton();
        addProjectBtn = createAddProjectButton();
        calcTokenCostBtn = createCalcTokenCostButton();
    }

    private @NotNull JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 5, 0));
        buttonPanel.add(submitBtn);
        buttonPanel.add(calcTokenCostBtn);
        buttonPanel.add(addProjectBtn);
        buttonPanel.add(addFileBtn);
        return buttonPanel;
    }

    private @NotNull TokenUsageBar createTokenUsageBar() {
        TokenUsageBar tokenUsageBar = new TokenUsageBar();
        tokenUsageBar.setVisible(false);
        tokenUsageBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 3));
        return tokenUsageBar;
    }

    private @NotNull JPanel createCalcProjectPanel() {
        return new JPanel(new GridLayout(1, 2));
    }

    private @NotNull JPanel createProgressPanel() {
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.add(tokenUsageBar, BorderLayout.CENTER);
        return progressPanel;
    }

    private @NotNull JButton createCalcTokenCostButton() {
        JButton button = new JHoverButton(CALC_TOKENS_COST, CalculateIcon, true);
        button.addActionListener(e -> controller.calculateTokensAndCost());
        button.setMinimumSize(minSize);
        button.setMaximumSize(maxSize);
        return button;
    }

    private @NotNull JButton createAddProjectButton() {
        JButton button = new JHoverButton(ADD_PROJECT_TO_CONTEXT, AddFileIcon, true);
        button.addActionListener(this::handleProjectContext);
        button.setMinimumSize(minSize);
        button.setMaximumSize(maxSize);
        return button;
    }

    private void handleProjectContext(ActionEvent e) {
        if (projectContextController.isProjectContextAdded()) {
            confirmProjectContextRemoval();
        } else {
            projectContextController.addProjectContext();
        }
    }

    private @NotNull JButton createAddFileButton() {
        JButton button = new JHoverButton(AddFileIcon, false);
        button.addActionListener(this::selectFilesForPromptContext);
        button.setMinimumSize(minSize);
        button.setMaximumSize(maxSize);
        return button;
    }

    private @NotNull JButton createSubmitButton() {
        JButton button = new JHoverButton(SubmitIcon, false);
        button.setActionCommand(Constant.SUBMIT_ACTION);
        button.addActionListener(this::onSubmitPrompt);
        button.setMinimumSize(minSize);
        button.setMaximumSize(maxSize);
        return button;
    }

    /**
     * Add files to the prompt context.
     */
    private void selectFilesForPromptContext(ActionEvent e) {
        java.util.List<VirtualFile> openFiles = editorFileButtonManager.getOpenFiles();
        List<VirtualFile> sortedFiles = new ArrayList<>(openFiles);
        sortedFiles.sort(Comparator.comparing(VirtualFile::getName, String.CASE_INSENSITIVE_ORDER));

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(FileSelectionPanelFactory.createPanel(project, sortedFiles), null)
                .setTitle(FILTER_AND_DOUBLE_CLICK_TO_ADD_TO_PROMPT_CONTEXT)
                .setRequestFocus(true)
                .setResizable(true)
                .setMovable(false)
                .setMinSize(new Dimension(300, 350))
                .createPopup();

        if (addFileBtn.isShowing()) {
            new ContextPopupMenu().show(submitBtn,
                    popup,
                    devoxxGenieToolWindowContent.getSize().width,
                    promptInputArea.getLocationOnScreen().y);
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
        });
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

    private void setupMessageBus() {
        MessageBusConnection messageBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
        messageBusConnection.subscribe(AppTopics.SETTINGS_CHANGED_TOPIC, this);
        messageBusConnection.subscribe(AppTopics.PROMPT_SUBMISSION_TOPIC, this);
    }

    public void resetProjectContext() {
        projectContextController.resetProjectContext();
    }

    public void updateAddProjectButton(boolean isProjectContextAdded) {
        updateAddProjectButton(isProjectContextAdded, 0);
    }

    public void updateAddProjectButton(boolean isProjectContextAdded, int tokenCount) {
        if (isProjectContextAdded) {
            setAddProjectButton(DeleteIcon, REMOVE_CONTEXT, REMOVE_ENTIRE_PROJECT_FROM_PROMPT_CONTEXT);
            if (tokenCount > 0) {
                addProjectBtn.setText(WindowContextFormatterUtil.format(tokenCount, "tokens"));
            }
        } else {
            setAddProjectButton(AddFileIcon, ADD_PROJECT_TO_CONTEXT, ADD_ENTIRE_PROJECT_TO_PROMPT_CONTEXT);
        }
    }

    private void setAddProjectButton(Icon addFileIcon, String addProjectToContext, String addEntireProjectToPromptContext) {
        addProjectBtn.setIcon(addFileIcon);
        addProjectBtn.setText(addProjectToContext);
        addProjectBtn.setToolTipText(addEntireProjectToPromptContext);
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
}