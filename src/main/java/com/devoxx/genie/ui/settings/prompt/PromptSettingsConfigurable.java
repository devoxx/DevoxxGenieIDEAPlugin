package com.devoxx.genie.ui.settings.prompt;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Consumer;

public class PromptSettingsConfigurable implements Configurable {

    private final PromptSettingsComponent promptSettingsComponent;

    public PromptSettingsConfigurable() {
        promptSettingsComponent = new PromptSettingsComponent();
    }

    /**
     * Get the display name
     * @return the display name
     */
    @Nls
    @Override
    public String getDisplayName() {
        return "Prompts";
    }

    /**
     * Get the Prompt Settings component
     * @return the component
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        return promptSettingsComponent.createSettingsPanel();
    }

    /**
     * Check if the settings have been modified
     * @return true if the settings have been modified
     */
    @Override
    public boolean isModified() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        boolean isModified = false;

        isModified |= !StringUtil.equals(promptSettingsComponent.getSystemPromptField().getText(), settings.getSystemPrompt());
        isModified |= !StringUtil.equals(promptSettingsComponent.getTestPromptField().getText(), settings.getTestPrompt());
        isModified |= !StringUtil.equals(promptSettingsComponent.getExplainPromptField().getText(), settings.getExplainPrompt());
        isModified |= !StringUtil.equals(promptSettingsComponent.getReviewPromptField().getText(), settings.getReviewPrompt());
        isModified |= !StringUtil.equals(promptSettingsComponent.getCustomPromptField().getText(), settings.getCustomPrompt());

        return isModified;
    }

    /**
     * Apply the changes to the settings
     */
    @Override
    public void apply() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        updateTextAreaIfModified(promptSettingsComponent.getSystemPromptField(), settings.getSystemPrompt(), settings::setSystemPrompt);
        updateTextAreaIfModified(promptSettingsComponent.getTestPromptField(), settings.getTestPrompt(), settings::setTestPrompt);
        updateTextAreaIfModified(promptSettingsComponent.getExplainPromptField(), settings.getExplainPrompt(), settings::setExplainPrompt);
        updateTextAreaIfModified(promptSettingsComponent.getReviewPromptField(), settings.getReviewPrompt(), settings::setReviewPrompt);
        updateTextAreaIfModified(promptSettingsComponent.getCustomPromptField(), settings.getCustomPrompt(), settings::setCustomPrompt);
    }

    /**
     * Reset the text area to the default value
     */
    @Override
    public void reset() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        promptSettingsComponent.getSystemPromptField().setText(settings.getSystemPrompt());
        promptSettingsComponent.getTestPromptField().setText(settings.getTestPrompt());
        promptSettingsComponent.getExplainPromptField().setText(settings.getExplainPrompt());
        promptSettingsComponent.getReviewPromptField().setText(settings.getReviewPrompt());
        promptSettingsComponent.getCustomPromptField().setText(settings.getCustomPrompt());
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
}
