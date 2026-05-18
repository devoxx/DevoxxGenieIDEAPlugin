package com.devoxx.genie.ui.settings.skill;

import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for the langchain4j Skills settings tab (issue #1040).
 */
public class SkillsSettingsConfigurable implements Configurable {

    private final Project project;
    private final SkillsSettingsComponent component;

    public SkillsSettingsConfigurable(Project project) {
        this.project = project;
        this.component = new SkillsSettingsComponent(project);
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Skills";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return component.createPanelWithHelp();
    }

    @Override
    public boolean isModified() {
        return component.isModified();
    }

    @Override
    public void apply() {
        component.apply();
        // Notify listeners so the Compose welcome screen rebuilds its skills list
        // immediately on Apply/OK instead of waiting for the next tab switch.
        // Re-uses the existing Commands change topic — the welcome screen treats this
        // event as "any user-facing slash content changed" and re-reads both commands
        // and skills.
        project.getMessageBus()
                .syncPublisher(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC)
                .onCustomPromptsChanged();
    }

    @Override
    public void reset() {
        component.reset();
    }
}
