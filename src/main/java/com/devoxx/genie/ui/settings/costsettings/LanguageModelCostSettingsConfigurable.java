package com.devoxx.genie.ui.settings.costsettings;

import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LanguageModelCostSettingsConfigurable implements Configurable {

    private final MessageBus messageBus;

    public LanguageModelCostSettingsConfigurable(@NotNull Project project) {
        this.messageBus = project.getMessageBus();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "LLM Costs";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return new LanguageModelCostSettingsComponent().createPanel();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() {
        // Notify listeners that settings have changed
        messageBus.syncPublisher(AppTopics.LLM_SETTINGS_CHANGED_TOPIC).llmSettingsChanged();
    }
}
