package com.devoxx.genie.ui;

import com.devoxx.genie.ui.util.DoubleConverter;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

import static com.intellij.openapi.options.Configurable.isFieldModified;

public class Settings implements Configurable {

    public static final String MODEL_PROVIDER = "com.devoxx.genie.settings.modelProvider";

    private final DoubleConverter doubleConverter;

    private JTextField ollamaUrlField;
    private JTextField lmstudioUrlField;
    private JTextField gpt4allUrlField;

    private JPasswordField openAiKeyField;
    private JPasswordField mistralKeyField;
    private JPasswordField anthropicKeyField;
    private JPasswordField groqKeyField;
    private JPasswordField deepInfraKeyField;

    private JFormattedTextField temperatureField;
    private JFormattedTextField topPField;

    private JFormattedTextField timeoutField;
    private JFormattedTextField retryField;

    private JTextField testPromptField;
    private JTextField explainPromptField;
    private JTextField reviewPromptField;
    private JTextField customPromptField;

    private final String keyEmoji = "\uD83D\uDD11";
    private final String wwwEmoji = "\uD83D\uDD17";

    public Settings() {
        doubleConverter = new DoubleConverter();
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Devoxx Genie Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        SettingsState settings = SettingsState.getInstance();

        JPanel settingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        // Helper method to add a text field with label
        setTitle("Local Large Language Models", settingsPanel, gbc);

        ollamaUrlField = addFieldWithLinkButton(settingsPanel, gbc, "Ollama URL:", settings.getOllamaModelUrl(), "https://ollama.com/");
        lmstudioUrlField = addFieldWithLinkButton(settingsPanel, gbc, "LMStudio URL:", settings.getLmstudioModelUrl(), "https://lmstudio.ai/");
        gpt4allUrlField = addFieldWithLinkButton(settingsPanel, gbc, "GPT4All URL:", settings.getGpt4allModelUrl(), "https://gpt4all.io/");

        setTitle("Cloud based Large Language Models", settingsPanel, gbc);

        openAiKeyField = addFieldWithLabelPasswordAndLinkButton(settingsPanel, gbc, "OpenAI API Key :", settings.getOpenAIKey(), "https://platform.openai.com/api-keys");
        mistralKeyField = addFieldWithLabelPasswordAndLinkButton(settingsPanel, gbc, "Mistral API Key :", settings.getMistralKey(), "https://console.mistral.ai/api-keys");
        anthropicKeyField = addFieldWithLabelPasswordAndLinkButton(settingsPanel, gbc, "Anthropic API Key :", settings.getAnthropicKey(), "https://console.anthropic.com/settings/keys");
        groqKeyField = addFieldWithLabelPasswordAndLinkButton(settingsPanel, gbc, "Groq API Key :", settings.getGroqKey(), "https://console.groq.com/keys");
        deepInfraKeyField = addFieldWithLabelPasswordAndLinkButton(settingsPanel, gbc, "DeepInfra API Key :", settings.getDeepInfraKey(), "https://deepinfra.com/dash/api_keys");

        setTitle("LLM Parameters", settingsPanel, gbc);

        temperatureField = addFormattedFieldWithLabel(settingsPanel, gbc, "Temperature:", settings.getTemperature());
        topPField = addFormattedFieldWithLabel(settingsPanel, gbc, "Top-P:", settings.getTopP());
        timeoutField = addFormattedFieldWithLabel(settingsPanel, gbc, "Timeout (in secs):", settings.getTimeout());
        retryField = addFormattedFieldWithLabel(settingsPanel, gbc, "Maximum retries :", settings.getMaxRetries());

        setTitle("Predefined Command Prompts", settingsPanel, gbc);

        testPromptField = addTextFieldWithLabel(settingsPanel, gbc, "Test prompt :", settings.getTestPrompt());
        explainPromptField = addTextFieldWithLabel(settingsPanel, gbc, "Explain prompt :", settings.getExplainPrompt());
        reviewPromptField = addTextFieldWithLabel(settingsPanel, gbc, "Review prompt :", settings.getReviewPrompt());
        customPromptField = addTextFieldWithLabel(settingsPanel, gbc, "Custom prompt :", settings.getCustomPrompt());
        return settingsPanel;
    }

    private void setTitle(String title, JPanel settingsPanel, GridBagConstraints gbc) {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        JLabel titleLabel = new JLabel(title);
        jPanel.add(titleLabel, BorderLayout.WEST);
        jPanel.add(new JSeparator(), BorderLayout.CENTER);
        settingsPanel.add(titleLabel, gbc);
        resetGbc(gbc);
    }

