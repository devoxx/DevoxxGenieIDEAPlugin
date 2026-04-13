package com.devoxx.genie.ui.settings.general;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Top-level "Analytics" configurable for DevoxxGenie. Currently hosts the anonymous
 * usage analytics opt-out toggle (task-206); future general toggles can live here too.
 */
public class GeneralSettingsConfigurable implements Configurable {

    private GeneralSettingsComponent component;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Analytics";
    }

    @Override
    public @Nullable JComponent createComponent() {
        component = new GeneralSettingsComponent();
        return component.getPanel();
    }

    @Override
    public boolean isModified() {
        return component != null && component.isModified();
    }

    @Override
    public void apply() {
        if (component != null) {
            component.apply();
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
