package com.devoxx.genie.ui;

import com.devoxx.genie.service.SettingsStateService;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class DevoxxGeniePromptSettingsManager implements Configurable {

    private JTextArea systemPromptField;
    private JTextArea testPromptField;
    private JTextArea explainPromptField;
    private JTextArea reviewPromptField;
    private JTextArea customPromptField;

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

        setTitle("The Prompts", settingsPanel, gbc);

        systemPromptField = addTextAreaWithLabel(settingsPanel, gbc, "System prompt :", settings.getSystemPrompt());
        testPromptField = addTextAreaWithLabel(settingsPanel, gbc, "Test prompt :", settings.getTestPrompt());
        explainPromptField = addTextAreaWithLabel(settingsPanel, gbc, "Explain prompt :", settings.getExplainPrompt());
        reviewPromptField = addTextAreaWithLabel(settingsPanel, gbc, "Review prompt :", settings.getReviewPrompt());
        customPromptField = addTextAreaWithLabel(settingsPanel, gbc, "Custom prompt :", settings.getCustomPrompt());
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
     * Add a text area with label
     * @param panel the panel
     * @param gbc   the gridbag constraints
     * @param label the label
     * @param value the value
     * @return the text field
     */
    private @NotNull JTextArea addTextAreaWithLabel(@NotNull JPanel panel,
                                                     GridBagConstraints gbc,
                                                     String label,
                                                     String value) {
        panel.add(new JLabel(label), gbc);
        gbc.gridx++;
        JTextArea textArea = new JTextArea(value, 3, 40);
        panel.add(textArea, gbc);
        resetGbc(gbc);
        return textArea;
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

        isModified |= !StringUtil.equals(systemPromptField.getText(), settings.getSystemPrompt());
        isModified |= !StringUtil.equals(testPromptField.getText(), settings.getTestPrompt());
        isModified |= !StringUtil.equals(explainPromptField.getText(), settings.getExplainPrompt());
        isModified |= !StringUtil.equals(reviewPromptField.getText(), settings.getReviewPrompt());
        isModified |= !StringUtil.equals(customPromptField.getText(), settings.getCustomPrompt());

        return isModified;
    }

    @Override
    public void apply() {
        SettingsStateService settings = SettingsStateService.getInstance();
        updateTextAreaIfModified(systemPromptField, settings.getSystemPrompt(), settings::setSystemPrompt);
        updateTextAreaIfModified(testPromptField, settings.getTestPrompt(), settings::setTestPrompt);
        updateTextAreaIfModified(explainPromptField, settings.getExplainPrompt(), settings::setExplainPrompt);
        updateTextAreaIfModified(reviewPromptField, settings.getReviewPrompt(), settings::setReviewPrompt);
        updateTextAreaIfModified(customPromptField, settings.getCustomPrompt(), settings::setCustomPrompt);
    }

    /**
     * Update the text area if the value has changed
     * @param textArea     the text area
     * @param currentValue the current value
     * @param updateAction the update action
     */
    public void updateTextAreaIfModified(@NotNull JTextArea textArea,
                                         Object currentValue,
                                         Consumer<String> updateAction) {
        String newValue = textArea.getText();
        if (newValue != null && !newValue.equals(currentValue)) {
            updateAction.accept(newValue);
        }
    }

    @Override
    public void reset() {
        SettingsStateService settingsState = SettingsStateService.getInstance();

        testPromptField.setText(settingsState.getTestPrompt());
        explainPromptField.setText(settingsState.getExplainPrompt());
        reviewPromptField.setText(settingsState.getReviewPrompt());
        customPromptField.setText(settingsState.getCustomPrompt());
    }
}
