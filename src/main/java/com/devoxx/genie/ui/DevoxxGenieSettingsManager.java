package com.devoxx.genie.ui;

import com.devoxx.genie.service.SettingsStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.function.Consumer;

import static com.intellij.openapi.options.Configurable.isFieldModified;

public class DevoxxGenieSettingsManager implements Configurable {

    private JTextField ollamaUrlField;
    private JTextField lmstudioUrlField;
    private JTextField gpt4allUrlField;
    private JTextField janUrlField;

    private JPasswordField openAiKeyField;
    private JPasswordField mistralKeyField;
    private JPasswordField anthropicKeyField;
    private JPasswordField groqKeyField;
    private JPasswordField deepInfraKeyField;
    private JPasswordField geminiKeyField;

    private JPasswordField tavilySearchKeyField;
    private JPasswordField googleSearchKeyField;
    private JPasswordField googleCSIKeyField;

    private JCheckBox hideSearchCheckBox;

    @Nls
    @Override
    public String getDisplayName() {
        return "Devoxx Genie Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        SettingsStateService settings = SettingsStateService.getInstance();

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
        janUrlField = addFieldWithLinkButton(settingsPanel, gbc, "Jan URL:", settings.getJanModelUrl(), "https://jan.ai/download");

        setTitle("Cloud based Large Language Models", settingsPanel, gbc);

        openAiKeyField = addFieldWithLabelPasswordAndLinkButton(settingsPanel, gbc, "OpenAI API Key :", settings.getOpenAIKey(), "https://platform.openai.com/api-keys");
        mistralKeyField = addFieldWithLabelPasswordAndLinkButton(settingsPanel, gbc, "Mistral API Key :", settings.getMistralKey(), "https://console.mistral.ai/api-keys");
        anthropicKeyField = addFieldWithLabelPasswordAndLinkButton(settingsPanel, gbc, "Anthropic API Key :", settings.getAnthropicKey(), "https://console.anthropic.com/settings/keys");
        groqKeyField = addFieldWithLabelPasswordAndLinkButton(settingsPanel, gbc, "Groq API Key :", settings.getGroqKey(), "https://console.groq.com/keys");
        deepInfraKeyField = addFieldWithLabelPasswordAndLinkButton(settingsPanel, gbc, "DeepInfra API Key :", settings.getDeepInfraKey(), "https://deepinfra.com/dash/api_keys");
        geminiKeyField = addFieldWithLabelPasswordAndLinkButton(settingsPanel, gbc, "Gemini API Key :", settings.getGeminiKey(), "https://aistudio.google.com/app/apikey");

        setTitle("Search Providers", settingsPanel, gbc);
        hideSearchCheckBox = addCheckBoxWithLabel(settingsPanel, gbc, "Hide Search Providers", false, "", false);
        tavilySearchKeyField = addFieldWithLabelPasswordAndLinkButton(settingsPanel, gbc, "Tavily Web Search API Key :", settings.getTavilySearchKey(), "https://app.tavily.com/home");
        googleSearchKeyField = addFieldWithLabelPasswordAndLinkButton(settingsPanel, gbc, "Google Web Search API Key :", settings.getGoogleSearchKey(), "https://developers.google.com/custom-search/docs/paid_element#api_key");
        googleCSIKeyField = addFieldWithLabelPasswordAndLinkButton(settingsPanel, gbc, "Google Custom Search Engine ID :", settings.getGoogleCSIKey(), "https://programmablesearchengine.google.com/controlpanel/create");

        hideSearchCheckBox.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            tavilySearchKeyField.setEnabled(!selected);
            googleSearchKeyField.setEnabled(!selected);
            googleCSIKeyField.setEnabled(!selected);
        });

        return settingsPanel;
    }

    /**
     * Set the title of the settings panel
     *
     * @param title         the title
     * @param settingsPanel the settings panel
     * @param gbc           the grid bag constraints
     */
    private void setTitle(String title,
                          @NotNull JPanel settingsPanel,
                          @NotNull GridBagConstraints gbc) {
        JLabel titleLabel = new JLabel(title);

        gbc.insets = JBUI.insets(10, 0);
        settingsPanel.add(titleLabel, gbc);

        // Reset the insets for the next component
        gbc.insets = JBUI.emptyInsets();

        // Add vertical spacing below the title
        gbc.weighty = 1.0; // Allow the empty space to expand vertically
        settingsPanel.add(new JLabel(), gbc);

        // Reset the constraints for the next component
        gbc.weighty = 0.0;
        resetGbc(gbc);
    }

    /**
     * Add a field with label and a link button
     *
     * @param panel the panel
     * @param gbc   the gridbag constraints
     * @param label the label
     * @param value the value
     * @param url   the url
     * @return the text field
     */
    private @NotNull JTextField addFieldWithLinkButton(@NotNull JPanel panel,
                                                       GridBagConstraints gbc,
                                                       String label,
                                                       String value,
                                                       String url) {
        String wwwEmoji = "\uD83D\uDD17";
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

    /**
     * Add field with label, password and link button
     *
     * @param panel the panel
     * @param gbc   the gridbag constraints
     * @param label the label
     * @param value the value
     * @param url   the url
     * @return the password field
     */
    private @NotNull JPasswordField addFieldWithLabelPasswordAndLinkButton(@NotNull JPanel panel,
                                                                           GridBagConstraints gbc,
                                                                           String label,
                                                                           String value,
                                                                           String url) {
        String keyEmoji = "\uD83D\uDD11";
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
     *
     * @param emoji      the emoji to use in the button
     * @param toolTipMsg the tooltip message
     * @param url        the url to open when clicked
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
            } catch (Exception ex) {
                Project project = ProjectManager.getInstance().getOpenProjects()[0];
                NotificationUtil.sendNotification(project, "Error: Unable to open the link");
            }
        });
        return btnApiKey;
    }

    /**
     * Add a formatted field with label
     *
     * @param panel the panel
     * @param gbc   the grid bag constraints
     * @param label the label
     * @param value the value
     * @param tooltip the tooltip
     * @return the formatted field
     */
    private @NotNull JCheckBox addCheckBoxWithLabel(@NotNull JPanel panel,
                                                    GridBagConstraints gbc,
                                                    String label,
                                                    Boolean value,
                                                    @NotNull String tooltip,
                                                    boolean labelInNextColumn) {
        JCheckBox checkBox = new JCheckBox();
        if (!tooltip.isEmpty()) {
            checkBox.setToolTipText(tooltip);
        }
        if (value != null) {
            checkBox.setSelected(value);
        }

        if (labelInNextColumn) {
            JPanel jPanel = new JPanel();
            jPanel.setLayout(new BorderLayout());
            jPanel.add(new JLabel(label), BorderLayout.CENTER);
            jPanel.add(checkBox, BorderLayout.WEST);
            gbc.gridx++;
            panel.add(jPanel, gbc);
        } else {
            panel.add(new JLabel(label), gbc);
            gbc.gridx++;
            panel.add(checkBox, gbc);
        }

        resetGbc(gbc);
        return checkBox;
    }

    private void resetGbc(@NotNull GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
    }

    @Override
    public boolean isModified() {
        SettingsStateService settings = SettingsStateService.getInstance();

        boolean isModified = isFieldModified(ollamaUrlField, settings.getOllamaModelUrl());
        isModified |= isFieldModified(lmstudioUrlField, settings.getLmstudioModelUrl());
        isModified |= isFieldModified(gpt4allUrlField, settings.getGpt4allModelUrl());
        isModified |= isFieldModified(janUrlField, settings.getJanModelUrl());

        isModified |= isFieldModified(openAiKeyField, settings.getOpenAIKey());
        isModified |= isFieldModified(mistralKeyField, settings.getMistralKey());
        isModified |= isFieldModified(anthropicKeyField, settings.getAnthropicKey());
        isModified |= isFieldModified(groqKeyField, settings.getGroqKey());
        isModified |= isFieldModified(deepInfraKeyField, settings.getDeepInfraKey());
        isModified |= isFieldModified(geminiKeyField, settings.getGeminiKey());

        isModified |= !settings.getHideSearchButtonsFlag().equals(hideSearchCheckBox.isSelected());
        isModified |= isFieldModified(tavilySearchKeyField, settings.getTavilySearchKey());
        isModified |= isFieldModified(googleSearchKeyField, settings.getGoogleSearchKey());
        isModified |= isFieldModified(googleCSIKeyField, settings.getGoogleCSIKey());

        return isModified;
    }

    @Override
    public void apply() {
        SettingsStateService settings = SettingsStateService.getInstance();

        boolean apiKeyModified = false;
        apiKeyModified |= updateSettingIfModified(openAiKeyField, settings.getOpenAIKey(), settings::setOpenAIKey);
        apiKeyModified |= updateSettingIfModified(mistralKeyField, settings.getMistralKey(), settings::setMistralKey);
        apiKeyModified |= updateSettingIfModified(anthropicKeyField, settings.getAnthropicKey(), settings::setAnthropicKey);
        apiKeyModified |= updateSettingIfModified(groqKeyField, settings.getGroqKey(), settings::setGroqKey);
        apiKeyModified |= updateSettingIfModified(deepInfraKeyField, settings.getDeepInfraKey(), settings::setDeepInfraKey);
        apiKeyModified |= updateSettingIfModified(geminiKeyField, settings.getGeminiKey(), settings::setGeminiKey);

        apiKeyModified |= updateSettingIfModified(hideSearchCheckBox, settings.getHideSearchButtonsFlag(), value ->
            settings.setHideSearchButtonsFlag(Boolean.parseBoolean(value))
        );
        apiKeyModified |= updateSettingIfModified(tavilySearchKeyField, settings.getTavilySearchKey(), settings::setTavilySearchKey);
        apiKeyModified |= updateSettingIfModified(googleSearchKeyField, settings.getGoogleSearchKey(), settings::setGoogleSearchKey);
        apiKeyModified |= updateSettingIfModified(googleCSIKeyField, settings.getGoogleCSIKey(), settings::setGoogleCSIKey);

        if (apiKeyModified) {
            // Only notify the listener if an API key has changed, so we can refresh the LLM providers list in the UI
            notifySettingsChanged();
        }
        updateSettingIfModified(hideSearchCheckBox, settings.getHideSearchButtonsFlag(), value -> {
            settings.setHideSearchButtonsFlag(Boolean.parseBoolean(value));
        });

        updateSettingIfModified(ollamaUrlField, settings.getOllamaModelUrl(), settings::setOllamaModelUrl);
        updateSettingIfModified(lmstudioUrlField, settings.getLmstudioModelUrl(), settings::setLmstudioModelUrl);
        updateSettingIfModified(gpt4allUrlField, settings.getGpt4allModelUrl(), settings::setGpt4allModelUrl);
        updateSettingIfModified(janUrlField, settings.getJanModelUrl(), settings::setJanModelUrl);
    }

    /**
     * Update the setting if the field value has changed
     * @param field        the field
     * @param currentValue the current value
     * @param updateAction the update action
     */
    public boolean updateSettingIfModified(JComponent field,
                                           Object currentValue,
                                           Consumer<String> updateAction) {
        String newValue = extractStringValue(field);
        if (newValue != null && !newValue.equals(currentValue)) {
            updateAction.accept(newValue);
            return true;
        }
        return false;
    }

    /**
     * Extract the string value from the field
     * @param field the field
     * @return the string value
     */
    private @Nullable String extractStringValue(JComponent field) {
        if (field instanceof JTextField jtextfield) {
            return jtextfield.getText();
        } else if (field instanceof JCheckBox jcheckbox) {
            return Boolean.toString(jcheckbox.isSelected());
        }
        return null;
    }

    /**
     * Notify the listeners that the settings have changed
     */
    private void notifySettingsChanged() {
        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        messageBus.syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC).settingsChanged();
    }

    @Override
    public void reset() {
        SettingsStateService settingsState = SettingsStateService.getInstance();

        ollamaUrlField.setText(settingsState.getOllamaModelUrl());
        lmstudioUrlField.setText(settingsState.getLmstudioModelUrl());
        gpt4allUrlField.setText(settingsState.getGpt4allModelUrl());
        janUrlField.setText(settingsState.getJanModelUrl());

        hideSearchCheckBox.setSelected(settingsState.getHideSearchButtonsFlag());
    }
}
