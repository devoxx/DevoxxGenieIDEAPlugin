package com.devoxx.genie.ui.settings.appearance;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable component for DevoxxGenie appearance settings.
 */
public class AppearanceSettingsConfigurable implements Configurable {
    
    private AppearanceSettingsComponent settingsComponent;
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Appearance";
    }
    
    @Override
    public @Nullable JComponent createComponent() {
        settingsComponent = new AppearanceSettingsComponent();
        settingsComponent.addListeners();
        return settingsComponent.createPanel();
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
