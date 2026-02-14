package com.devoxx.genie.ui.settings.completion;

import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
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
        return AbstractSettingsComponent.wrapWithHelpButton(
            settingsComponent.createPanel(),
            "https://genie.devoxx.com/docs/features/inline-completion");
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
