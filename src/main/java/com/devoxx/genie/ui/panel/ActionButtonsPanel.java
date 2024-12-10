package com.devoxx.genie.ui.panel;

import com.devoxx.genie.controller.ActionPanelController;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.ProjectContentService;
import com.devoxx.genie.service.TokenCalculationService;
import com.devoxx.genie.ui.DevoxxGenieToolWindowContent;
import com.devoxx.genie.ui.EditorFileButtonManager;
import com.devoxx.genie.ui.component.ContextPopupMenu;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.component.TokenUsageBar;
import com.devoxx.genie.ui.listener.PromptSubmissionListener;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.util.WindowContextFormatterUtil;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.JBUI;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.EncodingType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.devoxx.genie.model.Constant.*;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.*;

public class ActionButtonsPanel extends JPanel implements SettingsChangeListener, PromptSubmissionListener {

    private final transient Project project;

    private final transient EditorFileButtonManager editorFileButtonManager;
    private final JPanel calcProjectPanel = new JPanel(new GridLayout(1, 2));

    private final JButton addFileBtn = new JHoverButton(AddFileIcon, false);
    private final JButton submitBtn = new JHoverButton(SubmitIcon, false);
    private final JButton addProjectBtn = new JHoverButton(ADD_PROJECT_TO_CONTEXT, AddFileIcon, true);
    private final JButton calcTokenCostBtn = new JHoverButton(CALC_TOKENS_COST, CalculateIcon, true);
    private final JPanel mainContent = new JPanel(new BorderLayout());

    private final PromptInputArea promptInputArea;
    private final ComboBox<ModelProvider> llmProvidersComboBox;
    private final ComboBox<LanguageModel> modelNameComboBox;
    private final TokenUsageBar tokenUsageBar = new TokenUsageBar();
    private int tokenCount;

    private final transient DevoxxGenieToolWindowContent devoxxGenieToolWindowContent;

    private boolean isProjectContextAdded = false;
    private String projectContext;

    private final transient TokenCalculationService tokenCalculationService;
    private final transient ActionPanelController controller;

    private final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

    public ActionButtonsPanel(Project project,
                              PromptInputArea promptInputArea,
                              PromptOutputPanel promptOutputPanel,
                              ComboBox<ModelProvider> llmProvidersComboBox,
                              ComboBox<LanguageModel> modelNameComboBox,
                              DevoxxGenieToolWindowContent devoxxGenieToolWindowContent) {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(10));

        // Initialize fields and components
        this.project = project;
        this.promptInputArea = promptInputArea;
        this.editorFileButtonManager = new EditorFileButtonManager(project, addFileBtn);
        this.llmProvidersComboBox = llmProvidersComboBox;
        this.modelNameComboBox = modelNameComboBox;
        this.devoxxGenieToolWindowContent = devoxxGenieToolWindowContent;
        this.tokenCalculationService = new TokenCalculationService();

        this.controller = new ActionPanelController(
                project, promptInputArea, promptOutputPanel,
                llmProvidersComboBox, modelNameComboBox, this
        );

        this.llmProvidersComboBox.addActionListener(e -> updateButtonVisibility());