    private JTextField addTextFieldWithLabel(JPanel panel,
                                             GridBagConstraints gbc,
                                             String label,
                                             String value) {
        panel.add(new JLabel(label), gbc);
        gbc.gridx++;
        JTextField textField = new JTextField(value);
        panel.add(textField, gbc);
        resetGbc(gbc);
        return textField;
    }

    private JTextField addFieldWithLinkButton(JPanel panel,
                                              GridBagConstraints gbc,
                                              String label,
                                              String value,
                                              String url) {
        JButton btnApiKey = createButton(wwwEmoji, "Download from", url);
        panel.add(new JLabel(label), gbc);
        gbc.gridx++;
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        JTextField textField = new JTextField(value);
        jPanel.add(textField, BorderLayout.CENTER);
        jPanel.add(btnApiKey, BorderLayout.WEST);
        panel.add(jPanel, gbc);
        resetGbc(gbc);
        return textField;
    }

    private JPasswordField addFieldWithLabelPasswordAndLinkButton(JPanel panel,
                                                                  GridBagConstraints gbc,
                                                                  String label,
                                                                  String value,
                                                                  String url) {
        JButton btnApiKey = createButton(keyEmoji, "Get your API Key from ", url);
        panel.add(new JLabel(label), gbc);
        gbc.gridx++;
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        JPasswordField passwordField = new JPasswordField(value);
        passwordField.setEchoChar('*');
        jPanel.add(passwordField, BorderLayout.CENTER);
        jPanel.add(btnApiKey, BorderLayout.WEST);
        panel.add(jPanel, gbc);
        resetGbc(gbc);
        return passwordField;
    }

    /**
     * Create a button with emoji and tooltip message and open the URL in the browser
     * @param emoji the emoji to use in the button
     * @param toolTipMsg the tooltip message
     * @param url the url to open when clicked
     * @return the created button
     */
    private @NotNull JButton createButton(String emoji,
                                          String toolTipMsg,
                                          String url) {
        JButton btnApiKey = new JButton(emoji);
        btnApiKey.setToolTipText(toolTipMsg + " " + url);
        btnApiKey.addActionListener(e -> {
            try {
                BrowserUtil.open(url);
                // Desktop.getDesktop().browse(java.net.URI.create(toolTipMsg));
            } catch (Exception ex) {
                Project project = ProjectManager.getInstance().getOpenProjects()[0];
                NotificationUtil.sendNotification(project, "Error: Unable to open the link");
            }
        });
        return btnApiKey;
    }

    private JFormattedTextField addFormattedFieldWithLabel(JPanel panel,
                                                           GridBagConstraints gbc,
                                                           String label,
                                                           Number value) {
        panel.add(new JLabel(label), gbc);
        gbc.gridx++;
        JFormattedTextField formattedField = new JFormattedTextField();
        setValue(formattedField, value);
        panel.add(formattedField, gbc);
        resetGbc(gbc);
        return formattedField;
    }

    private void resetGbc(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
    }

    @Override
    public boolean isModified() {
        SettingsState settings = SettingsState.getInstance();

        boolean isModified = isFieldModified(ollamaUrlField, settings.getOllamaModelUrl());
        isModified |= isFieldModified(lmstudioUrlField, settings.getLmstudioModelUrl());
        isModified |= isFieldModified(gpt4allUrlField, settings.getGpt4allModelUrl());
        isModified |= isFieldModified(temperatureField, Objects.requireNonNull(doubleConverter.toString(settings.getTemperature())));
        isModified |= isFieldModified(topPField, Objects.requireNonNull(doubleConverter.toString(settings.getTopP())));
        isModified |= isFieldModified(timeoutField, settings.getTimeout());
        isModified |= isFieldModified(retryField, settings.getMaxRetries());
        isModified |= isFieldModified(testPromptField, settings.getTestPrompt());
        isModified |= isFieldModified(explainPromptField, settings.getExplainPrompt());
        isModified |= isFieldModified(reviewPromptField, settings.getReviewPrompt());
        isModified |= isFieldModified(customPromptField, settings.getCustomPrompt());
        isModified |= isFieldModified(openAiKeyField, settings.getOpenAIKey());
        isModified |= isFieldModified(mistralKeyField, settings.getMistralKey());
        isModified |= isFieldModified(anthropicKeyField, settings.getAnthropicKey());
        isModified |= isFieldModified(groqKeyField, settings.getGroqKey());
        isModified |= isFieldModified(deepInfraKeyField, settings.getDeepInfraKey());
        return isModified;
    }

