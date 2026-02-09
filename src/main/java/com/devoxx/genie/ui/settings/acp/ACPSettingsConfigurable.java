package com.devoxx.genie.ui.settings.acp;

import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for ACP (Agent Client Protocol) settings.
 * Registered in plugin.xml under the DevoxxGenie settings tree.
 */
public class ACPSettingsConfigurable implements Configurable {

    private final Project project;
    private final MessageBus messageBus;
    private ACPSettingsComponent acpSettingsComponent;

    public ACPSettingsConfigurable(@NotNull Project project) {
        this.project = project;
        this.messageBus = project.getMessageBus();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ACP Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        acpSettingsComponent = new ACPSettingsComponent();
        return acpSettingsComponent.createPanel();
    }

    @Override
    public boolean isModified() {
        return acpSettingsComponent != null && acpSettingsComponent.isModified();
    }

    @Override
    public void apply() {
        if (acpSettingsComponent != null) {
            acpSettingsComponent.apply();
            messageBus.syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC).settingsChanged(true);
        }
    }

    @Override
    public void reset() {
        if (acpSettingsComponent != null) {
            acpSettingsComponent.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        acpSettingsComponent = null;
    }
}
