package com.devoxx.genie.ui.panel;

import com.devoxx.genie.chatmodel.ChatModelProvider;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.ChatPromptExecutor;
import com.devoxx.genie.service.ProjectContentService;
import com.devoxx.genie.ui.DevoxxGenieToolWindowContent;
import com.devoxx.genie.ui.EditorFileButtonManager;
import com.devoxx.genie.ui.component.ContextPopupMenu;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.component.PromptInputArea;
import com.devoxx.genie.ui.component.TokenUsageBar;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.util.WindowContextFormatterUtil;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.EncodingType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static com.devoxx.genie.model.Constant.*;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.*;
import static javax.swing.SwingUtilities.invokeLater;

public class ActionButtonsPanel extends JPanel implements SettingsChangeListener {

    private final Project project;

    private final ChatPromptExecutor chatPromptExecutor;
    private final EditorFileButtonManager editorFileButtonManager;
    private final JPanel calcProjectPanel = new JPanel(new GridLayout(1, 2));

    private final JButton addFileBtn = new JHoverButton(AddFileIcon, true);
    private final JButton submitBtn = new JHoverButton(SubmitIcon, true);
    private final JButton tavilySearchBtn = new JHoverButton(WebSearchIcon, true);
    private final JButton googleSearchBtn = new JHoverButton(GoogleIcon, true);
    private final JButton addProjectBtn = new JHoverButton("Add full project to prompt", AddFileIcon, true);
    private final JButton calcTokenCostBtn = new JHoverButton("Calc tokens/cost", CalculateIcon, true);

    private final PromptInputArea promptInputArea;
    private final PromptOutputPanel promptOutputPanel;
    private final ComboBox<ModelProvider> llmProvidersComboBox;
    private final ComboBox<LanguageModel> modelNameComboBox;
    private final TokenUsageBar tokenUsageBar = new TokenUsageBar();
    private final JProgressBar progressBar = new JProgressBar();
    private int tokenCount;

    private final DevoxxGenieToolWindowContent devoxxGenieToolWindowContent;
    private final ChatModelProvider chatModelProvider = new ChatModelProvider();

    private boolean isPromptRunning = false;
    private boolean isProjectContextAdded = false;
    private ChatMessageContext currentChatMessageContext;
    private String projectContext;

    public ActionButtonsPanel(Project project,
                              PromptInputArea promptInputArea,
                              PromptOutputPanel promptOutputPanel,
                              ComboBox<ModelProvider> llmProvidersComboBox,
                              ComboBox<LanguageModel> modelNameComboBox,
                              DevoxxGenieToolWindowContent devoxxGenieToolWindowContent) {
        setLayout(new BorderLayout());

        this.project = project;
        this.promptInputArea = promptInputArea;
        this.promptOutputPanel = promptOutputPanel;
        this.chatPromptExecutor = new ChatPromptExecutor();
        this.editorFileButtonManager = new EditorFileButtonManager(project, addFileBtn);
        this.llmProvidersComboBox = llmProvidersComboBox;
        this.modelNameComboBox = modelNameComboBox;
        this.devoxxGenieToolWindowContent = devoxxGenieToolWindowContent;
        this.llmProvidersComboBox.addActionListener(e -> updateAddProjectButtonVisibility());

        ApplicationManager.getApplication().getMessageBus()
            .connect()
            .subscribe(AppTopics.SETTINGS_CHANGED_TOPIC, this);

        setupUI();

        setupAddProjectButton();
        configureSearchButtonsVisibility();
    }

    private void updateAddProjectButtonVisibility() {
        calcProjectPanel.setVisible(isProjectContextSupportedProvider());
    }