    @Override
    public void apply() {

        SettingsState settings = SettingsState.getInstance();

        if (isFieldModified(ollamaUrlField, settings.getOllamaModelUrl())) {
            settings.setOllamaModelUrl(ollamaUrlField.getText());
        }

        if (isFieldModified(lmstudioUrlField, settings.getLmstudioModelUrl())) {
            settings.setLmstudioModelUrl(lmstudioUrlField.getText());
        }

        if (isFieldModified(gpt4allUrlField, settings.getGpt4allModelUrl())) {
            settings.setGpt4allModelUrl(gpt4allUrlField.getText());
        }

        if (isFieldModified(temperatureField, Objects.requireNonNull(doubleConverter.toString(settings.getTemperature())))) {
            settings.setTemperature(doubleConverter.fromString(temperatureField.getText()));
        }

        if (isFieldModified(topPField, Objects.requireNonNull(doubleConverter.toString(settings.getTopP())))) {
            settings.setTopP(doubleConverter.fromString(topPField.getText()));
        }

        if (isFieldModified(timeoutField, settings.getTimeout())) {
            settings.setTimeout(safeCastToInteger(timeoutField.getValue()));
        }

        if (isFieldModified(retryField, settings.getMaxRetries())) {
            settings.setMaxRetries(safeCastToInteger(retryField.getValue()));
        }

        if (isFieldModified(testPromptField, settings.getTestPrompt())) {
            settings.setTestPrompt(testPromptField.getText());
        }

        if (isFieldModified(explainPromptField, settings.getExplainPrompt())) {
            settings.setExplainPrompt(explainPromptField.getText());
        }

        if (isFieldModified(reviewPromptField, settings.getReviewPrompt())) {
            settings.setReviewPrompt(reviewPromptField.getText());
        }

        if (isFieldModified(customPromptField, settings.getCustomPrompt())) {
            settings.setCustomPrompt(customPromptField.getText());
        }

        if (isFieldModified(openAiKeyField, settings.getOpenAIKey())) {
            settings.setOpenAIKey(new String(openAiKeyField.getPassword()));
        }

        if (isFieldModified(mistralKeyField, settings.getMistralKey())) {
            settings.setMistralKey(new String(mistralKeyField.getPassword()));
        }

        if (isFieldModified(anthropicKeyField, settings.getAnthropicKey())) {
            settings.setAnthropicKey(new String(anthropicKeyField.getPassword()));
        }

        if (isFieldModified(groqKeyField, settings.getGroqKey())) {
            settings.setGroqKey(new String(groqKeyField.getPassword()));
        }

        if (isFieldModified(deepInfraKeyField, settings.getDeepInfraKey())) {
            settings.setDeepInfraKey(new String(deepInfraKeyField.getPassword()));
        }

        // Now notify others
        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        messageBus.syncPublisher(SettingsChangeListener.TOPIC).settingsChanged();

    }

    @Override
    public void reset() {
        SettingsState settingsState = SettingsState.getInstance();

        ollamaUrlField.setText(settingsState.getOllamaModelUrl());
        lmstudioUrlField.setText(settingsState.getLmstudioModelUrl());
        gpt4allUrlField.setText(settingsState.getGpt4allModelUrl());

        testPromptField.setText(settingsState.getTestPrompt());
        explainPromptField.setText(settingsState.getExplainPrompt());
        reviewPromptField.setText(settingsState.getReviewPrompt());
        customPromptField.setText(settingsState.getCustomPrompt());

        setValue(temperatureField, settingsState.getTemperature());
        setValue(topPField, settingsState.getTopP());
        setValue(timeoutField, settingsState.getTimeout());
        setValue(retryField, settingsState.getMaxRetries());
    }

    private static void setValue(JFormattedTextField field, Number value) {
        try {
            NumberFormat format = NumberFormat.getInstance(Locale.getDefault());

            if (value instanceof Double) {
                if (format instanceof DecimalFormat) {
                    ((DecimalFormat) format).applyPattern("#0.00");  // Ensures one decimal place in the display
                }
            } else {
                format.setParseIntegerOnly(true);
            }
            field.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(format)));
            field.setValue(value);
        } catch (IllegalArgumentException e) {
            // Handle the case where the string cannot be parsed to a number
            field.setValue(0);
        }
    }

    private Integer safeCastToInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}
