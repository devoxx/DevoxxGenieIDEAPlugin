package com.devoxx.genie.ui.settings.mcp;

import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for MCP (Multi-Agent Communications Protocol) settings.
 */
public class MCPSettingsConfigurable implements Configurable {

    private final MessageBus messageBus;
    private MCPSettingsComponent mcpSettingsComponent;

    public MCPSettingsConfigurable(@NotNull Project project) {
        this.messageBus = project.getMessageBus();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "MCP Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mcpSettingsComponent = new MCPSettingsComponent();
        return mcpSettingsComponent.createPanel();
    }

    @Override
    public boolean isModified() {
        return mcpSettingsComponent != null && mcpSettingsComponent.isModified();
    }

    @Override
    public void apply() {
        if (mcpSettingsComponent != null) {
            mcpSettingsComponent.apply();
            
            // TODO Check if things have changed, if so Notify listeners that settings have changed
            messageBus.syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC).settingsChanged(true);
        }
    }

    @Override
    public void reset() {
        if (mcpSettingsComponent != null) {
            mcpSettingsComponent.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        mcpSettingsComponent = null;
    }
}
