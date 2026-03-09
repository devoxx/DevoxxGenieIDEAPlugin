package com.devoxx.genie.ui.settings.automation;

import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for Event Automation settings.
 */
public class EventAutomationSettingsConfigurable implements Configurable {

    private final MessageBus messageBus;
    private EventAutomationSettingsComponent component;

    public EventAutomationSettingsConfigurable(@NotNull Project project) {
        this.messageBus = project.getMessageBus();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Event Automations (BETA)";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        component = new EventAutomationSettingsComponent();
        return component.createPanelWithHelp();
    }

    @Override
    public boolean isModified() {
        return component != null && component.isModified();
    }

    @Override
    public void apply() {
        if (component != null) {
            component.apply();
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
