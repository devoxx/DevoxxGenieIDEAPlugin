package com.devoxx.genie.service.mcp;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for working with MCP servers
 */
@Slf4j
public class MCPService {

    private MCPService() {}

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
     * Refresh the MCP tool window visibility for all open projects
     * This should be called when MCP settings are changed
     */
    public static void refreshToolWindowVisibility() {
        ApplicationManager.getApplication().getMessageBus()
            .syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC)
            .settingsChanged(true);
    }

    /**
     * Reset the notification flag (usually when MCP is disabled)
     */
    public static void resetNotificationFlag() {
        notificationShown = false;
    }
    
    /**
     * Log a debug message for MCP actions regardless of settings
     * This ensures the messages are always available for capture
     * 
     * @param message The message to log
     */
    public static void logDebug(String message) {
        // Always log internally but only show in IDE log if debug logs are enabled
        if (isDebugLogsEnabled()) {
            log.info("[MCP Debug] {}", message);
        } else {
            // Log at debug level so we don't pollute the logs
            log.debug("[MCP] {}", message);
        }
    }
}