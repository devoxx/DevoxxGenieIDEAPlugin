package com.devoxx.genie.ui.settings.commands;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for the renamed "Commands" tab (previously "Custom Prompts" / "Skills").
 * See issue #1040.
 */
public class CommandsSettingsConfigurable implements Configurable {

    private final Project project;
    private final CommandsSettingsComponent commandsSettingsComponent;

    public CommandsSettingsConfigurable(Project project) {
        this.project = project;
        this.commandsSettingsComponent = new CommandsSettingsComponent(project);
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Commands";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return commandsSettingsComponent.createPanelWithHelp();
    }

    @Override
    public boolean isModified() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        return !settings.getCommands().equals(commandsSettingsComponent.getCommands());
    }

    @Override
    public void apply() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        settings.setCommands(commandsSettingsComponent.getCommands());

        project.getMessageBus()
                .syncPublisher(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC)
                .onCustomPromptsChanged();
    }

    @Override
    public void reset() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        commandsSettingsComponent.setCommands(settings.getCommands());
    }
}
