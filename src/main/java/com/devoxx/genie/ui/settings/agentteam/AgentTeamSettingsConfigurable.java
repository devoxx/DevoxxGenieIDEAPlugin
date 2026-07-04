package com.devoxx.genie.ui.settings.agentteam;

import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AgentTeamSettingsConfigurable implements Configurable {

    private final MessageBus messageBus;
    private AgentTeamSettingsComponent component;

    public AgentTeamSettingsConfigurable(@NotNull Project project) {
        this.messageBus = project.getMessageBus();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Agent Team";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        component = new AgentTeamSettingsComponent();
        return component.createPanelWithHelp();
    }

    @Override
    public boolean isModified() {
        return component != null && component.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (component != null) {
            try {
                component.apply();
            } catch (IllegalArgumentException e) {
                // Keep the settings dialog open with the validation message
                throw new ConfigurationException(e.getMessage());
            }
            messageBus.syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC).settingsChanged(true);
        }
    }

    @Override
    public void reset() {
        if (component != null) {
            component.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        component = null;
    }
}
