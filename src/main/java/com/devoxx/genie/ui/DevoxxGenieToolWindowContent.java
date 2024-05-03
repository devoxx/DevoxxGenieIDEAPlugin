package com.devoxx.genie.ui;

import com.devoxx.genie.chatmodel.ChatModelProvider;
import com.devoxx.genie.chatmodel.anthropic.AnthropicChatModelFactory;
import com.devoxx.genie.chatmodel.deepinfra.DeepInfraChatModelFactory;
import com.devoxx.genie.chatmodel.groq.GroqChatModelFactory;
import com.devoxx.genie.chatmodel.mistral.MistralChatModelFactory;
import com.devoxx.genie.chatmodel.ollama.OllamaChatModelFactory;
import com.devoxx.genie.chatmodel.openai.OpenAIChatModelFactory;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.model.request.PromptContext;
import com.devoxx.genie.service.PromptExecutionService;
import com.devoxx.genie.ui.component.*;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.devoxx.genie.ui.util.EditorUtil;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.devoxx.genie.chatmodel.LLMProviderConstant.getLLMProviders;
import static com.devoxx.genie.ui.util.DevoxxGenieIcons.*;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * The Devoxx Genie Tool Window Content.
 */
public class DevoxxGenieToolWindowContent implements SettingsChangeListener {

    public static final String WORKING_MESSAGE = "working.message";

    private final Project project;
    private final ResourceBundle resourceBundle = ResourceBundle.getBundle("messages");
    private final ChatModelProvider genieClient = new ChatModelProvider();
    private final FileEditorManager fileEditorManager;

    @Getter
    private final JPanel contentPanel = new JPanel();
    private final ComboBox<String> llmProvidersComboBox = new ComboBox<>();
    private final ComboBox<String> modelNameComboBox = new ComboBox<>();

    private PromptInputComponent promptInputComponent;
    private PromptOutputPanel promptOutputPanel;
    private PromptContextFileListPanel promptContextFileListPanel;

    private final JButton configBtn = new JHoverButton(CogIcon, true);
    private final JButton submitBtn = new JHoverButton(SubmitIcon, true);
    private final JButton addFileBtn = new JHoverButton(AddFileIcon, true);
    private final JButton historyBtn = new JHoverButton(ClockIcon, true);
    private final JButton newConversationBtn = new JHoverButton(PlusIcon, true);

    private final PromptExecutionService promptExecutionService;
    private String lastSelectedProvider;

    /**
     * The Devoxx Genie Tool Window Content constructor.
     *
     * @param toolWindow the tool window
     */
    public DevoxxGenieToolWindowContent(ToolWindow toolWindow) {
        project = toolWindow.getProject();
        fileEditorManager = FileEditorManager.getInstance(project);
        promptExecutionService = PromptExecutionService.getInstance();

        setupUI();

        addLLMProvidersToComboBox();

        setLastSelectedProvider();
    }

    private @Nullable void setLastSelectedProvider() {
        lastSelectedProvider = SettingsState.getInstance().getLastSelectedProvider();
        if (lastSelectedProvider != null) {
            llmProvidersComboBox.setSelectedItem(lastSelectedProvider);
        }
    }

    private void setupUI() {
        promptInputComponent = new PromptInputComponent(project, resourceBundle);
        promptOutputPanel = new PromptOutputPanel(project, resourceBundle);
        promptContextFileListPanel = new PromptContextFileListPanel(project);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(createSelectionPanel(), BorderLayout.NORTH);
        topPanel.add(createConversationPanel(), BorderLayout.CENTER);

        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(topPanel, BorderLayout.NORTH);

        Splitter splitter = new Splitter(true, 0.8f);
        splitter.setFirstComponent(promptOutputPanel);
        splitter.setSecondComponent(createInputPanel());
        splitter.setHonorComponentsMinimumSize(true);

        contentPanel.add(splitter, BorderLayout.CENTER);
    }

    /**
     * Refresh the LLM providers dropdown because the Settings have been changed.
     */
    public void settingsChanged() {
        llmProvidersComboBox.removeAllItems();
        addLLMProvidersToComboBox();
    }

    /**
     * Add the LLM providers to combobox.
     * Only show the cloud-based LLM providers for which we have an API Key.
     */
    private void addLLMProvidersToComboBox() {
        getLLMProviders().forEach(llmProvidersComboBox::addItem);
    }

