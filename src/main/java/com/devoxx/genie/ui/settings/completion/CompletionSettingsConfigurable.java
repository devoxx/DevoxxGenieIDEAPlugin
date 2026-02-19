package com.devoxx.genie.ui.settings.completion;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CompletionSettingsConfigurable implements Configurable {

    private CompletionSettingsComponent settingsComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Inline Completion";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsComponent = new CompletionSettingsComponent();
        settingsComponent.addListeners();
        return settingsComponent.createPanelWithHelp();
    }

    @Override
    public boolean isModified() {
        return settingsComponent != null && settingsComponent.isModified();
    }

    @Override
    public void apply() {
        if (settingsComponent != null) {
            settingsComponent.apply();
        }
    }

    @Override
    public void reset() {
        if (settingsComponent != null) {
            settingsComponent.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}
