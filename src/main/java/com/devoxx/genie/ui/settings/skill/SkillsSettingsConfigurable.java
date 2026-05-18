package com.devoxx.genie.ui.settings.skill;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for the langchain4j Skills settings tab (issue #1040).
 */
public class SkillsSettingsConfigurable implements Configurable {

    private final SkillsSettingsComponent component;

    public SkillsSettingsConfigurable(Project project) {
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
    }

    @Override
    public void reset() {
        component.reset();
    }
}