    /**
     * Create the conversation panel.
     *
     * @return the conversation panel
     */
    @NotNull
    private JPanel createConversationPanel() {

        JPanel conversationPanel = new JPanel(new BorderLayout());
        conversationPanel.setPreferredSize(new Dimension(0, 30));
        conversationPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel newConversationLabel = new JLabel("New conversation " + getCurrentTimestamp());
        newConversationLabel.setForeground(JBColor.GRAY);
        newConversationLabel.setPreferredSize(new Dimension(0, 30));

        newConversationBtn.setPreferredSize(new Dimension(25, 30));
        configBtn.setPreferredSize(new Dimension(25, 30));

        JPanel conversationButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        conversationButtonPanel.add(newConversationBtn);
        conversationButtonPanel.add(configBtn);
        conversationButtonPanel.setPreferredSize(new Dimension(60, 30));
        conversationButtonPanel.setMinimumSize(new Dimension(60, 30));

        historyBtn.setToolTipText("Show chat history");
        newConversationBtn.setToolTipText("Start a new conversation");
        configBtn.setToolTipText("Plugin settings");

        configBtn.addActionListener(e -> showSettingsDialog());
        newConversationBtn.addActionListener(e -> {
            newConversationLabel.setText("New conversation " + getCurrentTimestamp());
            promptOutputPanel.clear();
            promptExecutionService.clearChatMessages();
            enableButtons();
        });

        conversationPanel.add(newConversationLabel, BorderLayout.CENTER);
        conversationPanel.add(conversationButtonPanel, BorderLayout.EAST);

        return conversationPanel;
    }

