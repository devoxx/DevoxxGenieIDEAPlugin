package com.devoxx.genie.ui.settings.runner;

import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for CLI/ACP Runners settings.
 * Registered in plugin.xml under the DevoxxGenie parent settings group.
 */
public class RunnerSettingsConfigurable implements Configurable {

    private final Project project;
    private final MessageBus messageBus;
    private RunnerSettingsComponent runnerSettingsComponent;

    public RunnerSettingsConfigurable(@NotNull Project project) {
        this.project = project;
        this.messageBus = project.getMessageBus();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "CLI/ACP Runners";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        runnerSettingsComponent = new RunnerSettingsComponent(project);
        return runnerSettingsComponent.createPanelWithHelp();
    }

    @Override
    public boolean isModified() {
        return runnerSettingsComponent != null && runnerSettingsComponent.isModified();
    }

    @Override
    public void apply() {
        if (runnerSettingsComponent != null) {
            runnerSettingsComponent.apply();
            messageBus.syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC).settingsChanged(true);
        }
    }

    @Override
    public void reset() {
        if (runnerSettingsComponent != null) {
            runnerSettingsComponent.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        runnerSettingsComponent = null;
    }
}
