package com.devoxx.genie.ui;

import com.devoxx.genie.DevoxxGenieClient;
import com.devoxx.genie.chatmodel.anthropic.AnthropicChatModelFactory;
import com.devoxx.genie.chatmodel.deepinfra.DeepInfraChatModelFactory;
import com.devoxx.genie.chatmodel.groq.GroqChatModelFactory;
import com.devoxx.genie.chatmodel.mistral.MistralChatModelFactory;
import com.devoxx.genie.chatmodel.ollama.OllamaChatModelFactory;
import com.devoxx.genie.chatmodel.openai.OpenAIChatModelFactory;
import com.devoxx.genie.model.ChatInteraction;
import com.devoxx.genie.service.ChatHistoryObserver;
import com.devoxx.genie.service.ChatMessageHistoryService;
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
import com.intellij.openapi.options.ShowSettingsUtil;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.devoxx.genie.model.enumarations.ModelProvider.*;
import static com.devoxx.genie.ui.CommandHandler.*;


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

    public static class DevoxxGenieToolWindowContent implements CommandHandlerListener,
                                                                ChatHistoryObserver,
                                                                SettingsChangeListener {

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

        private final DevoxxGenieClient genieClient;
        private final ChatMessageHistoryService chatMessageHistoryService = new ChatMessageHistoryService();
        private final FileEditorManager fileEditorManager;
        private final JPanel contentPanel = new JPanel();
        private final ComboBox<String> llmProvidersComboBox = new ComboBox<>();
        private final ComboBox<String> modelNameComboBox = new ComboBox<>();
        private final JButton submitBtn = new JButton();
        private final JButton clearBtn = new JButton();
        private final JButton previousInteractionBtn = new JButton();
        private final JButton nextInteractionBtn = new JButton();
        private final JButton configBtn = new JButton("＋");
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
            commandHandler = new CommandHandler(this);

            // Load the resource bundle
            resourceBundle = ResourceBundle.getBundle("messages");

            // Create the buttons
            submitBtn.setText(resourceBundle.getString("btn.submit.label"));
            clearBtn.setText(resourceBundle.getString("btn.clear.label"));
            clearBtn.setToolTipText("Clear chat history");

            previousInteractionBtn.setText("<");
            nextInteractionBtn.setText(">");

            chatMessageHistoryService.addObserver(this);

            // Set the placeholder text
            promptInputArea.setPlaceholder(resourceBundle.getString("prompt.placeholder"));

            updateButtonStates();
            addLLMProvidersToComboBox();
            handleModelProviderSelectionChange();
            setupUIComponents();
        }

        /**
         * Refresh the LLM providers dropdown because the Settings have been changed.
         */
        public void settingsChanged() {
            llmProvidersComboBox.removeAllItems();
            addLLMProvidersToComboBox();
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
        private void addLLMProvidersToComboBox() {
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

            JPanel providerPanel = new JPanel();
            providerPanel.setLayout(new BorderLayout());
            providerPanel.add(configBtn, BorderLayout.WEST);
            providerPanel.add(llmProvidersComboBox, BorderLayout.CENTER);

            toolPanel.add(providerPanel);
            toolPanel.add(Box.createVerticalStrut(5));
            toolPanel.add(modelNameComboBox);

            configBtn.addActionListener(e -> {
                if (project != null) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "Devoxx Genie Settings");
                }
            });
            llmProvidersComboBox.addActionListener(e -> handleModelProviderSelectionChange());
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
            nextPrevPanel.add(previousInteractionBtn);
            previousInteractionBtn.setToolTipText("Show previous chat response");
            nextPrevPanel.add(nextInteractionBtn);
            nextInteractionBtn.setToolTipText("Show next chat response");
            nextPrevPanel.add(chatCounterLabel);

            JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.add(submitBtn, BorderLayout.WEST);
            buttonPanel.add(nextPrevPanel, BorderLayout.CENTER);
            buttonPanel.add(clearBtn, BorderLayout.EAST);

            JBScrollPane nextPrevScrollPane = new JBScrollPane(buttonPanel);

            submitPanel.add(inputScrollPane, BorderLayout.CENTER);
            submitPanel.add(nextPrevScrollPane, BorderLayout.SOUTH);

            inputPanel.add(submitPanel, BorderLayout.SOUTH);

            submitBtn.addActionListener(e -> onSubmit());
            clearBtn.addActionListener(e -> {
                promptOutputArea.setText(getWelcomeText());
                promptInputArea.setPlaceholder(resourceBundle.getString("prompt.placeholder"));
                chatMessageHistoryService.clearHistory();
                updateButtonStates();
            });
            previousInteractionBtn.addActionListener(e -> chatMessageHistoryService.setPreviousMessage());
            nextInteractionBtn.addActionListener(e -> chatMessageHistoryService.setNextMessage());

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
            submitBtn.setEnabled(false);
            promptInputArea.setEnabled(false);
        }

        private void enableButtons() {
            promptInputArea.setEnabled(true);
            submitBtn.setEnabled(true);
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
                        try {
                            String response = genieClient.executeGeniePrompt(userPrompt,
                                                                             languageAndText.getLanguage(),
                                                                             languageAndText.getText());
                            String htmlResponse = updateUIWithResponse(response);

                            chatMessageHistoryService.addMessage(llmProvidersComboBox.getSelectedItem() == null ? "" : llmProvidersComboBox.getSelectedItem().toString(),
                                modelNameComboBox.getSelectedItem() == null ? "" : modelNameComboBox.getSelectedItem().toString(),
                                command.isEmpty() ? userPrompt : command,
                                htmlResponse);

                        } catch (Exception e) {
                            NotificationUtil.sendNotification(project, e.getMessage());
                            enableButtons();
                        }
                    }
                }.queue();
            } else {
                NotificationUtil.sendNotification(project, resourceBundle.getString("no.editor"));
            }
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
         * Process the model provider selection.
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

        public JPanel getContentPanel() {
            return contentPanel;
        }

        @Override
        public void onHistoryUpdated(int currentIndex, int totalMessages) {
            ChatInteraction currentInteraction = chatMessageHistoryService.getCurrentChatInteraction();
            promptInputArea.setText(currentInteraction.getQuestion());
            promptOutputArea.setText(currentInteraction.getResponse());
            updateButtonStates();
        }

        private void updateButtonStates() {
            int chatIndex = chatMessageHistoryService.getChatIndex();
            int chatHistorySize = chatMessageHistoryService.getChatHistorySize();
            nextInteractionBtn.setEnabled(chatIndex < chatHistorySize - 1);
            previousInteractionBtn.setEnabled(chatIndex > 0);
            clearBtn.setEnabled(chatHistorySize > 1);
            if (chatHistorySize > 1) {
                chatCounterLabel.setText(String.format("%d/%d", (chatIndex + 1), chatHistorySize));
            } else {
                chatCounterLabel.setText("");
            }
        }
    }
}