    private static @NotNull String getCurrentTimestamp() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM ''yy HH:mm");
        return dateTime.format(formatter);
    }

    /**
     * Create the LLM and model name selection panel.
     *
     * @return the selection panel
     */
    @NotNull
    private JPanel createSelectionPanel() {
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));

        JPanel providerPanel = new JPanel(new BorderLayout(), true);
        providerPanel.add(llmProvidersComboBox, BorderLayout.CENTER);

        toolPanel.add(providerPanel);
        toolPanel.add(Box.createVerticalStrut(5));
        toolPanel.add(modelNameComboBox);

        llmProvidersComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, llmProvidersComboBox.getPreferredSize().height));
        modelNameComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, modelNameComboBox.getPreferredSize().height));

        llmProvidersComboBox.addActionListener(e -> handleModelProviderSelectionChange());
        modelNameComboBox.addActionListener(e -> processModelNameSelection());

        return toolPanel;
    }

    /**
     * Show the settings dialog.
     */
    private void showSettingsDialog() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Devoxx Genie Settings");
    }

    /**
     * Create the input panel.
     *
     * @return the input panel
     */
    @NotNull
    private JPanel createInputPanel() {

        addFileBtn.setToolTipText("Add file(s) to prompt context");
        submitBtn.setToolTipText("Submit the prompt");

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(submitBtn, BorderLayout.WEST);
        buttonPanel.add(addFileBtn, BorderLayout.EAST);

        // Disable addFileBtn if no files are open in the IDEA Editor
        if (fileEditorManager.getSelectedFiles().length == 0) {
            addFileBtn.setEnabled(false);
            addFileBtn.setToolTipText("No files open in the editor");
        }

        // Add listener to enable addFileBtn when files are opened in the IDEA Editor
        ApplicationManager.getApplication().getMessageBus().connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                @Override
                public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    addFileBtn.setEnabled(true);
                    addFileBtn.setToolTipText("Select file(s) for prompt context");
                }

                @Override
                public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    if (fileEditorManager.getSelectedFiles().length == 0) {
                        addFileBtn.setEnabled(false);
                        addFileBtn.setToolTipText("No files open in the editor");
                    }
                }
            });

        JPanel submitPanel = new JPanel(new BorderLayout());
        submitPanel.setMinimumSize(new Dimension(Integer.MAX_VALUE, 100));
        submitPanel.add(promptContextFileListPanel, BorderLayout.NORTH);
        submitPanel.add(new JBScrollPane(promptInputComponent), BorderLayout.CENTER);
        submitPanel.add(new JBScrollPane(buttonPanel), BorderLayout.SOUTH);

        submitBtn.addActionListener(e -> onSubmitPrompt());
        addFileBtn.addActionListener(e -> selectFilesForPromptContext());

        return submitPanel;
    }

    /**
     * Add files to the prompt context.
     */
    private void selectFilesForPromptContext() {
        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(FileSelectionPanelFactory.createPanel(project), null)
            .setTitle("Double-Click To Add To Prompt Context")
            .setRequestFocus(true)
            .setResizable(true)
            .setMovable(false)
            .createPopup();

        if (addFileBtn.isShowing()) {
            new ContextPopupMenu().show(submitBtn,
                popup,
                this.getContentPanel().getSize().width,
                this.promptInputComponent.getLocationOnScreen().y);
        }
    }

    /**
     * Submit the user prompt.
     */
    private void onSubmitPrompt() {
        invokeLater(() -> {
            submitBtn.setIcon(StopIcon);
            submitBtn.setToolTipText("Prompt is running, please be patient...");
        });

        String userPromptText = promptInputComponent.getText();

        if (userPromptText.isEmpty()) {
            return;
        }

        PromptContext promptContext = getPromptContext(userPromptText,
            promptContextFileListPanel.getFiles(),
            fileEditorManager.getSelectedTextEditor(),
            genieClient.getChatLanguageModel());

        disableButtons();

        runPromptInBackground(promptContext);
    }

    /**
     * Run the prompt in the background
     *
     * @param promptContext the prompt context
     */
    private void runPromptInBackground(PromptContext promptContext) {

        Task.Backgroundable task =
            new Task.Backgroundable(promptContext.getProject(), resourceBundle.getString(WORKING_MESSAGE), true) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    promptOutputPanel.addUserPrompt(promptContext.getUserPrompt());

                    executePrompt(promptContext);

                    progressIndicator.setText(resourceBundle.getString(WORKING_MESSAGE));
                }
            };
        task.queue();

        // TODO Check if user cancels the prompt, if se we can cancel/kill the task
    }

    /**
     * Execute the prompt.
     *
     * @param promptContext the prompt context
     */
    private void executePrompt(PromptContext promptContext) {
        disableButtons();

        getCommandFromPrompt(promptContext.getUserPrompt()).ifPresentOrElse(fixedPrompt -> {
            try {
                promptContext.setUserPrompt(fixedPrompt);

                String response = promptExecutionService.executeQuery(promptContext);

                promptOutputPanel.addChatResponse(promptContext, response);
            } catch (IllegalAccessException e) {
                promptOutputPanel.addWarningText(promptContext, e.getMessage());
            }
            enableButtons();
        }, this::enableButtons);
    }

    /**
     * Get the command from the prompt.
     *
     * @param prompt the prompt
     * @return the command
     */
    private Optional<String> getCommandFromPrompt(String prompt) {
        if (prompt.startsWith("/")) {
            SettingsState settings = SettingsState.getInstance();

            if (prompt.equalsIgnoreCase("/test")) {
                prompt = settings.getTestPrompt();
            } else if (prompt.equalsIgnoreCase("/review")) {
                prompt = settings.getReviewPrompt();
            } else if (prompt.equalsIgnoreCase("/explain")) {
                prompt = settings.getExplainPrompt();
            } else if (prompt.equalsIgnoreCase("/custom")) {
                prompt = settings.getCustomPrompt();
            } else {
                promptOutputPanel.showHelpText();
                return Optional.empty();
            }
        }
        return Optional.of(prompt);
    }

    /**
     * Get the prompt context
     *
     * @param files  the files
     * @param editor the editor
     * @return the prompt context with language and text
     */
    private PromptContext getPromptContext(String userPrompt,
                                           List<VirtualFile> files,
                                           Editor editor,
                                           ChatLanguageModel chatLanguageModel) {
        PromptContext promptContext = new PromptContext();
        promptContext.setProject(project);
        promptContext.setUserPrompt(userPrompt);
        promptContext.setChatLanguageModel(chatLanguageModel);
        promptContext.setModelName((String) modelNameComboBox.getSelectedItem());
        promptContext.setLlmProvider((String) llmProvidersComboBox.getSelectedItem());

        if (files.isEmpty() && editor == null) {
            // A regular prompt without any selected code or files
            return promptContext;
        } else if (files.isEmpty() && editor.getSelectionModel().getSelectedText() != null) {
            // A prompt with selected code in the editor
            EditorInfo editorInfo = EditorUtil.getEditorInfo(project, editor);
            promptContext.setEditorInfo(editorInfo);
            return promptContext;
        } else if (!files.isEmpty()) {
            // A prompt with selected files
            return getPromptContextWithSelectedFiles(promptContext, userPrompt, files);
        } else {
            // A prompt with alla the content from open editor
            EditorInfo editorInfo = new EditorInfo();
            editorInfo.setSelectedText(editor.getDocument().getText());
            promptContext.setEditorInfo(editorInfo);
            return promptContext;
        }
    }

    /**
     * Get the prompt context from the selected files.
     *
     * @param userPrompt the user prompt
     * @param files      the files
     * @return the prompt context
     */
    private @NotNull PromptContext getPromptContextWithSelectedFiles(PromptContext promptContext,
                                                                     String userPrompt,
                                                                     List<VirtualFile> files) {
        promptContext.setEditorInfo(new EditorInfo(files));
        promptContext.setContext(getUserPromptWithContext(userPrompt, files));
        return promptContext;
    }

    /**
     * Get user prompt with context.
     *
     * @param userPrompt the user prompt
     * @param files      the files
     * @return the user prompt with context
     */
    private @NotNull String getUserPromptWithContext(String userPrompt, List<VirtualFile> files) {
        StringBuilder userPromptContext = new StringBuilder();
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        files.forEach(file -> ApplicationManager.getApplication().runReadAction(() -> {
            Document document = fileDocumentManager.getDocument(file);
            if (document != null) {
                userPromptContext.append("Filename: ").append(file.getName()).append("\n");
                String content = document.getText();
                userPromptContext.append(content).append("\n");
            } else {
                NotificationUtil.sendNotification(project, "Error reading file: " + file.getName());
            }
        }));

        userPromptContext.append(userPrompt);
        return userPromptContext.toString();
    }

    private void disableButtons() {
        submitBtn.setEnabled(false);
        promptInputComponent.setEnabled(false);
    }

    private void enableButtons() {
        submitBtn.setIcon(SubmitIcon);
        promptInputComponent.setEnabled(true);
        submitBtn.setEnabled(true);
    }

    /**
     * Process the model name selection.
     */
    private void processModelNameSelection() {
        String selectedModel = (String) modelNameComboBox.getSelectedItem();
        if (selectedModel != null) {
            genieClient.setModelName(selectedModel);
            SettingsState.getInstance().setLastSelectedModel(selectedModel);
        }
    }

    /**
     * Process the model provider selection change.
     * Set the model provider and update the model names.
     */
    private void handleModelProviderSelectionChange() {
        String selectedProvider = (String) llmProvidersComboBox.getSelectedItem();
        if (selectedProvider != null) {
            ModelProvider provider = ModelProvider.valueOf(selectedProvider);
            genieClient.setModelProvider(ModelProvider.valueOf(selectedProvider));
            modelNameComboBox.setVisible(true);
            modelNameComboBox.removeAllItems();

            if (lastSelectedProvider != null && !lastSelectedProvider.equalsIgnoreCase(selectedProvider)) {
                SettingsState.getInstance().setLastSelectedProvider(selectedProvider);
            }

            switch (provider) {
                case Ollama:
                    new OllamaChatModelFactory().getModelNames().forEach(modelNameComboBox::addItem);
                    break;
                case OpenAI:
                    new OpenAIChatModelFactory().getModelNames().forEach(modelNameComboBox::addItem);
                    break;
                case Anthropic:
                    new AnthropicChatModelFactory().getModelNames().forEach(modelNameComboBox::addItem);
                    break;
                case Mistral:
                    new MistralChatModelFactory().getModelNames().forEach(modelNameComboBox::addItem);
                    break;
                case Groq:
                    new GroqChatModelFactory().getModelNames().forEach(modelNameComboBox::addItem);
                    break;
                case DeepInfra:
                    new DeepInfraChatModelFactory().getModelNames().forEach(modelNameComboBox::addItem);
                    break;
                case LMStudio, GPT4All:
                    modelNameComboBox.setVisible(false);
                    break;
                default:
                    break;
            }
        }
    }
}
