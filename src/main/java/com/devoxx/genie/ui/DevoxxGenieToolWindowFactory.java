package com.devoxx.genie.ui;

import com.devoxx.genie.DevoxxGenieClient;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.OllamaService;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.component.PlaceholderTextArea;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.devoxx.genie.model.Pair;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

final class DevoxxGenieToolWindowFactory implements ToolWindowFactory, DumbAware {

    private static final Logger log = LoggerFactory.getLogger(DevoxxGenieToolWindowFactory.class);

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        DevoxxGenieToolWindowContent toolWindowContent = new DevoxxGenieToolWindowContent(toolWindow);
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private static class DevoxxGenieToolWindowContent {

        public static final String DEVOXX_GENIE_WORKING = "Devoxx Genie working...";

        public static final String HELP_CMD = "/help";
        public static final String TEST_CMD = "/test";
        public static final String REVIEW_CMD = "/review";
        public static final String EXPLAIN_CMD = "/explain";
        public static final String DEFAULT_LANGUAGE = "Java";
        private final String[] items = {"Ollama", "LMStudio", "GPT4All"};

        private final DevoxxGenieClient genieClient;
        private final FileEditorManager fileEditorManager;
        private final JPanel contentPanel = new JPanel();
        private final ComboBox<String> providersComboBox = new ComboBox<>(items);
        private final ComboBox<String> modelComboBox = new ComboBox<>();
        private final JButton submitButton = new JButton("Submit");
        private final JButton clearButton = new JButton("Clear");
        private final PlaceholderTextArea promptInputArea = new PlaceholderTextArea("Enter your prompt here or type /help", 3, 80);
        private final JEditorPane promptOutputArea = new JEditorPane();

        private final Project project;

        public DevoxxGenieToolWindowContent(ToolWindow toolWindow) {
            project = toolWindow.getProject();
            fileEditorManager = FileEditorManager.getInstance(project);
            genieClient = DevoxxGenieClient.getInstance();

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
            // Set the panel's layout to BoxLayout, aligning components along the Y-axis (top to bottom)
            toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));
            // Ensure each combo box takes full horizontal space and aligns correctly
            providersComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, providersComboBox.getPreferredSize().height));
            modelComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, modelComboBox.getPreferredSize().height));

            // Add the combo boxes to the panel
            toolPanel.add(providersComboBox);
            toolPanel.add(Box.createVerticalStrut(5)); // Add some vertical spacing between components
            toolPanel.add(modelComboBox);

            // Add action listeners as before
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
            promptOutputArea.setText("""
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
                    <h2>Welcome to Devoxx Genie</h2>
                    <p>The Devoxx Genie plugin allows you to interact with local running Large Language Models (LLMs) even without an internet connection..</p>
                    <p>Start by selecting a language model provider.</p>
                    <p>Select some code, type your prompt and click the submit button.</p>
                    <p>Utility commands are:
                    <ul>
                    <li>/test - write a unit test</li>
                    <li>/review - review code</li>
                    <li>/explain - explain the code</li>
                    </ul>
                    </p>
                    <p>You can also change the LLM provider REST endpoints in the plugin settings.</p>
                    <p>Enjoy!</p>
                </body>
                </html>
                """);
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

        public void handleCommand(String command) {
            switch (command.toLowerCase()) {
                case TEST_CMD:
                    executePrompt("Write a unit test for this code using JUnit.");
                    break;
                case REVIEW_CMD:
                    executePrompt("Review the selected code, can it be improved or are there bugs?");
                    break;
                case EXPLAIN_CMD:
                    executePrompt("Explain the code so a junior developer can understand it.");
                    break;
                default:
                    String value = """
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
                            Available commands for selected code:<br>
                            <UL>
                            <LI>/test - write a unit test</LI>
                            <LI>/review - review code</LI>
                            <LI>/explain - explain the code</LI>
                            </UL>
                        </body>
                        </html>
                        """;
                    promptOutputArea.setText(value);
                    break;
            }
        }

        private void onSubmit() {

            String prompt = promptInputArea.getText();
            if (prompt.isEmpty()) {
                return;
            }
            disableButtons();

            if (prompt.startsWith("/")) {
                if (prompt.equalsIgnoreCase(HELP_CMD) ||
                    prompt.equalsIgnoreCase(TEST_CMD) ||
                    prompt.equalsIgnoreCase(REVIEW_CMD) ||
                    prompt.equalsIgnoreCase(EXPLAIN_CMD)) {
                    handleCommand(prompt);
                    enableButtons();
                } else {
                    NotificationUtil.sendNotification(project, "Unknown command: " + prompt);
                }
                return;
            }

            Task.Backgroundable task = new Task.Backgroundable(project, DEVOXX_GENIE_WORKING, true) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    progressIndicator.setText(DEVOXX_GENIE_WORKING);
                    executePrompt(promptInputArea.getText());
                }
            };
            task.queue();
        }

        private void disableButtons() {
            submitButton.setEnabled(false);
            clearButton.setEnabled(false);
            promptInputArea.setEnabled(false);
            promptOutputArea.setText(WorkingMessage.getWorkingMessage());
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
        private Pair getEditorLanguageAndText(Editor editor) {
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

            return new Pair(languageName, selectedText);
        }

        /**
         * Execute the user prompt.
         * @param userPrompt the user prompt
         */
        private void executePrompt(String userPrompt) {
            Editor editor = fileEditorManager.getSelectedTextEditor();
            if (editor != null) {
                Pair value = getEditorLanguageAndText(editor);
                new Task.Backgroundable(project, DEVOXX_GENIE_WORKING, true) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        String response = genieClient.executeGeniePrompt(userPrompt, value.language(), value.text());
                        updateUIWithResponse(response);
                    }
                }.queue();
            } else {
                NotificationUtil.sendNotification(project, "No source file open.");
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
                log.error("Error getting Ollama models", e);
                NotificationUtil.sendNotification(project, "Error getting Ollama models, make sure Ollama is running.");
            }
        }

        public JPanel getContentPanel() {
            return contentPanel;
        }
    }
}
