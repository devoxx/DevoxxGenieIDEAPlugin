package com.devoxx.genie.ui;

import com.devoxx.genie.model.Constant;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Objects;

public class Settings implements Configurable {

    public static final String MODEL_PROVIDER = "com.devoxx.genie.settings.modelProvider";

    public static final String OLLAMA_MODEL_URL = "com.devoxx.genie.settings.ollamaUrl";
    public static final String LMSTUDIO_MODEL_URL = "com.devoxx.genie.settings.lmstudioUrl";
    public static final String GPT4ALL_MODEL_URL = "com.devoxx.genie.settings.gpt4allUrl";

    public static final String TEMPERATURE = "com.devoxx.genie.settings.temperature";
    public static final String TOP_P = "com.devoxx.genie.settings.topP";
    public static final String MAX_RETRIES = "com.devoxx.genie.settings.maxRetries";
    public static final String TIMEOUT = "com.devoxx.genie.settings.timeout";

    private final JPanel settingsPanel;
    private boolean isModified = false;

    public Settings() {

        // Add 3 input fields to provide URL per Language model
        final JTextField ollamaUrlField = new JTextField();
        String ollamaUrl = Objects.requireNonNullElse(
            PropertiesComponent.getInstance().getValue(OLLAMA_MODEL_URL),
            Constant.OLLAMA_MODEL_URL);
        ollamaUrlField.setText(ollamaUrl);
        ollamaUrlField.addActionListener(e -> {
            PropertiesComponent
                .getInstance()
                .setValue(OLLAMA_MODEL_URL, e.getActionCommand());
            this.isModified = true;
        });

        final JTextField lmstudioUrlField = new JTextField();
        String lmstudioUrl = Objects.requireNonNullElse(
            PropertiesComponent.getInstance().getValue(LMSTUDIO_MODEL_URL),
            Constant.LMSTUDIO_MODEL_URL);
        lmstudioUrlField.setText(lmstudioUrl);
        lmstudioUrlField.addActionListener(e -> {
            PropertiesComponent
                .getInstance()
                .setValue(LMSTUDIO_MODEL_URL, e.getActionCommand());
            this.isModified = true;
        });

        final JTextField gpt4allUrlField = new JTextField();
        String gpt4allUrl = Objects.requireNonNullElse(
            PropertiesComponent.getInstance().getValue(GPT4ALL_MODEL_URL),
            Constant.GPT4ALL_MODEL_URL);
        gpt4allUrlField.setText(gpt4allUrl);
        gpt4allUrlField.addActionListener(e -> {
            PropertiesComponent
                .getInstance()
                .setValue(GPT4ALL_MODEL_URL, e.getActionCommand());
            this.isModified = true;
        });

        final JFormattedTextField temperatureField = new JFormattedTextField();
        String temperatureValue = Objects.requireNonNullElse(
            PropertiesComponent.getInstance().getValue(TEMPERATURE),
            Constant.TEMPERATURE);
        setValue(temperatureField, temperatureValue, 0.7);
        temperatureField.addActionListener(e -> {
            PropertiesComponent
                .getInstance()
                .setValue(TEMPERATURE, e.getActionCommand());
            this.isModified = true;
        });

        final JFormattedTextField topPField = new JFormattedTextField();
        String topPValue = Objects.requireNonNullElse(
            PropertiesComponent.getInstance().getValue(TOP_P),
            Constant.TOP_P);
        setValue(topPField, topPValue, 0.7);
        topPField.addActionListener(e -> {
            PropertiesComponent
                .getInstance()
                .setValue(TOP_P, e.getActionCommand());
            this.isModified = true;
        });

        final JFormattedTextField timeoutField = new JFormattedTextField();
        String timeoutValue = Objects.requireNonNullElse(
            PropertiesComponent.getInstance().getValue(TIMEOUT),
            Constant.TIMEOUT);
        setValue(timeoutField, timeoutValue, 60);
        timeoutField.addActionListener(e -> {
            PropertiesComponent
                .getInstance()
                .setValue(TIMEOUT, e.getActionCommand());
            this.isModified = true;
        });

        // retryField
        final JFormattedTextField retryField = new JFormattedTextField();
        String retryValue = Objects.requireNonNullElse(
            PropertiesComponent.getInstance().getValue(MAX_RETRIES),
            Constant.MAX_RETRIES);
        setValue(retryField, retryValue, 3);
        retryField.addActionListener(e -> {
            PropertiesComponent
                .getInstance()
                .setValue(MAX_RETRIES, e.getActionCommand());
            this.isModified = true;
        });

        settingsPanel = new JPanel(new GridBagLayout());
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
    }

    private static void setValue(JFormattedTextField field, String value, Number defaultValue) {
        try {
            NumberFormat format = NumberFormat.getInstance(Locale.getDefault());

            if (defaultValue instanceof Double) {
                if (format instanceof DecimalFormat) {
                    ((DecimalFormat) format).applyPattern("#0.00");  // Ensures one decimal place in the display
                }
            } else {
                format.setParseIntegerOnly(true);
            }
            field.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(format)));
            Number temperature = format.parse(value);
            field.setValue(temperature);
        } catch (ParseException | IllegalArgumentException e) {
            // Handle the case where the string cannot be parsed to a number
            field.setValue(defaultValue);
        }
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Devoxx Genie Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return settingsPanel;
    }

    @Override
    public boolean isModified() {
        return isModified;
    }

    @Override
    public void apply() {
    }

    @Override
    public void reset() {
    }
}
