package com.devoxx.genie.ui;

import com.devoxx.genie.ui.panel.mcp.MCPLogPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the MCP Log tool window
 */
public class MCPLogToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Use invokeLater to avoid race conditions during initialization
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            MCPLogPanel mcpLogPanel = new MCPLogPanel(project);
            Content content = ContentFactory.getInstance().createContent(
                    mcpLogPanel, 
                    "DevoxxGenie MCP Logs", 
                    false
            );
            toolWindow.getContentManager().addContent(content);
        });
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // Only show this tool window when MCP is enabled
        // When enabled the user can turn on/off MCP logging
        return com.devoxx.genie.service.mcp.MCPService.isMCPEnabled();
    }
    
    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setStripeTitle("DevoxxGenie MCP Logs");
        toolWindow.setToHideOnEmptyContent(false);
    }
}
