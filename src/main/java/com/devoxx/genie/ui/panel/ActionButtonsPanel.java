package com.devoxx.genie.ui.panel;

import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.chatmodel.ChatModelProvider;
import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.service.*;
import com.devoxx.genie.ui.DevoxxGenieToolWindowContent;
import com.devoxx.genie.ui.EditorFileButtonManager;
import com.devoxx.genie.ui.component.ContextPopupMenu;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.component.PromptInputArea;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.EditorUtil;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import dev.langchain4j.data.message.UserMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.List;

import static com.devoxx.genie.model.Constant.*;
import static com.devoxx.genie.model.Constant.ADD_FILE_S_TO_PROMPT_CONTEXT;
import static com.devoxx.genie.ui.util.DevoxxGenieIcons.*;
import static javax.swing.SwingUtilities.invokeLater;

public class ActionButtonsPanel extends JPanel implements SettingsChangeListener {

    private static final Logger LOG = Logger.getInstance(ActionButtonsPanel.class);

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

    private final PromptInputArea promptInputComponent;
    private final PromptOutputPanel promptOutputPanel;
    private final ComboBox<String> llmProvidersComboBox;
    private final ComboBox<LanguageModel> modelNameComboBox;
    private final DevoxxGenieToolWindowContent devoxxGenieToolWindowContent;
    private final ChatModelProvider chatModelProvider = new ChatModelProvider();

    private boolean isPromptRunning = false;
    private boolean isProjectContextAdded = false;
    private ChatMessageContext currentChatMessageContext;
    private String projectContext;

