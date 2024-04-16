package com.devoxx.genie.ui;

import com.devoxx.genie.ui.util.DoubleConverter;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
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

    private JFormattedTextField temperatureField;
    private JFormattedTextField topPField;

    private JFormattedTextField timeoutField;
    private JFormattedTextField retryField;

    private JTextField testPromptField;
    private JTextField explainPromptField;
    private JTextField reviewPromptField;

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

        JPanel settingsPanel = new JPanel(new GridBagLayout());
        SettingsState settings = SettingsState.getInstance();

        // Add 3 input fields to provide URL per Language model
        ollamaUrlField = new JTextField();
        ollamaUrlField.setText(settings.getOllamaModelUrl());

        lmstudioUrlField = new JTextField();
        lmstudioUrlField.setText(settings.getLmstudioModelUrl());

        gpt4allUrlField = new JTextField();
        gpt4allUrlField.setText(settings.getGpt4allModelUrl());

        temperatureField = new JFormattedTextField();
        setValue(temperatureField, settings.getTemperature());

        topPField = new JFormattedTextField();
        setValue(topPField, settings.getTopP());

        timeoutField = new JFormattedTextField();
        setValue(timeoutField, settings.getTimeout());

        // Prompts
        testPromptField = new JTextField();
        testPromptField.setText(settings.getTestPrompt());

        explainPromptField = new JTextField();
        explainPromptField.setText(settings.getExplainPrompt());

        reviewPromptField = new JTextField();
        reviewPromptField.setText(settings.getReviewPrompt());

        // retryField
        retryField = new JFormattedTextField();
        setValue(retryField, settings.getMaxRetries());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);

        // Ollama URL field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        settingsPanel.add(new JLabel("Ollama URL:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0; // Give extra horizontal space to the input field
        settingsPanel.add(ollamaUrlField, gbc);

        // LMStudio URL field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        settingsPanel.add(new JLabel("LMStudio URL:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        settingsPanel.add(lmstudioUrlField, gbc);

        // GPT4All URL field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        settingsPanel.add(new JLabel("GPT4All URL:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        settingsPanel.add(gpt4allUrlField, gbc);

        // Temperature field
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        settingsPanel.add(new JLabel("Temperature:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        settingsPanel.add(temperatureField, gbc);

        // TopP field
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        settingsPanel.add(new JLabel("Top-P:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        settingsPanel.add(topPField, gbc);

        // Timeout field
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        settingsPanel.add(new JLabel("Timeout (in secs):"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        settingsPanel.add(timeoutField, gbc);

        // Retry field
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        settingsPanel.add(new JLabel("Maximum retries :"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        settingsPanel.add(retryField, gbc);

        // Test prompt field
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        settingsPanel.add(new JLabel("Test prompt :"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        settingsPanel.add(testPromptField, gbc);

        // Explain prompt field
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        settingsPanel.add(new JLabel("Explain prompt :"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        settingsPanel.add(explainPromptField, gbc);

        // Review prompt field
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        settingsPanel.add(new JLabel("Review prompt :"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        settingsPanel.add(reviewPromptField, gbc);

        return settingsPanel;
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