        setupUI();
        setupAccessibility();
        setupMessageBus();
    }

    private void setupUI() {

        // Setup token usage bar
        tokenUsageBar.setVisible(false);
        tokenUsageBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 3));
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.add(tokenUsageBar, BorderLayout.CENTER);
        add(progressPanel, BorderLayout.NORTH);

        // Configure buttons
        setupButtons();

        // Add button panel to main content
        mainContent.add(buttonPanel, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);
    }

    private void setupButtons() {
        // Configure Submit button
        submitBtn.setToolTipText(SUBMIT_THE_PROMPT + SHIFT_ENTER);
        submitBtn.setActionCommand(Constant.SUBMIT_ACTION);
        submitBtn.addActionListener(this::onSubmitPrompt);

        // Configure Add File button
        addFileBtn.setToolTipText(ADD_FILE_S_TO_PROMPT_CONTEXT);
        addFileBtn.addActionListener(this::selectFilesForPromptContext);

        calcTokenCostBtn.setToolTipText(CALCULATE_TOKENS_COST);
        calcTokenCostBtn.addActionListener(e -> calculateTokensAndCost());

        addProjectBtn.setToolTipText(ADD_ENTIRE_PROJECT_TO_PROMPT_CONTEXT);
        addProjectBtn.addActionListener(e -> {
            if (isProjectContextAdded) {
                confirmProjectContextRemoval();
            } else {
                addProjectToContext();
            }
        });

        buttonPanel.setLayout(new GridLayout(1, 4, 5, 0));

        // Add buttons with horizontal glue between them
        buttonPanel.add(submitBtn);
        buttonPanel.add(calcTokenCostBtn);
        buttonPanel.add(addProjectBtn);
        buttonPanel.add(addFileBtn);

        // Set minimum size for buttons to prevent them from becoming too small
        Dimension minSize = new Dimension(100, 30);
        submitBtn.setMinimumSize(minSize);
        calcTokenCostBtn.setMinimumSize(minSize);
        addProjectBtn.setMinimumSize(minSize);
        addFileBtn.setMinimumSize(minSize);

        // Set maximum size to prevent buttons from growing too large
        Dimension maxSize = new Dimension(200, 30);
        submitBtn.setMaximumSize(maxSize);
        calcTokenCostBtn.setMaximumSize(maxSize);
        addProjectBtn.setMaximumSize(maxSize);
        addFileBtn.setMaximumSize(maxSize);
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
                devoxxGenieToolWindowContent.getContentPanel().getSize().width,
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

        disableUIForPromptExecution();

        boolean response = controller.executePrompt(actionEvent.getActionCommand(), isProjectContextAdded, projectContext);
        if (!response) {
            enableButtons();
        }
    }

    private void disableUIForPromptExecution() {
        disableSubmitBtn();
        disableButtons();
        promptInputArea.startGlowing();
    }

    public void enableButtons() {
        ApplicationManager.getApplication().invokeLater(() -> {
            submitBtn.setIcon(SubmitIcon);
            submitBtn.setToolTipText(SUBMIT_THE_PROMPT + " (Ctrl+Enter)");
            promptInputArea.setEnabled(true);
            promptInputArea.stopGlowing();
        });
    }

    private void disableSubmitBtn() {
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

    @Override
    public void setSize(@NotNull Dimension dimension) {
        super.setSize(dimension);
        revalidateLayout();
    }

    private void revalidateLayout() {
        if (getWidth() < 400) {
            buttonPanel.setLayout(new GridLayout(0, 1, 0, 5)); // Stack vertically when narrow
        } else {
            buttonPanel.setLayout(new GridBagLayout()); // Single row when wide
        }
        buttonPanel.revalidate();
    }

    private void disableButtons() {
        promptInputArea.setEnabled(false);
    }

    public void resetProjectContext() {
        updateAddProjectButton();
    }

    private void updateAddProjectButton() {
        if (isProjectContextAdded) {
            addProjectBtn.setIcon(DeleteIcon);
            addProjectBtn.setText(REMOVE_CONTEXT);
            addProjectBtn.setToolTipText(REMOVE_ENTIRE_PROJECT_FROM_PROMPT_CONTEXT);
        } else {
            addProjectBtn.setIcon(AddFileIcon);
            addProjectBtn.setText(ADD_PROJECT_TO_CONTEXT);
            addProjectBtn.setToolTipText(ADD_ENTIRE_PROJECT_TO_PROMPT_CONTEXT);
        }
    }

    /**
     * Check if the selected provider supports project context.
     * Included also Ollama because of the Llama 3.1 release with a window context of 128K.
     *
     * @return true if the provider supports project context
     */
    private boolean isProjectContextSupportedProvider() {
        ModelProvider selectedProvider = (ModelProvider) llmProvidersComboBox.getSelectedItem();
        return selectedProvider != null && isSupportedProvider(selectedProvider);
    }

    private void removeProjectContext() {
        projectContext = null;
        isProjectContextAdded = false;

        addProjectBtn.setIcon(AddFileIcon);
        addProjectBtn.setText(ADD_PROJECT_TO_CONTEXT);
        addProjectBtn.setToolTipText(ADD_ENTIRE_PROJECT_TO_PROMPT_CONTEXT);

        resetTokenUsageBar();
        tokenCount = 0;

        NotificationUtil.sendNotification(project, "Project context removed successfully");
    }

    private boolean isSupportedProvider(@NotNull ModelProvider modelProvider) {
        return modelProvider.equals(ModelProvider.Google) ||
            modelProvider.equals(ModelProvider.Anthropic) ||
            modelProvider.equals(ModelProvider.OpenAI) ||
            modelProvider.equals(ModelProvider.Mistral) ||
            modelProvider.equals(ModelProvider.DeepSeek) ||
            modelProvider.equals(ModelProvider.OpenRouter) ||
            modelProvider.equals(ModelProvider.DeepInfra) ||
            modelProvider.equals(ModelProvider.Ollama);
    }

    private void addProjectToContext() {
        ModelProvider modelProvider = (ModelProvider) llmProvidersComboBox.getSelectedItem();
        if (modelProvider == null) {
            NotificationUtil.sendNotification(project, "Please select a provider first");
            return;
        }

        if (!isSupportedProvider(modelProvider)) {
            NotificationUtil.sendNotification(project,
                "This feature only works for OpenAI, Anthropic, Gemini and Ollama providers because of the large token window context.");
            return;
        }

        addProjectBtn.setEnabled(false);
        tokenUsageBar.setVisible(true);
        tokenUsageBar.reset();

        int tokenLimit = getWindowContext();

        ProjectContentService.getInstance().getProjectContent(project, tokenLimit, false)
            .thenAccept(projectContent -> {
                projectContext = "Project Context:\n" + projectContent.getContent();
                isProjectContextAdded = true;
                ApplicationManager.getApplication().invokeLater(() -> {
                    addProjectBtn.setIcon(DeleteIcon);
                    tokenCount = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE).countTokens(projectContent.getContent());
                    addProjectBtn.setText(WindowContextFormatterUtil.format(tokenCount, "tokens"));
                    addProjectBtn.setToolTipText(REMOVE_ENTIRE_PROJECT_FROM_PROMPT_CONTEXT);
                    addProjectBtn.setEnabled(true);

                    tokenUsageBar.setTokens(tokenCount, tokenLimit);
                });
            })
            .exceptionally(ex -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    addProjectBtn.setEnabled(true);
                    tokenUsageBar.setVisible(false);
                    NotificationUtil.sendNotification(project, "Error adding project content: " + ex.getMessage());
                });
                return null;
            });
    }

    /**
     * Get the window context for the selected provider and model.
     *
     * @return the token limit
     */
    private int getWindowContext() {
        LanguageModel languageModel = (LanguageModel) modelNameComboBox.getSelectedItem();
        int tokenLimit = 4096;
        if (languageModel != null) {
            tokenLimit = languageModel.getContextWindow();
        }
        return tokenLimit;
    }

    private void updateButtonVisibility() {
        boolean isSupported = isProjectContextSupportedProvider();
        calcTokenCostBtn.setVisible(isSupported);
        addProjectBtn.setVisible(isSupported);
    }

    @Override
    public void settingsChanged(boolean hasKey) {
        calcProjectPanel.setVisible(hasKey && isProjectContextSupportedProvider());
        updateButtonVisibility();
    }

    private void calculateTokensAndCost() {
        LanguageModel selectedModel = (LanguageModel) modelNameComboBox.getSelectedItem();
        if (selectedModel == null) {
            NotificationUtil.sendNotification(project, "Please select a model first");
            return;
        }

        ModelProvider selectedProvider = (ModelProvider) llmProvidersComboBox.getSelectedItem();
        if (selectedProvider == null) {
            NotificationUtil.sendNotification(project, "Please select a provider first");
            return;
        }

        int maxTokens = selectedModel.getContextWindow();

        tokenCalculationService.calculateTokensAndCost(
            project,
            null,
            maxTokens,
            selectedProvider,
            selectedModel,
            DefaultLLMSettingsUtil.isApiKeyBasedProvider(selectedProvider));
    }

    public void updateTokenUsage(int maxTokens) {
        ApplicationManager.getApplication().invokeLater(() -> tokenUsageBar.setMaxTokens(maxTokens));
    }

    public void resetTokenUsageBar() {
        ApplicationManager.getApplication().invokeLater(() -> {
            tokenUsageBar.reset();
            tokenCount = 0;
        });
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
        );     if (result == Messages.YES) {
            removeProjectContext();
        }
    }
}
