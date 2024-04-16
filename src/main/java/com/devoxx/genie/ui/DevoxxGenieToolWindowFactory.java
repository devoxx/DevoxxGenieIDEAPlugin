package com.devoxx.genie.ui;

import com.devoxx.genie.DevoxxGenieClient;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
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
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import static com.devoxx.genie.ui.CommandHandler.*;

final class DevoxxGenieToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        DevoxxGenieToolWindowContent toolWindowContent = new DevoxxGenieToolWindowContent(toolWindow);
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    public static class DevoxxGenieToolWindowContent implements CommandHandlerListener {

        public static final String DEFAULT_LANGUAGE = "Java";

        public static final String WORKING_MESSAGE = "working.message";

        private final String[] items = {"Ollama", "LMStudio", "GPT4All"};

        private final DevoxxGenieClient genieClient;
        private final FileEditorManager fileEditorManager;
        private final JPanel contentPanel = new JPanel();
        private final ComboBox<String> providersComboBox = new ComboBox<>(items);
        private final ComboBox<String> modelComboBox = new ComboBox<>();
        private final JButton submitButton = new JButton();
        private final JButton clearButton = new JButton();
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
            submitButton.setText(resourceBundle.getString("btn.submit.label"));
            clearButton.setText(resourceBundle.getString("btn.clear.label"));
            promptInputArea.setPlaceholder(resourceBundle.getString("prompt.placeholder"));

            addOllamaModels();
            setupUIComponents();
        }

        private void setupUIComponents() {
            contentPanel.setLayout(new BorderLayout(0, 10));
            contentPanel.add(createSelectionPanel(), BorderLayout.NORTH);
            contentPanel.add(createInputPanel(), BorderLayout.CENTER);
        }

        @NotNull
        private JPanel createSelectionPanel() {
            JPanel toolPanel = new JPanel();
            toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));
            providersComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, providersComboBox.getPreferredSize().height));
            modelComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, modelComboBox.getPreferredSize().height));

            toolPanel.add(providersComboBox);
            toolPanel.add(Box.createVerticalStrut(5)); // Add some vertical spacing between components
            toolPanel.add(modelComboBox);

            providersComboBox.addActionListener(e -> processModelProviderSelection());
            modelComboBox.addActionListener(e -> processModelNameSelection());

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
                            handleCommand(text.toLowerCase().trim());

                            // Prevent the enter key from adding a new line
                            e.consume();
                        }
                    }
                }
            });

            // Submit Button - This should be aligned to the right, at the bottom
            JPanel submitPanel = new JPanel(new BorderLayout());
            submitButton.addActionListener(e -> onSubmit());

            clearButton.addActionListener(e -> promptOutputArea.setText("<html><body></body></html>"));

            // The submit button is put in a separate panel to align it to the right
            JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.add(clearButton, BorderLayout.WEST);
            buttonPanel.add(submitButton, BorderLayout.EAST);

            // The inputScrollPane is added to the submitPanel, not directly to the inputPanel
            submitPanel.add(inputScrollPane, BorderLayout.CENTER);
            submitPanel.add(buttonPanel, BorderLayout.SOUTH);

            // The submitPanel is then added to the inputPanel at the SOUTH border
            inputPanel.add(submitPanel, BorderLayout.SOUTH);

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
                resourceBundle.getString("welcome.tip"),
                resourceBundle.getString("welcome.enjoy")
            );
        }

        public void handleCommand(String command) {
            commandHandler.handleCommand(command);
        }

        public void showHelp() {
            String availableCommands = "<html><head><style type=\"text/css\">body { font-family: 'Source Code Pro', monospace; font-size: 14pt; margin: 5px; }</style></head><body>" +
                resourceBundle.getString("command.available") +
                "<br><ul>" +
                "<li>" + resourceBundle.getString("command.test") + "</li>" +
                "<li>" + resourceBundle.getString("command.review") + "</li>" +
                "<li>" + resourceBundle.getString("command.explain") + "</li>" +
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
                    prompt.equalsIgnoreCase(COMMAND_EXPLAIN)) {
                    handleCommand(prompt);
                    enableButtons();
                } else {
                    NotificationUtil.sendNotification(project, resourceBundle.getString("command.unknown") +  prompt);
                }
                return;
            }

            Task.Backgroundable task = new Task.Backgroundable(project, resourceBundle.getString(WORKING_MESSAGE), true) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    progressIndicator.setText(resourceBundle.getString(WORKING_MESSAGE));
                    executePrompt(promptInputArea.getText());
                }
            };
            task.queue();
        }

        private void disableButtons() {
            submitButton.setEnabled(false);
            clearButton.setEnabled(false);
            promptInputArea.setEnabled(false);
        }

        private void enableButtons() {
            promptInputArea.setEnabled(true);
            submitButton.setEnabled(true);
            clearButton.setEnabled(true);
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
        public void executePrompt(String userPrompt) {
            Editor editor = fileEditorManager.getSelectedTextEditor();
            if (editor != null) {
                LanguageTextPair languageAndText = getEditorLanguageAndText(editor);
                new Task.Backgroundable(project, resourceBundle.getString(WORKING_MESSAGE), true) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        String response = genieClient.executeGeniePrompt(userPrompt,
                                                                         languageAndText.getLanguage(),
                                                                         languageAndText.getText());
                        updateUIWithResponse(response);
                    }
                }.queue();
            } else {
                NotificationUtil.sendNotification(project, resourceBundle.getString("no.editor"));
            }
        }

        /**
         * Update the UI with the response.
         * @param response the response
         */
        private void updateUIWithResponse(String response) {
            Parser parser = Parser.builder().build();
            HtmlRenderer renderer = HtmlRenderer.builder().build();

            Node document = parser.parse(response);
            String html = renderer.render(document);

            promptOutputArea.setText(html);
            enableButtons();
        }

        /**
         * Process the model name selection.
         */
        private void processModelNameSelection() {
            String selectedModel = (String) modelComboBox.getSelectedItem();
            if (selectedModel != null) {
                genieClient.setModelName(selectedModel);
            }
        }

        /**
         * Process the model provider selection.
         */
        private void processModelProviderSelection() {
            String selectedProvider = (String) providersComboBox.getSelectedItem();
            if (selectedProvider != null) {
                genieClient.setModelProvider(ModelProvider.valueOf(selectedProvider));
            }

            // When Ollama is selected we need to show the available language models
            boolean isOllamaSelected = selectedProvider != null &&
                                       selectedProvider.equalsIgnoreCase(ModelProvider.Ollama.getName());
            modelComboBox.setVisible(isOllamaSelected);
        }

        /**
         * Add Ollama models for the model combo box.
         */
        private void addOllamaModels() {
            try {
                OllamaModelEntryDTO[] ollamaModels = new OllamaService().getModels();
                for (OllamaModelEntryDTO model : ollamaModels) {
                    modelComboBox.addItem(model.getName());
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
