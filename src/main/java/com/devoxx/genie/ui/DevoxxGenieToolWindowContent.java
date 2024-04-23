package com.devoxx.genie.ui;

import com.devoxx.genie.DevoxxGenieClient;
import com.devoxx.genie.chatmodel.anthropic.AnthropicChatModelFactory;
import com.devoxx.genie.chatmodel.deepinfra.DeepInfraChatModelFactory;
import com.devoxx.genie.chatmodel.groq.GroqChatModelFactory;
import com.devoxx.genie.chatmodel.mistral.MistralChatModelFactory;
import com.devoxx.genie.chatmodel.ollama.OllamaChatModelFactory;
import com.devoxx.genie.chatmodel.openai.OpenAIChatModelFactory;
import com.devoxx.genie.model.ChatInteraction;
import com.devoxx.genie.model.LanguageTextPair;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.platform.logger.GenieLogger;
import com.devoxx.genie.service.ChatHistoryObserver;
import com.devoxx.genie.service.ChatMessageHistoryService;
import com.devoxx.genie.ui.component.ContextPopupMenu;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.component.PlaceholderTextArea;
import com.devoxx.genie.ui.util.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;

import static com.devoxx.genie.chatmodel.LLMProviderConstant.getLLMProviders;
import static com.devoxx.genie.ui.CommandHandler.*;
import static com.devoxx.genie.ui.util.DevoxxGenieIcons.*;

/**
 * The Devoxx Genie Tool Window Content.
 */
public class DevoxxGenieToolWindowContent implements CommandHandlerListener,
                                                     ChatHistoryObserver,
                                                     SettingsChangeListener {

    private static final GenieLogger log = new GenieLogger(DevoxxGenieToolWindowContent.class);

    public static final String WORKING_MESSAGE = "working.message";

    private final Project project;
    private final ResourceBundle resourceBundle = ResourceBundle.getBundle("messages");
    private final DevoxxGenieClient genieClient = DevoxxGenieClient.getInstance();
    private final ChatMessageHistoryService chatMessageHistoryService = new ChatMessageHistoryService();
    private final FileEditorManager fileEditorManager;
    @Getter
    private final JPanel contentPanel = new JPanel();
    private final ComboBox<String> llmProvidersComboBox = new ComboBox<>();
    private final ComboBox<String> modelNameComboBox = new ComboBox<>();
    private final JButton previousInteractionBtn = new JButton("<");
    private final JButton nextInteractionBtn = new JButton(">");
    private final JButton configBtn = new JButton("＋");

    private final PlaceholderTextArea promptInputArea = new PlaceholderTextArea(3, 80);
    private final JEditorPane promptOutputArea = new JEditorPane("text/html", WelcomeUtil.getWelcomeText(resourceBundle));

    private final JButton historyBtn = new JButton(ClockIcon);
    private final JButton submitBtn = new JHoverButton(SubmitIcon);
    private final JButton addContextBtn = new JButton(PlusIcon);

    private final CommandHandler commandHandler = new CommandHandler(this);

    /**
     * The Devoxx Genie Tool Window Content constructor.
     * @param toolWindow the tool window
     */
    public DevoxxGenieToolWindowContent(ToolWindow toolWindow) {
        project = toolWindow.getProject();
        fileEditorManager = FileEditorManager.getInstance(project);

        setupUI();

        chatMessageHistoryService.addObserver(this);

//        updateActionButtonStates();
        addLLMProvidersToComboBox();
        handleModelProviderSelectionChange();
    }

    private void setupUI() {
        contentPanel.setLayout(new BorderLayout(0, 10));
        contentPanel.add(createSelectionPanel(), BorderLayout.NORTH);
        contentPanel.add(createOutputPanel(), BorderLayout.CENTER);
        contentPanel.add(createInputPanel(), BorderLayout.SOUTH);
    }

    /**
     * Refresh the LLM providers dropdown because the Settings have been changed.
     */
    public void settingsChanged() {
        llmProvidersComboBox.removeAllItems();
        addLLMProvidersToComboBox();
        updateChatMemorySize();
    }

    private static void updateChatMemorySize() {
        SettingsState settingState = SettingsState.getInstance();
        if (DevoxxGenieClient.getInstance().getChatMemorySize() != settingState.getMaxMemory()) {
            DevoxxGenieClient.getInstance().setChatMemorySize(settingState.getMaxMemory());
        }
    }

    /**
     * Add the LLM providers to combobox.
     * Only show the cloud-based LLM providers for which we have an API Key.
     */
    private void addLLMProvidersToComboBox() {
        getLLMProviders().forEach(llmProvidersComboBox::addItem);
    }

    /**
     * Create the LLM and model name selection panel.
     * @return the selection panel
     */
    @NotNull
    private JPanel createSelectionPanel() {
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));

