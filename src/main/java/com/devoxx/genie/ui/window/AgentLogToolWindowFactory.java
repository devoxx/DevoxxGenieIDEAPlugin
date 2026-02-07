package com.devoxx.genie.ui.window;

import com.devoxx.genie.ui.panel.agent.AgentLogPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the Agent Log tool window.
 */
public class AgentLogToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ApplicationManager.getApplication().invokeLater(() -> {
            AgentLogPanel agentLogPanel = new AgentLogPanel(project);
            Content content = ContentFactory.getInstance().createContent(
                    agentLogPanel,
                    "DevoxxGenie Agent Logs",
                    false
            );
            toolWindow.getContentManager().addContent(content);
        });
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentModeEnabled());
    }

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setStripeTitle("DevoxxGenie Agent Logs");
        toolWindow.setToHideOnEmptyContent(false);
    }
}