    public ActionButtonsPanel(Project project,
                              PromptInputArea promptInputComponent,
                              PromptOutputPanel promptOutputPanel,
                              ComboBox<String> llmProvidersComboBox,
                              ComboBox<LanguageModel> modelNameComboBox,
                              DevoxxGenieToolWindowContent devoxxGenieToolWindowContent) {
        setLayout(new BorderLayout());

        this.project = project;
        this.promptInputComponent = promptInputComponent;
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
        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(FileSelectionPanelFactory.createPanel(project), null)
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
                promptInputComponent.getLocationOnScreen().y);
        }
    }

    /**
     * Submit the user prompt.
     */
    private void onSubmitPrompt(ActionEvent actionEvent) {
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
        String selectedProvider = (String) llmProvidersComboBox.getSelectedItem();
        return selectedProvider != null && (
            selectedProvider.equals(ModelProvider.OpenAI.getName()) ||
                selectedProvider.equals(ModelProvider.Anthropic.getName()) ||
                selectedProvider.equals(ModelProvider.Gemini.getName())
        );
    }

    /**
     * get the user prompt text.
     */
    private @Nullable String getUserPromptText() {
        String userPromptText = promptInputComponent.getText();
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

        currentChatMessageContext = createChatMessageContext(actionEvent, userPromptText);

        // Set the webSearchRequested flag based on the action command
        currentChatMessageContext.setWebSearchRequested(
            actionEvent.getActionCommand().equals(Constant.TAVILY_SEARCH_ACTION) ||
                actionEvent.getActionCommand().equals(Constant.GOOGLE_SEARCH_ACTION)
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
            promptInputComponent.setEnabled(true);
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
     * Get the chat message context.
     *
     * @param actionEvent the action event
     * @param userPrompt  the user prompt
     * @return the prompt context with language and text
     */
    private @NotNull ChatMessageContext createChatMessageContext(ActionEvent actionEvent,
                                                                 String userPrompt) {
        ChatMessageContext chatMessageContext = new ChatMessageContext();
        chatMessageContext.setProject(project);
        chatMessageContext.setName(String.valueOf(System.currentTimeMillis()));
        chatMessageContext.setUserPrompt(userPrompt);
        chatMessageContext.setUserMessage(UserMessage.userMessage(userPrompt));
        chatMessageContext.setLlmProvider((String) llmProvidersComboBox.getSelectedItem());

        LanguageModel selectedLanguageModel = (LanguageModel) modelNameComboBox.getSelectedItem();
        if (selectedLanguageModel != null) {
            chatMessageContext.setModelName(selectedLanguageModel.getName());
        }

        if (DevoxxGenieStateService.getInstance().getStreamMode() && actionEvent.getActionCommand().equals(Constant.SUBMIT_ACTION)) {
            chatMessageContext.setStreamingChatLanguageModel(chatModelProvider.getStreamingChatLanguageModel(chatMessageContext));
        } else {
            chatMessageContext.setChatLanguageModel(chatModelProvider.getChatLanguageModel(chatMessageContext));
        }

        setChatTimeout(chatMessageContext);

        setWindowContext(userPrompt, chatMessageContext);

        return chatMessageContext;
    }

    /**
     * Set the chat context based on
     * 1. The selected files & open file or selected code snippet
     * 2. Or full project context
     *
     * @param userPrompt         the user prompt
     * @param chatMessageContext the chat message context
     */
    private void setWindowContext(String userPrompt, ChatMessageContext chatMessageContext) {
        if (projectContext != null && isProjectContextAdded) {
            chatMessageContext.setContext(projectContext);
        } else {
            Editor selectedTextEditor = editorFileButtonManager.getSelectedTextEditor();

            // Add files to the context
            List<VirtualFile> files = FileListManager.getInstance().getFiles();
            if (!files.isEmpty()) {
                addSelectedFiles(chatMessageContext, userPrompt, files);
            }

            // Set the context based on the selected code snippet or the complete file
            if (selectedTextEditor != null) {
                addEditorInfo(userPrompt, selectedTextEditor, chatMessageContext);
            }
        }
    }

    /**
     * Add the selected code snippet to the chat message context.
     *
     * @param userPrompt         the user prompt
     * @param editor             the editor
     * @param chatMessageContext the chat message context
     */
    private void addEditorInfo(String userPrompt,
                               Editor editor,
                               @NotNull ChatMessageContext chatMessageContext) {
        EditorInfo editorInfo = generateEditorInformation(editor);
        chatMessageContext.setEditorInfo(editorInfo);
    }

    /**
     * Create the editor info based on the selected code snippet or the complete file.
     *
     * @param editor the editor
     * @return the editor info
     */
    private @NotNull EditorInfo generateEditorInformation(Editor editor) {
        EditorInfo editorInfo = EditorUtil.getEditorInfo(project, editor);
        String selectedText = editor.getSelectionModel().getSelectedText();

        // Add selected text
        if (selectedText != null) {
            editorInfo.setSelectedText(selectedText);
        } else {
            // Or add the complete file
            editorInfo.setSelectedText(editor.getDocument().getText());
            editorInfo.setSelectedFiles(List.of(editor.getVirtualFile()));
        }
        return editorInfo;
    }

    /**
     * Set the timeout for the chat message context.
     *
     * @param chatMessageContext the chat message context
     */
    private void setChatTimeout(ChatMessageContext chatMessageContext) {
        Integer timeout = DevoxxGenieStateService.getInstance().getTimeout();
        if (timeout == 0) {
            chatMessageContext.setTimeout(60);
        } else {
            chatMessageContext.setTimeout(timeout);
        }
    }

    /**
     * Add selected files to the chat message context.
     *
     * @param chatMessageContext the chat message context
     * @param userPrompt         the user prompt
     * @param files              the files
     */
    private void addSelectedFiles(@NotNull ChatMessageContext chatMessageContext,
                                  String userPrompt,
                                  List<VirtualFile> files) {

        chatMessageContext.setEditorInfo(new EditorInfo(files));

        MessageCreationService.getInstance().createUserPromptWithContextAsync(project, userPrompt, files)
            .thenAccept(chatMessageContext::setContext)
            .exceptionally(ex -> {
                ErrorHandler.handleError(project, ex);
                return null;
            });
    }

    /**
     * Disable the prompt input component.
     */
    private void disableButtons() {
        promptInputComponent.setEnabled(false);
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

        NotificationUtil.sendNotification(project, "Project context removed successfully");
    }

    private void addProjectToContext() {

        Object selectedItem = llmProvidersComboBox.getSelectedItem();
        if (selectedItem != null && ((String) selectedItem).isEmpty()) {
            NotificationUtil.sendNotification(project, "Please select a provider first");
            return;
        }

        if (!selectedItem.equals(ModelProvider.Gemini.getName()) &&
            !selectedItem.equals(ModelProvider.Anthropic.getName()) &&
            !selectedItem.equals(ModelProvider.OpenAI.getName())) {
            NotificationUtil.sendNotification(project,
                "This feature only works for OpenAI, Anthropic and Gemini providers because of the large token window context.");
            return;
        }

        new Task.Backgroundable(project, "Scanning Project", true) {
            String projectContent;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                projectContent = startScanning(indicator);
            }

            @Override
            public void onSuccess() {
                processScanningResult(projectContent);
            }

            @Override
            public void onFinished() {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(projectContent), null);

                isProjectContextAdded = true;
                addProjectBtn.setEnabled(true);
            }
        }.queue();

        addProjectBtn.setEnabled(false);
    }

    /**
     * Start scanning the project.
     *
     * @param indicator the progress indicator
     * @return the scanning result
     */
    private String startScanning(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        indicator.setText("Scanning project files...");
        String scanningResult = "";
        try {
            scanningResult = ProjectScannerService.getInstance().scanProject(project, getTokenLimit(), false).get();
        } catch (Exception e) {
            LOG.error("Failed to scan project", e);
        }
        return scanningResult;
    }

    /**
     * Get the token limit for the selected provider and model.
     *
     * @return the token limit
     */
    private int getTokenLimit() {
        ModelProvider selectedProvider = ModelProvider.fromString((String) llmProvidersComboBox.getSelectedItem());
        LanguageModel selectedItem = (LanguageModel) modelNameComboBox.getSelectedItem();
        int tokenLimit = 4096;
        if (selectedItem != null) {
            tokenLimit = ChatModelFactoryProvider.getFactoryByProvider(selectedProvider)
                .map(factory -> selectedItem.getMaxTokens())
                .orElse(4096);
        }
        return tokenLimit;
    }

    /**
     * Process the scanning result.
     *
     * @param projectContent the project content
     */
    private void processScanningResult(String projectContent) {
        if (projectContent != null && !projectContent.isEmpty()) {
            projectContext = "Project Context:\n" + projectContent;
            Encoding ENCODING = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
            int totalTokens = ENCODING.countTokens(projectContent);

            addProjectBtn.setIcon(DeleteIcon);
            addProjectBtn.setText("Full Project (" + NumberFormat.getInstance().format(totalTokens) + " tokens)");
            addProjectBtn.setToolTipText("Remove entire project from prompt context");
        } else {
            NotificationUtil.sendNotification(project, "Failed to add project context");
        }
    }

    @Override
    public void settingsChanged(boolean hasKey) {
        calcProjectPanel.setVisible(hasKey && isProjectContextSupportedProvider());
        updateAddProjectButtonVisibility();
    }

    /**
     * Calculate the tokens and cost for the entire project.
     */
    private void calculateTokensAndCost() {
        Object selectedItem = llmProvidersComboBox.getSelectedItem();
        if (selectedItem == null || ((String) selectedItem).isEmpty()) {
            NotificationUtil.sendNotification(project, "Please select a provider first");
            return;
        }

        LanguageModel selectedModel = (LanguageModel) modelNameComboBox.getSelectedItem();

        if (selectedModel == null) {
            NotificationUtil.sendNotification(project, "Please select a model first");
            return;
        }

        new Task.Backgroundable(project, "Calculating Tokens and Cost", true) {
            private int tokenCount;
            private double cost;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                String projectContent = startScanning(indicator, true);
                tokenCount = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE).countTokens(projectContent);
                cost = calculateCost(tokenCount, selectedModel.getCostPer1MTokensInput());
            }

            @Override
            public void onSuccess() {
                String message = String.format("Project contains %s tokens. Estimated minimum cost: $%.3f",
                    NumberFormat.getInstance().format(tokenCount), cost);
                NotificationUtil.sendNotification(project, message);
            }
        }.queue();
    }

    private String startScanning(@NotNull ProgressIndicator indicator, boolean isTokenCalculation) {
        indicator.setIndeterminate(true);
        indicator.setText("Scanning project files...");
        String scanningResult = "";
        try {
            scanningResult = ProjectScannerService.getInstance().scanProject(project, getTokenLimit(), isTokenCalculation).get();
        } catch (Exception e) {
            LOG.error("Failed to scan project", e);
        }
        return scanningResult;
    }

    private double calculateCost(int tokenCount, double costPer1000Tokens) {
        return (tokenCount / 1_000_000.0) * costPer1000Tokens;
    }
}