    /**
     * Create the Action button panel with Submit, the Web Search and Add file buttons.
     */
    public void setupUI() {

        submitBtn.setToolTipText(SUBMIT_THE_PROMPT);
        submitBtn.setActionCommand(Constant.SUBMIT_ACTION);
        submitBtn.addActionListener(this::onSubmitPrompt);
        add(submitBtn, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout());
        createSearchButton(searchPanel, tavilySearchBtn, TAVILY_SEARCH_ACTION, SEARCH_THE_WEB_WITH_TAVILY_FOR_AN_ANSWER);
        createSearchButton(searchPanel, googleSearchBtn, GOOGLE_SEARCH_ACTION, SEARCH_GOOGLE_FOR_AN_ANSWER);
        add(searchPanel, BorderLayout.CENTER);

        addFileBtn.setToolTipText(ADD_FILE_S_TO_PROMPT_CONTEXT);
        addFileBtn.addActionListener(this::selectFilesForPromptContext);

        add(addFileBtn, BorderLayout.EAST);
        add(addProjectBtn, BorderLayout.SOUTH);

        progressBar.setVisible(false);
        progressBar.setIndeterminate(true);

        tokenUsageBar.setVisible(false);
        tokenUsageBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 3));

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.add(tokenUsageBar, BorderLayout.CENTER);
        progressPanel.add(progressBar, BorderLayout.SOUTH);
        add(progressPanel, BorderLayout.NORTH);
    }

    /**
     * Create the search button.
     *
     * @param panel        the panel
     * @param searchBtn    the search button
     * @param searchAction the search action
     * @param tooltipText  the tooltip text
     */
    private void createSearchButton(@NotNull JPanel panel,
                                    @NotNull JButton searchBtn,
                                    String searchAction,
                                    String tooltipText) {
        searchBtn.setMaximumSize(new Dimension(30, 30));
        searchBtn.setActionCommand(searchAction);
        searchBtn.setToolTipText(tooltipText);
        searchBtn.addActionListener(this::onSubmitPrompt);
        panel.add(searchBtn);
    }

    /**
     * Add files to the prompt context.
     */
    private void selectFilesForPromptContext(ActionEvent e) {
        java.util.List<VirtualFile> openFiles = editorFileButtonManager.getOpenFiles();

        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(FileSelectionPanelFactory.createPanel(project, openFiles), null)
            .setTitle("Filter and Double-Click To Add To Prompt Context")
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
        progressBar.setVisible(true);

        if (isPromptRunning) {
            stopPromptExecution();
            return;
        }

        if (!validateAndPreparePrompt(actionEvent)) {
            return;
        }

        executePrompt();
    }

    /**
     * Execute the prompt.
     */
    private void executePrompt() {
        disableUIForPromptExecution();

        chatPromptExecutor.updatePromptWithCommandIfPresent(currentChatMessageContext, promptOutputPanel)
            .ifPresentOrElse(
                command -> startPromptExecution(),
                this::enableButtons
            );
    }

    /**
     * Start the prompt execution.
     */
    private void startPromptExecution() {
        isPromptRunning = true;
        chatPromptExecutor.executePrompt(currentChatMessageContext, promptOutputPanel, this::enableButtons);
    }

    /**
     * Stop the prompt execution.
     */
    private void stopPromptExecution() {
        chatPromptExecutor.stopPromptExecution();
        isPromptRunning = false;
        enableButtons();
    }

    public void resetProjectContext() {
        projectContext = null;
        isProjectContextAdded = false;
        if (currentChatMessageContext != null) {
            currentChatMessageContext.setContext(null);
            currentChatMessageContext.setFullProjectContextAdded(false);
        }
        updateAddProjectButton();
    }

    private void updateAddProjectButton() {
        if (isProjectContextAdded) {
            addProjectBtn.setIcon(DeleteIcon);
            addProjectBtn.setText("Remove full project from prompt");
            addProjectBtn.setToolTipText("Remove entire project from prompt context");
        } else {
            addProjectBtn.setIcon(AddFileIcon);
            addProjectBtn.setText("Add full project to prompt");
            addProjectBtn.setToolTipText("Add entire project to prompt context");
        }
    }

    private boolean isProjectContextSupportedProvider() {
        ModelProvider selectedProvider = (ModelProvider) llmProvidersComboBox.getSelectedItem();
        return selectedProvider != null && (
            selectedProvider.equals(ModelProvider.OpenAI) ||
            selectedProvider.equals(ModelProvider.Anthropic) ||
            selectedProvider.equals(ModelProvider.Gemini)
        );
    }

    /**
     * get the user prompt text.
     */
    private @Nullable String getUserPromptText() {
        String userPromptText = promptInputArea.getText();
        if (userPromptText.isEmpty()) {
            NotificationUtil.sendNotification(project, "Please enter a prompt.");
            return null;
        }
        return userPromptText;
    }

    /**
     * Disable the UI for prompt execution.
     */
    private void disableUIForPromptExecution() {
        disableSubmitBtn();
        disableButtons();
    }

    /**
     * Validate and prepare the prompt.
     *
     * @param actionEvent the action event
     * @return true if the prompt is valid
     */
    private boolean validateAndPreparePrompt(ActionEvent actionEvent) {
        String userPromptText = getUserPromptText();
        if (userPromptText == null) {
            return false;
        }

        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        LanguageModel selectedLanguageModel = (LanguageModel) modelNameComboBox.getSelectedItem();

        currentChatMessageContext = ChatMessageContextUtil.createContext(
            project,
            userPromptText,
            selectedLanguageModel,
            chatModelProvider,
            stateService,
            actionEvent.getActionCommand(),
            editorFileButtonManager,
            projectContext,
            isProjectContextAdded
        );

        return true;
    }

    /**
     * Enable the prompt input component and reset the Submit button icon.
     */
    public void enableButtons() {
        SwingUtilities.invokeLater(() -> {
            submitBtn.setIcon(SubmitIcon);
            submitBtn.setToolTipText(SUBMIT_THE_PROMPT);
            progressBar.setVisible(false);
            promptInputArea.setEnabled(true);
            isPromptRunning = false;
        });
    }

    /**
     * Disable the Submit button.
     */
    private void disableSubmitBtn() {
        invokeLater(() -> {
            submitBtn.setIcon(StopIcon);
            submitBtn.setToolTipText(PROMPT_IS_RUNNING_PLEASE_BE_PATIENT);
        });
    }

    /**
     * Disable the prompt input component.
     */
    private void disableButtons() {
        promptInputArea.setEnabled(false);
    }

    /**
     * Set the search buttons visibility based on settings.
     */
    public void configureSearchButtonsVisibility() {
        if (DevoxxGenieStateService.getInstance().getHideSearchButtonsFlag()) {
            tavilySearchBtn.setVisible(false);
            googleSearchBtn.setVisible(false);
        } else {
            tavilySearchBtn.setVisible(!DevoxxGenieStateService.getInstance().getTavilySearchKey().isEmpty());
            googleSearchBtn.setVisible(!DevoxxGenieStateService.getInstance().getGoogleSearchKey().isEmpty() &&
                !DevoxxGenieStateService.getInstance().getGoogleCSIKey().isEmpty());
        }
    }

    /**
     * Setup the Add Project button.
     */
    private void setupAddProjectButton() {
        addProjectBtn.setToolTipText("Add entire project to prompt context");
        addProjectBtn.addActionListener(e -> toggleProjectContext());

        calcTokenCostBtn.setToolTipText("Calculate tokens and cost for the entire project");
        calcTokenCostBtn.addActionListener(e -> calculateTokensAndCost());

        calcProjectPanel.add(calcTokenCostBtn);
        calcProjectPanel.add(addProjectBtn);
        add(calcProjectPanel, BorderLayout.SOUTH);

        updateAddProjectButtonVisibility();
    }

    /**
     * Add the project source code to the prompt context.
     */
    private void toggleProjectContext() {
        if (isProjectContextAdded) {
            removeProjectContext();
        } else {
            addProjectToContext();
        }
    }

    private void removeProjectContext() {
        projectContext = null;
        isProjectContextAdded = false;

        addProjectBtn.setIcon(AddFileIcon);
        addProjectBtn.setText("Add full project to prompt");
        addProjectBtn.setToolTipText("Add entire project to prompt context");

        resetTokenUsageBar();
        tokenCount = 0;

        NotificationUtil.sendNotification(project, "Project context removed successfully");
    }

    private void addProjectToContext() {
        ModelProvider modelProvider = (ModelProvider) llmProvidersComboBox.getSelectedItem();
        if (modelProvider == null) {
            NotificationUtil.sendNotification(project, "Please select a provider first");
            return;
        }

        if (!modelProvider.equals(ModelProvider.Gemini) &&
            !modelProvider.equals(ModelProvider.Anthropic) &&
            !modelProvider.equals(ModelProvider.OpenAI)) {
            NotificationUtil.sendNotification(project,
                "This feature only works for OpenAI, Anthropic and Gemini providers because of the large token window context.");
            return;
        }

        addProjectBtn.setEnabled(false);
        tokenUsageBar.setVisible(true);
        tokenUsageBar.reset();

        int tokenLimit = getWindowContext();

        ProjectContentService.getInstance().getProjectContent(project, tokenLimit, false)
            .thenAccept(projectContent -> {
                projectContext = "Project Context:\n" + projectContent;
                isProjectContextAdded = true;
                SwingUtilities.invokeLater(() -> {
                    addProjectBtn.setIcon(DeleteIcon);
                    tokenCount = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE).countTokens(projectContent);
                    addProjectBtn.setText("Full Project (" + WindowContextFormatterUtil.format(tokenCount, "tokens") + ")");
                    addProjectBtn.setToolTipText("Remove entire project from prompt context");
                    addProjectBtn.setEnabled(true);

                    tokenUsageBar.setTokens(tokenCount, tokenLimit);
                });
            })
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                    addProjectBtn.setEnabled(true);
                    tokenUsageBar.setVisible(false);
                    NotificationUtil.sendNotification(project, "Error adding project content: " + ex.getMessage());
                });
                return null;
            });
    }

    /**
     * Get the window context for the selected provider and model.
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

    @Override
    public void settingsChanged(boolean hasKey) {
        calcProjectPanel.setVisible(hasKey && isProjectContextSupportedProvider());
        updateAddProjectButtonVisibility();
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

        if (!DefaultLLMSettingsUtil.isApiBasedProvider(selectedProvider)) {
            NotificationUtil.sendNotification(project, "Cost calculation is not applicable for local providers");
            return;
        }

        ProjectContentService.getInstance().calculateTokensAndCost(
            project,
            getWindowContext(),
            selectedProvider,
            selectedModel
        );
    }

    public void updateTokenUsage(int maxTokens) {
        SwingUtilities.invokeLater(() -> tokenUsageBar.setMaxTokens(maxTokens));
    }

    public void resetTokenUsageBar() {
        SwingUtilities.invokeLater(() -> {
            tokenUsageBar.reset();
            tokenCount = 0;
        });
    }
}
