package com.devoxx.genie.ui.settings.prompt;

import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Consumer;

public class PromptSettingsConfigurable implements Configurable {

    private final PromptSettingsComponent promptSettingsComponent;

    public PromptSettingsConfigurable(Project project) {
        promptSettingsComponent = new PromptSettingsComponent(project);
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return promptSettingsComponent.createPanel();
    }

    /**
     * Get the display name
     *
     * @return the display name
     */
    @Nls
    @Override
    public String getDisplayName() {
        return "Prompts";
    }

    /**
     * Check if the settings have been modified
     *
     * @return true if the settings have been modified
     */
    @Override
    public boolean isModified() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        boolean isModified = false;

        isModified |= !StringUtil.equals(promptSettingsComponent.getSystemPromptField().getText(), settings.getSystemPrompt());

        isModified |= !settings.getCustomPrompts().equals(promptSettingsComponent.getCustomPrompts());

        return isModified;
    }

    /**
     * Apply the changes to the settings
     */
    @Override
    public void apply() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        updateTextAreaIfModified(promptSettingsComponent.getSystemPromptField(), settings.getSystemPrompt(), settings::setSystemPrompt);

        settings.setCustomPrompts(promptSettingsComponent.getCustomPrompts());

        ApplicationManager
            .getApplication()
            .getMessageBus()
            .syncPublisher(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC)
            .onCustomPromptsChanged();
    }

    /**
     * Reset the text area to the default value
     */
    @Override
    public void reset() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        promptSettingsComponent.getSystemPromptField().setText(settings.getSystemPrompt());

        promptSettingsComponent.setCustomPrompts(settings.getCustomPrompts());
    }

    /**
     * Update the text area if the value has changed
     *
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
