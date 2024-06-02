package com.devoxx.genie.ui;

import com.devoxx.genie.service.SettingsStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.DoubleConverter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import static com.intellij.openapi.options.Configurable.isFieldModified;

public class DevoxxGenieLLMSettingsManager implements Configurable {

    private final DoubleConverter doubleConverter;

    private JFormattedTextField temperatureField;
    private JFormattedTextField topPField;

    private JFormattedTextField timeoutField;
    private JFormattedTextField retryField;
    private JFormattedTextField chatMemorySizeField;

    private JTextField maxOutputTokensField;

    private JCheckBox astModeCheckBox;
    private JCheckBox astParentClassCheckBox;
    private JCheckBox astReferenceFieldCheckBox;
    private JCheckBox astReferenceClassesCheckBox;

    private JCheckBox streamModeCheckBox;

    public DevoxxGenieLLMSettingsManager() {
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
        SettingsStateService settings = SettingsStateService.getInstance();

        JPanel settingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        setTitle("LLM Settings", settingsPanel, gbc);

        streamModeCheckBox = addCheckBoxWithLabel(settingsPanel, gbc, "Enable Stream Mode (Beta)", settings.getStreamMode(),
            "Streaming response does not support (yet) copy code buttons to clipboard", false);

        setTitle("LLM Parameters", settingsPanel, gbc);

        chatMemorySizeField = addFormattedFieldWithLabel(settingsPanel, gbc, "Chat memory size:", settings.getTemperature());
        temperatureField = addFormattedFieldWithLabel(settingsPanel, gbc, "Temperature:", settings.getTemperature());
        topPField = addFormattedFieldWithLabel(settingsPanel, gbc, "Top-P:", settings.getTopP());
        maxOutputTokensField = addTextFieldWithLabel(settingsPanel, gbc, "Maximum output tokens :", settings.getMaxOutputTokens());
        timeoutField = addFormattedFieldWithLabel(settingsPanel, gbc, "Timeout (in secs):", settings.getTimeout());
        retryField = addFormattedFieldWithLabel(settingsPanel, gbc, "Maximum retries :", settings.getMaxRetries());

        setTitle("Abstract Syntax Tree Config", settingsPanel, gbc);
        astModeCheckBox = addCheckBoxWithLabel(settingsPanel, gbc, "Enable AST Mode (Beta)", settings.getAstMode(),
            "Enable Abstract Syntax Tree mode for code generation, results in including related classes in the prompt.", false);
        astParentClassCheckBox = addCheckBoxWithLabel(settingsPanel, gbc, "Include project parent class(es)", settings.getAstParentClass(), "", true);
        astReferenceClassesCheckBox = addCheckBoxWithLabel(settingsPanel, gbc, "Include class references", settings.getAstClassReference(), "", true);
        astReferenceFieldCheckBox = addCheckBoxWithLabel(settingsPanel, gbc, "Include field references", settings.getAstFieldReference(), "", true);

        astModeCheckBox.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            astParentClassCheckBox.setEnabled(selected);
            astReferenceClassesCheckBox.setEnabled(selected);
            astReferenceFieldCheckBox.setEnabled(selected);
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
     * Add a text field with label
     * @param panel the panel
     * @param gbc   the gridbag constraints
     * @param label the label
     * @param value the value
     * @return the text field
     */
    private @NotNull JTextField addTextFieldWithLabel(@NotNull JPanel panel,
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

    /**
     * Add a formatted field with label
     *
     * @param panel the panel
     * @param gbc   the gridbag constraints
     * @param label the label
     * @param value the value
     * @return the formatted field
     */
    private @NotNull JFormattedTextField addFormattedFieldWithLabel(@NotNull JPanel panel,
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

        boolean isModified = false;
        isModified |= !settings.getStreamMode().equals(streamModeCheckBox.isSelected());

        isModified |= isFieldModified(temperatureField, Objects.requireNonNull(doubleConverter.toString(settings.getTemperature())));
        isModified |= isFieldModified(topPField, Objects.requireNonNull(doubleConverter.toString(settings.getTopP())));
        isModified |= isFieldModified(timeoutField, settings.getTimeout());
        isModified |= isFieldModified(maxOutputTokensField, settings.getMaxOutputTokens());
        isModified |= isFieldModified(retryField, settings.getMaxRetries());
        isModified |= isFieldModified(chatMemorySizeField, settings.getChatMemorySize());

        isModified |= !settings.getAstMode().equals(astModeCheckBox.isSelected());
        isModified |= !settings.getAstParentClass().equals(astParentClassCheckBox.isSelected());
        isModified |= !settings.getAstClassReference().equals(astReferenceClassesCheckBox.isSelected());
        isModified |= !settings.getAstFieldReference().equals(astReferenceFieldCheckBox.isSelected());

        return isModified;
    }

    @Override
    public void apply() {
        SettingsStateService settings = SettingsStateService.getInstance();

        boolean apiKeyModified = false;

        if (apiKeyModified) {
            // Only notify the listener if an API key has changed, so we can refresh the LLM providers list in the UI
            notifySettingsChanged();
        }

        updateSettingIfModified(temperatureField, doubleConverter.toString(settings.getTemperature()), value -> settings.setTemperature(doubleConverter.fromString(value)));
        updateSettingIfModified(topPField, doubleConverter.toString(settings.getTopP()), value -> settings.setTopP(doubleConverter.fromString(value)));
        updateSettingIfModified(timeoutField, settings.getTimeout(), value -> settings.setTimeout(safeCastToInteger(value)));
        updateSettingIfModified(retryField, settings.getMaxRetries(), value -> settings.setMaxRetries(safeCastToInteger(value)));
        updateSettingIfModified(maxOutputTokensField, settings.getMaxOutputTokens(), settings::setMaxOutputTokens);

        updateSettingIfModified(astModeCheckBox, settings.getAstMode(), value -> settings.setAstMode(Boolean.parseBoolean(value)));
        updateSettingIfModified(astParentClassCheckBox, settings.getAstParentClass(), value -> settings.setAstParentClass(Boolean.parseBoolean(value)));
        updateSettingIfModified(astReferenceClassesCheckBox, settings.getAstClassReference(), value -> settings.setAstClassReference(Boolean.parseBoolean(value)));
        updateSettingIfModified(astReferenceFieldCheckBox, settings.getAstFieldReference(), value -> settings.setAstFieldReference(Boolean.parseBoolean(value)));

        // Notify the listeners if the chat memory size has changed
        if (updateSettingIfModified(chatMemorySizeField, settings.getChatMemorySize(), value -> settings.setChatMemorySize(safeCastToInteger(value)))) {
            notifyChatMemorySizeChangeListeners();
        }

        updateSettingIfModified(streamModeCheckBox, settings.getStreamMode(), value -> settings.setStreamMode(Boolean.parseBoolean(value)));
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
     * Notify the chat memory size change listeners
     */
    public void notifyChatMemorySizeChangeListeners() {
        ApplicationManager.getApplication().getMessageBus()
            .syncPublisher(AppTopics.CHAT_MEMORY_SIZE_TOPIC)
            .onChatMemorySizeChanged(SettingsStateService.getInstance().getChatMemorySize());
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

        chatMemorySizeField.setText(String.valueOf(settingsState.getChatMemorySize()));
        maxOutputTokensField.setText(settingsState.getMaxOutputTokens());

        astReferenceFieldCheckBox.setSelected(settingsState.getAstFieldReference());
        astParentClassCheckBox.setSelected(settingsState.getAstParentClass());
        astReferenceClassesCheckBox.setSelected(settingsState.getAstClassReference());
        streamModeCheckBox.setSelected(settingsState.getStreamMode());

        setValue(temperatureField, settingsState.getTemperature());
        setValue(topPField, settingsState.getTopP());
        setValue(timeoutField, settingsState.getTimeout());
        setValue(retryField, settingsState.getMaxRetries());
        setValue(chatMemorySizeField, settingsState.getChatMemorySize());
    }

    /**
     * Set the value of the field
     * @param field the field
     * @param value the value
     */
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

    /**
     * Safely cast a string to an integer
     *
     * @param value the string value
     * @return the integer value
     */
    private @NotNull Integer safeCastToInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
