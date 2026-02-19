package com.devoxx.genie.ui.settings.agent;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AgentSettingsConfigurable implements Configurable {

    private final Project project;
    private final MessageBus messageBus;
    private AgentSettingsComponent agentSettingsComponent;

    public AgentSettingsConfigurable(@NotNull Project project) {
        this.project = project;
        this.messageBus = project.getMessageBus();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Agent Mode";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        agentSettingsComponent = new AgentSettingsComponent();
        return agentSettingsComponent.createPanelWithHelp();
    }

    @Override
    public boolean isModified() {
        return agentSettingsComponent != null && agentSettingsComponent.isModified();
    }

    @Override
    public void apply() {
        if (agentSettingsComponent != null) {
            agentSettingsComponent.apply();
            messageBus.syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC).settingsChanged(true);

            // Auto-open the Agent Logs panel when debug logging is enabled
            if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentDebugLogsEnabled())) {
                ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                        .getToolWindow("DevoxxGenieAgentLogs");
                if (toolWindow != null) {
                    toolWindow.show();
                }
            }
        }
    }

    @Override
    public void reset() {
        if (agentSettingsComponent != null) {
            agentSettingsComponent.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        agentSettingsComponent = null;
    }
}