//        toolBar.add(historyBtn);
//        providerPanel.add(toolBar, BorderLayout.NORTH);

        JPanel providerPanel = new JPanel();
        providerPanel.setLayout(new BorderLayout());
        providerPanel.add(configBtn, BorderLayout.WEST);
        providerPanel.add(llmProvidersComboBox, BorderLayout.CENTER);

        toolPanel.add(providerPanel);
        toolPanel.add(Box.createVerticalStrut(5));
        toolPanel.add(modelNameComboBox);

        llmProvidersComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, llmProvidersComboBox.getPreferredSize().height));
        modelNameComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, modelNameComboBox.getPreferredSize().height));

        configBtn.addActionListener(e -> showSettingsDialog());
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
     * Create the output panel.
     * @return the output panel
     */
    @NotNull
    private JPanel createOutputPanel() {
        JPanel outputPanel = new JPanel(new BorderLayout());

        // Output Area - This should expand to fill most of the space
        promptOutputArea.setEditable(false);
        promptOutputArea.setContentType("text/html");

        JScrollPane outputScrollPane = new JBScrollPane(promptOutputArea);
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);
        return outputPanel;
    }

    /**
     * Create the input panel.
     * @return the input panel
     */
    @NotNull
    private JPanel createInputPanel() {

        promptInputArea.setLineWrap(true);
        promptInputArea.setWrapStyleWord(true);
        promptInputArea.setPlaceholder(resourceBundle.getString("prompt.placeholder"));

        addContextBtn.setToolTipText("Add context to the prompt");
        submitBtn.setToolTipText("Submit the prompt");
        submitBtn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(submitBtn, BorderLayout.WEST);
        buttonPanel.add(addContextBtn, BorderLayout.EAST);

        JPanel submitPanel = new JPanel(new BorderLayout());
        submitPanel.add(new JBScrollPane(promptInputArea), BorderLayout.CENTER);
        submitPanel.add(new JBScrollPane(buttonPanel), BorderLayout.SOUTH);

        submitBtn.addActionListener(e -> onSubmit());
        addContextBtn.addActionListener(e -> addCodeToPromptContext());
        previousInteractionBtn.addActionListener(e -> chatMessageHistoryService.setPreviousMessage());
        nextInteractionBtn.addActionListener(e -> chatMessageHistoryService.setNextMessage());

        return submitPanel;
    }

    /**
     * Add code to the prompt context.
     */
    private void addCodeToPromptContext() {
        // TODO Show list of open files and allow user to select one or more to add to the prompt context
        JBPopup popup = JBPopupFactory.getInstance().createPopupChooserBuilder(List.of("File1", "File2"))
            .setTitle("Select Files To Add To Prompt Context")
            .setItemChosenCallback(selectedItem -> promptInputArea.append("\n\n" + selectedItem))
            .createPopup();

        if (addContextBtn.isShowing()) {
            new ContextPopupMenu().show(submitBtn, popup);
        } else {
            System.out.println("addContextBtn is not visible or not properly initialized.");
        }
    }

    /**
     * Show the help message.
     */
    public void showHelpMsg() {
        promptOutputArea.setText(HelpUtil.getHelpMessage(resourceBundle));
    }

    /**
     * Submit the user prompt.
     */
    private void onSubmit() {

        String prompt = promptInputArea.getText();

        if (prompt.isEmpty()) {
            log.info("No prompt entered");
            return;
        }

        Editor editor = fileEditorManager.getSelectedTextEditor();
        if (editor == null) {
            log.info("No editor selected");
            NotificationUtil.sendNotification(project, resourceBundle.getString("no.editor"));
            return;
        }

        disableButtons();

        promptOutputArea.setText(WorkingMessage.getWorkingMessage());

        if (executeCommand(prompt)) return;

        Task.Backgroundable task = new Task.Backgroundable(project, resourceBundle.getString(WORKING_MESSAGE), true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setText(resourceBundle.getString(WORKING_MESSAGE));
                executePrompt("", promptInputArea.getText());
            }
        };
        task.queue();
    }

    private boolean executeCommand(String prompt) {
        if (prompt.startsWith("/")) {
            if (prompt.equalsIgnoreCase(COMMAND_HELP) ||
                prompt.equalsIgnoreCase(COMMAND_TEST) ||
                prompt.equalsIgnoreCase(COMMAND_REVIEW) ||
                prompt.equalsIgnoreCase(COMMAND_EXPLAIN) ||
                prompt.equalsIgnoreCase(COMMAND_CUSTOM)) {
                commandHandler.handleCommand(prompt);
            } else {
                promptOutputArea.setText(HelpUtil.getHelpMessage(resourceBundle));
                NotificationUtil.sendNotification(project, resourceBundle.getString("command.unknown") + prompt);
            }
            enableButtons();
            return true;
        }
        return false;
    }

    private void disableButtons() {
        submitBtn.setEnabled(false);
        promptInputArea.setEnabled(false);
    }

    private void enableButtons() {
        promptInputArea.setEnabled(true);
        submitBtn.setEnabled(true);
    }

    /**
     * Execute the user prompt.
     * @param userPrompt the user prompt
     */
    @Override
    public void executePrompt(String command, String userPrompt) {
        new Task.Backgroundable(project, resourceBundle.getString(WORKING_MESSAGE), true) {

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                Editor editor = fileEditorManager.getSelectedTextEditor();
                if (editor == null) {
                    NotificationUtil.sendNotification(project, resourceBundle.getString("no.editor"));
                    return;
                }
                LanguageTextPair languageAndText = EditorUtil.getEditorLanguageAndText(editor);

                String response = genieClient.executeGeniePrompt(userPrompt, languageAndText);
                String htmlResponse = updateUIWithResponse(response);

                chatMessageHistoryService.addMessage(llmProvidersComboBox.getSelectedItem() == null ? "" : llmProvidersComboBox.getSelectedItem().toString(),
                    modelNameComboBox.getSelectedItem() == null ? "" : modelNameComboBox.getSelectedItem().toString(),
                    command.isEmpty() ? userPrompt : command,
                    htmlResponse);
            }
        }.queue();
    }

    /**
     * Update the UI with the response.  Convert the response to markdown to improve readability.
     * @param response the response
     * @return the markdown text
     */
    private String updateUIWithResponse(String response) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();

        Node document = parser.parse(response);
        String html = renderer.render(document);

        promptOutputArea.setText(html);
        enableButtons();

        return html;
    }

    /**
     * Process the model name selection.
     */
    private void processModelNameSelection() {
        String selectedModel = (String) modelNameComboBox.getSelectedItem();
        if (selectedModel != null) {
            genieClient.setModelName(selectedModel);
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

    /**
     * Update the chat history.
     * @param currentIndex the current index
     * @param totalMessages the total messages
     */
    @Override
    public void onChatHistoryUpdated(int currentIndex, int totalMessages) {
        ChatInteraction currentInteraction = chatMessageHistoryService.getCurrentChatInteraction();
        promptInputArea.setText(currentInteraction.getQuestion());
        promptOutputArea.setText(currentInteraction.getResponse());
//        updateActionButtonStates();
    }

//    /**
//     * Update the action button states: prev/next and clear buttons.
//     */
//    private void updateActionButtonStates() {
//        int chatIndex = chatMessageHistoryService.getChatIndex();
//        int chatHistorySize = chatMessageHistoryService.getChatHistorySize();
//        nextInteractionBtn.setEnabled(chatIndex < chatHistorySize - 1);
//        previousInteractionBtn.setEnabled(chatIndex > 0);
//        addContextBtn.setEnabled(chatHistorySize > 1);
//        if (chatHistorySize > 1) {
//            chatCounterLabel.setText(String.format("%d/%d", (chatIndex + 1), chatHistorySize));
//        } else {
//            chatCounterLabel.setText("");
//        }
//    }
}
