package com.devoxx.genie.ui.settings.security;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SecurityScanSettingsConfigurable implements Configurable {

    private final SecurityScanSettingsComponent component;

    public SecurityScanSettingsConfigurable(@SuppressWarnings("unused") Project project) {
        component = new SecurityScanSettingsComponent();
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Security Scanning";
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
