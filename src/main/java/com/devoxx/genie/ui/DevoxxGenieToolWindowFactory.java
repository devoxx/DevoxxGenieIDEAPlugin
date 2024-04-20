package com.devoxx.genie.ui;

import com.devoxx.genie.DevoxxGenieClient;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.ChatMessageHistoryService;
import com.devoxx.genie.service.OllamaService;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.component.PlaceholderTextArea;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.util.WorkingMessage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;
import com.devoxx.genie.model.LanguageTextPair;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.devoxx.genie.model.enumarations.ModelProvider.*;
import static com.devoxx.genie.ui.CommandHandler.*;
import static dev.langchain4j.model.anthropic.AnthropicChatModelName.*;
import static dev.langchain4j.model.mistralai.MistralAiChatModelName.*;
import static dev.langchain4j.model.openai.OpenAiChatModelName.*;


final class DevoxxGenieToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        DevoxxGenieToolWindowContent toolWindowContent = new DevoxxGenieToolWindowContent(toolWindow);
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(content);

        // Subscribe to settings changes
        MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        MessageBusConnection connection = bus.connect();
        connection.subscribe(SettingsChangeListener.TOPIC, toolWindowContent);
    }

    public static class DevoxxGenieToolWindowContent implements CommandHandlerListener, SettingsChangeListener {

        public static final String DEFAULT_LANGUAGE = "Java";

        public static final String WORKING_MESSAGE = "working.message";

        private final String[] llmProvidersWithKey = {
            Anthropic.getName(),
            DeepInfra.getName(),
            Groq.getName(),
            Mistral.getName(),
            OpenAI.getName()
        };

        private final String[] llmProviders = {
            GPT4All.getName(),
            LMStudio.getName(),
            Ollama.getName()
        };

        private final ChatMessageHistoryService chatMessageHistoryService;
        private final DevoxxGenieClient genieClient;
        private final FileEditorManager fileEditorManager;
        private final JPanel contentPanel = new JPanel();
        private final ComboBox<String> llmProvidersComboBox = new ComboBox<>();
        private final ComboBox<String> modelNameComboBox = new ComboBox<>();
        private final JButton submitButton = new JButton();
        private final JButton clearButton = new JButton();
        private final JButton prevChatMsgButton = new JButton();
        private final JButton nextChatMsgButton = new JButton();
        private final JLabel chatCounterLabel = new JLabel();
        private final PlaceholderTextArea promptInputArea = new PlaceholderTextArea(3, 80);
        private final JEditorPane promptOutputArea = new JEditorPane();

        private final Project project;
        private final CommandHandler commandHandler;
        private final ResourceBundle resourceBundle;

        public DevoxxGenieToolWindowContent(ToolWindow toolWindow) {
            project = toolWindow.getProject();
            fileEditorManager = FileEditorManager.getInstance(project);
            genieClient = DevoxxGenieClient.getInstance();
            commandHandler = new CommandHandler(this); // Initialize with the loaded ResourceBundle

            // Load the resource bundle
            resourceBundle = ResourceBundle.getBundle("messages");

            // Create the buttons
            submitButton.setText(resourceBundle.getString("btn.submit.label"));
            clearButton.setText(resourceBundle.getString("btn.clear.label"));
            clearButton.setToolTipText("Clear chat history");

            prevChatMsgButton.setText("<");
            nextChatMsgButton.setText(">");

            chatMessageHistoryService =
                new ChatMessageHistoryService(prevChatMsgButton, nextChatMsgButton, chatCounterLabel, clearButton, promptInputArea);

            // Set the placeholder text
            promptInputArea.setPlaceholder(resourceBundle.getString("prompt.placeholder"));

            populateProvidersToComboBox();
            processModelProviderSelection();
            addOllamaModels();
            setupUIComponents();
        }

        /**
         * Refresh the LLM providers dropdown because the Settings have been changed.
         */
        public void settingsChanged() {
            llmProvidersComboBox.removeAllItems();
            populateProvidersToComboBox();
        }

        private void setupUIComponents() {
            contentPanel.setLayout(new BorderLayout(0, 10));
            contentPanel.add(createSelectionPanel(), BorderLayout.NORTH);
            contentPanel.add(createInputPanel(), BorderLayout.CENTER);
        }

        /**
         * Add the LLM providers to combobox.
         * Only show the cloud-based LLM providers for which we have an API Key.
         */
        private void populateProvidersToComboBox() {
            SettingsState settingState = SettingsState.getInstance();

            Map<String, Supplier<String>> providerKeyMap = new HashMap<>();
            providerKeyMap.put(OpenAI.getName(), settingState::getOpenAIKey);
            providerKeyMap.put(Anthropic.getName(), settingState::getAnthropicKey);
            providerKeyMap.put(Mistral.getName(), settingState::getMistralKey);
            providerKeyMap.put(Groq.getName(), settingState::getGroqKey);
            providerKeyMap.put(DeepInfra.getName(), settingState::getDeepInfraKey);

            // Filter out cloud LLM providers that do not have a key
            List<String> providers = Stream.of(llmProvidersWithKey)
                .filter(provider -> Optional.ofNullable(providerKeyMap.get(provider))
                    .map(Supplier::get)
                    .filter(key -> !key.isBlank())
                    .isPresent())
                .collect(Collectors.toList());

            Collections.addAll(providers, llmProviders);
            providers.forEach(llmProvidersComboBox::addItem);
        }

        @NotNull
        private JPanel createSelectionPanel() {
            JPanel toolPanel = new JPanel();
            toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));
            llmProvidersComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, llmProvidersComboBox.getPreferredSize().height));
            modelNameComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, modelNameComboBox.getPreferredSize().height));

            toolPanel.add(llmProvidersComboBox);
            toolPanel.add(Box.createVerticalStrut(5));
            toolPanel.add(modelNameComboBox);

            llmProvidersComboBox.addActionListener(e -> processModelProviderSelection());
            modelNameComboBox.addActionListener(e -> processModelNameSelection());

            return toolPanel;
        }

        @NotNull
        private JPanel createInputPanel() {
            JPanel inputPanel = new JPanel(new BorderLayout());

            // Output Area - This should expand to fill most of the space
            promptOutputArea.setEditable(false);
            promptOutputArea.setContentType("text/html");
            promptOutputArea.setText(getWelcomeText());
            JScrollPane outputScrollPane = new JBScrollPane(promptOutputArea);
            inputPanel.add(outputScrollPane, BorderLayout.CENTER);

            // Input Area - This should be at the bottom
            promptInputArea.setLineWrap(true);
            promptInputArea.setWrapStyleWord(true);
            JScrollPane inputScrollPane = new JBScrollPane(promptInputArea);

            promptInputArea.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        String text = promptInputArea.getText().trim();

                        // Check if the input is a command
                        if (text.startsWith("/")) {
                            commandHandler.handleCommand(text.toLowerCase().trim());

                            // Prevent the enter key from adding a new line
                            e.consume();
                        }
                    }
                }
            });

            JPanel submitPanel = new JPanel(new BorderLayout());

            JPanel nextPrevPanel = new JPanel();
            nextPrevPanel.add(prevChatMsgButton);
            prevChatMsgButton.setToolTipText("Show previous chat response");
            nextPrevPanel.add(nextChatMsgButton);
            nextChatMsgButton.setToolTipText("Show next chat response");
            nextPrevPanel.add(chatCounterLabel);

            JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.add(clearButton, BorderLayout.WEST);
            buttonPanel.add(nextPrevPanel, BorderLayout.CENTER);
            buttonPanel.add(submitButton, BorderLayout.EAST);

            submitPanel.add(inputScrollPane, BorderLayout.CENTER);
            submitPanel.add(buttonPanel, BorderLayout.SOUTH);

            inputPanel.add(submitPanel, BorderLayout.SOUTH);

            submitButton.addActionListener(e -> onSubmit());
            clearButton.addActionListener(e -> {
                promptOutputArea.setText(getWelcomeText());
                promptInputArea.setPlaceholder(resourceBundle.getString("prompt.placeholder"));
            });
            prevChatMsgButton.addActionListener(e -> chatMessageHistoryService.setPreviousMessage(promptOutputArea));
            nextChatMsgButton.addActionListener(e -> chatMessageHistoryService.setNextMessage(promptOutputArea));

            return inputPanel;
        }

        public String getWelcomeText() {
            return """
                <html>
                <head>
                    <style type="text/css">
                        body {
                            font-family: 'Source Code Pro', monospace; font-size: 14pt;
                            margin: 5px;
                        }
                    </style>
                </head>
                <body>
                    <h2>%s</h2>
                    <p>%s</p>
                    <p>%s</p>
                    <p>%s
                    <ul>
                    <li>%s</li>
                    <li>%s</li>
                    <li>%s</li>
                    <li>%s</li>
                    </ul>
                    </p>
                    <p>%s</p>
                    <p>%s</p>
                </body>
                </html>
                """.formatted(
                resourceBundle.getString("welcome.title"),
                resourceBundle.getString("welcome.description"),
                resourceBundle.getString("welcome.instructions"),
                resourceBundle.getString("welcome.commands"),
                resourceBundle.getString("command.test"),
                resourceBundle.getString("command.review"),
                resourceBundle.getString("command.explain"),
                resourceBundle.getString("command.custom"),
                resourceBundle.getString("welcome.tip"),
                resourceBundle.getString("welcome.enjoy")
            );
        }

        public void showHelp() {
            String availableCommands = "<html><head><style type=\"text/css\">body { font-family: 'Source Code Pro', monospace; font-size: 14pt; margin: 5px; }</style></head><body>" +
                resourceBundle.getString("command.available") +
                "<br><ul>" +
                "<li>" + resourceBundle.getString("command.test") + "</li>" +
                "<li>" + resourceBundle.getString("command.review") + "</li>" +
                "<li>" + resourceBundle.getString("command.explain") + "</li>" +
                "<li>" + resourceBundle.getString("command.custom") + "</li>" +
                "</ul></body></html>";
            promptOutputArea.setText(availableCommands);
        }

        private void onSubmit() {

            String prompt = promptInputArea.getText();
            if (prompt.isEmpty()) {
                return;
            }
            disableButtons();

            promptOutputArea.setText(WorkingMessage.getWorkingMessage());

            if (prompt.startsWith("/")) {
                if (prompt.equalsIgnoreCase(COMMAND_HELP) ||
                    prompt.equalsIgnoreCase(COMMAND_TEST) ||
                    prompt.equalsIgnoreCase(COMMAND_REVIEW) ||
                    prompt.equalsIgnoreCase(COMMAND_EXPLAIN) ||
                    prompt.equalsIgnoreCase(COMMAND_CUSTOM)) {
                    commandHandler.handleCommand(prompt);
                } else {
                    showHelp();
                    NotificationUtil.sendNotification(project, resourceBundle.getString("command.unknown") +  prompt);
                }
                enableButtons();
                return;
            }

            Task.Backgroundable task = new Task.Backgroundable(project, resourceBundle.getString(WORKING_MESSAGE), true) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    progressIndicator.setText(resourceBundle.getString(WORKING_MESSAGE));
                    executePrompt("", promptInputArea.getText());
                }
            };
            task.queue();
        }

        private void disableButtons() {
            submitButton.setEnabled(false);
            promptInputArea.setEnabled(false);
        }

        private void enableButtons() {
            promptInputArea.setEnabled(true);
            submitButton.setEnabled(true);
        }

        /**
         * Get the language and text from the editor.
         * @param editor the editor
         * @return the language and text
         */
        private LanguageTextPair getEditorLanguageAndText(Editor editor) {
            String languageName = DEFAULT_LANGUAGE;
            Document document = editor.getDocument();
            VirtualFile file = FileDocumentManager.getInstance().getFile(document);

            AtomicReference<String> selectedTextRef = new AtomicReference<>();
            ApplicationManager.getApplication().runReadAction(() ->
                selectedTextRef.set(editor.getSelectionModel().getSelectedText()));

            String selectedText = selectedTextRef.get();

            if (selectedText == null && file != null) {
                FileType fileType = file.getFileType();
                languageName = fileType.getName();
                selectedText = document.getText();
            }

            return new LanguageTextPair(languageName, selectedText);
        }

        /**
         * Execute the user prompt.
         * @param userPrompt the user prompt
         */
        public void executePrompt(String command, String userPrompt) {
            Editor editor = fileEditorManager.getSelectedTextEditor();
            if (editor != null) {
                LanguageTextPair languageAndText = getEditorLanguageAndText(editor);
                new Task.Backgroundable(project, resourceBundle.getString(WORKING_MESSAGE), true) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        String response = genieClient.executeGeniePrompt(userPrompt,
                                                                         languageAndText.getLanguage(),
                                                                         languageAndText.getText());
                        String htmlResponse = updateUIWithResponse(response);
                        chatMessageHistoryService.addMessage(command.isEmpty()?userPrompt:command, htmlResponse);
                    }
                }.queue();
            } else {
                NotificationUtil.sendNotification(project, resourceBundle.getString("no.editor"));
            }
        }

        /**
         * Update the UI with the response.
         * @param response the response
         * @return the HTML markdown text
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
         * Process the model provider selection.
         */
        private void processModelProviderSelection() {
            String selectedProvider = (String) llmProvidersComboBox.getSelectedItem();
            if (selectedProvider != null) {
                genieClient.setModelProvider(ModelProvider.valueOf(selectedProvider));
            }

            if (selectedProvider != null) {
                modelNameComboBox.setVisible(true);
                modelNameComboBox.removeAllItems();

                if (selectedProvider.equalsIgnoreCase(Ollama.getName())) {
                    modelNameComboBox.setVisible(true);
                    modelNameComboBox.removeAllItems();
                    addOllamaModels();
                } else if (selectedProvider.equalsIgnoreCase(ModelProvider.OpenAI.getName())) {
                    modelNameComboBox.addItem(GPT_3_5_TURBO.toString());
                    modelNameComboBox.addItem(GPT_3_5_TURBO_16K.toString());
                    modelNameComboBox.addItem(GPT_4.toString());
                    modelNameComboBox.addItem(GPT_4_32K.toString());
                } else if (selectedProvider.equalsIgnoreCase(ModelProvider.Anthropic.getName())) {
                    modelNameComboBox.addItem(CLAUDE_3_OPUS_20240229.toString());
                    modelNameComboBox.addItem(CLAUDE_3_SONNET_20240229.toString());
                    modelNameComboBox.addItem(CLAUDE_3_HAIKU_20240307.toString());
                    modelNameComboBox.addItem(CLAUDE_2_1.toString());
                    modelNameComboBox.addItem(CLAUDE_2.toString());
                    modelNameComboBox.addItem(CLAUDE_INSTANT_1_2.toString());
                } else if (selectedProvider.equalsIgnoreCase(ModelProvider.Mistral.getName())) {
                    modelNameComboBox.addItem(OPEN_MISTRAL_7B.toString());
                    modelNameComboBox.addItem(OPEN_MIXTRAL_8x7B.toString());
                    modelNameComboBox.addItem(MISTRAL_SMALL_LATEST.toString());
                    modelNameComboBox.addItem(MISTRAL_MEDIUM_LATEST.toString());
                } else if (selectedProvider.equalsIgnoreCase(ModelProvider.Groq.getName())) {
                    modelNameComboBox.addItem("llama2-70b-4096");
                    modelNameComboBox.addItem("mixtral-8x7b-32768");
                    modelNameComboBox.addItem("gemma-7b-it");
                } else if (selectedProvider.equalsIgnoreCase(DeepInfra.getName())) {
                    // TODO Check which other models are available
                    modelNameComboBox.addItem("mistralai/Mixtral-8x7B-Instruct-v0.1");
                } else if (selectedProvider.equalsIgnoreCase(LMStudio.getName()) ||
                           selectedProvider.equalsIgnoreCase(GPT4All.getName())) {
                    modelNameComboBox.setVisible(false);
                }
            }
        }

        /**
         * Add Ollama models for the model combo box.
         */
        private void addOllamaModels() {
            try {
                OllamaModelEntryDTO[] ollamaModels = new OllamaService().getModels();
                for (OllamaModelEntryDTO model : ollamaModels) {
                    modelNameComboBox.addItem(model.getName());
                }
            } catch (IOException e) {
                NotificationUtil.sendNotification(project, resourceBundle.getString("ollama.not_running"));
            }
        }

        public JPanel getContentPanel() {
            return contentPanel;
        }
    }
}
