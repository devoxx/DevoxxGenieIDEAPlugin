package com.devoxx.genie.ui.window;

import com.devoxx.genie.ui.panel.log.AgentMcpLogPanel;
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
        // Always available: every chat publishes its system prompt to this panel
        // (AGENT/SYSTEM_PROMPT entry), so users can inspect the exact instructions the
        // model received even in plain chat mode without agent/MCP/CLI/RAG enabled.
        return true;
    }

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setStripeTitle("DevoxxGenie Logs");
        toolWindow.setToHideOnEmptyContent(false);
    }
}
