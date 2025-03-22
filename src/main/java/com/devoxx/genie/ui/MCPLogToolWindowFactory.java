package com.devoxx.genie.ui;

import com.devoxx.genie.service.mcp.MCPService;
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
        MCPLogPanel mcpLogPanel = new MCPLogPanel(project);
        Content content = ContentFactory.getInstance().createContent(
                mcpLogPanel, 
                "DevoxxGenie MCP Logs", 
                false
        );
        toolWindow.getContentManager().addContent(content);
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // Always show the tool window, even if MCP is disabled
        // This allows users to enable MCP from within the tool window
        return true;
    }
    
    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setStripeTitle("DevoxxGenie MCP Logs");
        toolWindow.setToHideOnEmptyContent(false);
    }
}
