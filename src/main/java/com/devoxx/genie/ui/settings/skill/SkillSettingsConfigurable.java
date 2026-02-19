package com.devoxx.genie.ui.settings.skill;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SkillSettingsConfigurable implements Configurable {

    private final Project project;
    private final SkillSettingsComponent skillSettingsComponent;

    public SkillSettingsConfigurable(Project project) {
        this.project = project;
        this.skillSettingsComponent = new SkillSettingsComponent(project);
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Skills";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return skillSettingsComponent.createPanelWithHelp();
    }

    @Override
    public boolean isModified() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        return !settings.getCustomPrompts().equals(skillSettingsComponent.getCustomPrompts());
    }

    @Override
    public void apply() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        settings.setCustomPrompts(skillSettingsComponent.getCustomPrompts());

        project.getMessageBus()
                .syncPublisher(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC)
                .onCustomPromptsChanged();
    }

    @Override
    public void reset() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        skillSettingsComponent.setCustomPrompts(settings.getCustomPrompts());
    }
}
