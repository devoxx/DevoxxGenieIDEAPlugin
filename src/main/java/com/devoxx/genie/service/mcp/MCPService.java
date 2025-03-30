package com.devoxx.genie.service.mcp;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for working with MCP servers
 */
@Slf4j
public class MCPService {

    // Static flag to prevent duplicate notifications
    private static boolean notificationShown = false;

    /**
     * Check if MCP is enabled in the settings
     * 
     * @return true if MCP is enabled, false otherwise
     */
    public static boolean isMCPEnabled() {
        return DevoxxGenieStateService.getInstance().getMcpEnabled();
    }

    /**
     * Check if MCP debug logs are enabled
     * 
     * @return true if debug logs are enabled, false otherwise
     */
    public static boolean isDebugLogsEnabled() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        return isMCPEnabled() && stateService.getMcpDebugLogsEnabled();
    }
    
    /**
     * Show the MCP log panel tool window
     * 
     * @param project The current project
     */
    public static void showMCPLogPanel(com.intellij.openapi.project.Project project) {
        if (isDebugLogsEnabled() && project != null) {
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                com.intellij.openapi.wm.ToolWindow toolWindow = 
                    com.intellij.openapi.wm.ToolWindowManager.getInstance(project).getToolWindow("DevoxxGenieMCPLogs");
                if (toolWindow != null && !toolWindow.isVisible()) {
                    toolWindow.show();
                    
                    // Show a single notification when logs are first shown
                    if (!notificationShown) {
                        com.devoxx.genie.ui.util.NotificationUtil.sendNotification(
                            project, "MCP logs are enabled - check the MCP Logs panel for details");
                        notificationShown = true;
                    }
                }
            });
        }
    }
    
    /**
     * Refresh the MCP tool window visibility for all open projects
     * This should be called when MCP settings are changed
     */
    public static void refreshToolWindowVisibility() {
        com.intellij.openapi.application.ApplicationManager.getApplication().getMessageBus()
            .syncPublisher(com.devoxx.genie.ui.topic.AppTopics.SETTINGS_CHANGED_TOPIC)
            .settingsChanged(true);
    }

    /**
     * Reset the notification flag (usually when MCP is disabled)
     */
    public static void resetNotificationFlag() {
        notificationShown = false;
    }
    
    /**
     * Log a debug message if debug logs are enabled
     * 
     * @param message The message to log
     */
    public static void logDebug(String message) {
        if (isDebugLogsEnabled()) {
            log.info("[MCP Debug] {}", message);
        }
    }
    
    /**
     * Check if MCP messages should be shown in UI components
     * Convenience method that checks both if MCP is enabled and if debug logs are enabled
     * 
     * @return true if MCP messages should be shown in UI components
     */
    public static boolean shouldShowMCPMessages() {
        return isMCPEnabled() && isDebugLogsEnabled();
    }
}