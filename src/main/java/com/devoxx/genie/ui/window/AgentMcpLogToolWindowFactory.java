package com.devoxx.genie.ui.window;

import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.panel.log.AgentMcpLogPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the unified Agent + MCP activity log tool window.
 */
public class AgentMcpLogToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ApplicationManager.getApplication().invokeLater(() -> {
            AgentMcpLogPanel panel = new AgentMcpLogPanel(project);
            Content content = ContentFactory.getInstance().createContent(panel, "DevoxxGenie Logs", false);
            toolWindow.getContentManager().addContent(content);
        });
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        DevoxxGenieStateService s = DevoxxGenieStateService.getInstance();
        boolean hasCliTools = s.getCliTools() != null && !s.getCliTools().isEmpty();
        return Boolean.TRUE.equals(s.getAgentModeEnabled()) || MCPService.isMCPEnabled() || hasCliTools;
    }

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setStripeTitle("DevoxxGenie Logs");
        toolWindow.setToHideOnEmptyContent(false);
    }
}
