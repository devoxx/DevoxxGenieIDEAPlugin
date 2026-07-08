package com.devoxx.genie.ui.settings.debug;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DebugSettingsConfigurable implements Configurable {

    private final Project project;
    private DebugSettingsComponent component;

    public DebugSettingsConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Debug";
    }

    @Override
    public @Nullable JComponent createComponent() {
        component = new DebugSettingsComponent();
        return component.getPanel();
    }

    @Override
    public boolean isModified() {
        return component != null && component.isModified();
    }

    @Override
    public void apply() {
        if (component == null) {
            return;
        }
        boolean nowEnabled = component.isRawRequestResponseLoggingSelected();
        component.apply();

        // Auto-open the Activity Log panel when raw logging is turned on, so users see it start filling.
        if (nowEnabled) {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DevoxxGenieActivityLogs");
            if (toolWindow != null) {
                toolWindow.show();
            }
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
