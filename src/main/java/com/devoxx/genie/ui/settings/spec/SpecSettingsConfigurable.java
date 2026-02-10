package com.devoxx.genie.ui.settings.spec;

import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for Spec Driven Development settings.
 * Registered in plugin.xml under the DevoxxGenie parent settings group.
 */
public class SpecSettingsConfigurable implements Configurable {

    private final MessageBus messageBus;
    private SpecSettingsComponent specSettingsComponent;

    public SpecSettingsConfigurable(@NotNull Project project) {
        this.messageBus = project.getMessageBus();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Spec Driven Dev";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        specSettingsComponent = new SpecSettingsComponent();
        return specSettingsComponent.createPanel();
    }

    @Override
    public boolean isModified() {
        return specSettingsComponent != null && specSettingsComponent.isModified();
    }

    @Override
    public void apply() {
        if (specSettingsComponent != null) {
            specSettingsComponent.apply();
            messageBus.syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC).settingsChanged(true);
        }
    }

    @Override
    public void reset() {
        if (specSettingsComponent != null) {
            specSettingsComponent.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        specSettingsComponent = null;
    }
}
