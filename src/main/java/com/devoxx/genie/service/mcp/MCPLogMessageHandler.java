package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import dev.langchain4j.mcp.client.logging.McpLogLevel;
import dev.langchain4j.mcp.client.logging.McpLogMessage;
import dev.langchain4j.mcp.client.logging.McpLogMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Custom implementation of McpLogMessageHandler to handle MCP log messages
 * This replaces the logback appender approach with a direct handler.
 */
@Slf4j
public class MCPLogMessageHandler implements McpLogMessageHandler {

    /**
     * Handle an MCP log message by transforming it into an MCPMessage
     * and publishing it to the application message bus.
     *
     * @param message   The log message content
     */

    @Override
    public void handleLogMessage(McpLogMessage message) {

        // Only process the message if MCP debug logs are enabled
        if (!MCPService.isDebugLogsEnabled()) {
            return;
        }

        // Format the message with protocol direction markers (< for incoming, > for outgoing)
        // Maintain the same format as the previous implementation for compatibility
        String formattedMessage = formatLogMessage(message.data().asText());
        
        // Log at the appropriate level based on the MCP log level
        logAtLevel(message.level(), formattedMessage);
        
        // Publish the message to the application message bus
        publishToBus(formattedMessage);
    }
    
    /**
     * Format the log message with direction markers (< for incoming, > for outgoing)
     * This preserves the format used in the previous implementation.
     * 
     * @param message The message to format
     * @return The formatted message
     */
    private @NotNull String formatLogMessage(@NotNull String message) {
        // The original implementation relied on the stdio transport to prefix
        // messages with < or > to indicate direction.
        // We'll try to determine direction based on content as a fallback:
        if (message.startsWith("{") || message.startsWith("[")) {
            return "< " + message; // Assume incoming JSON
        } else if (message.startsWith("POST") || message.startsWith("GET")) {
            return "> " + message; // Assume outgoing request
        } else {
            // For other messages, don't add a prefix since we can't reliably determine direction
            return message;
        }
    }
    
    /**
     * Log the message at the appropriate level based on the MCP log level
     *
     * @param level   The MCP log level
     * @param message The message to log
     */
    private void logAtLevel(@NotNull McpLogLevel level, String message) {
        switch (level) {
            case DEBUG:
                log.debug(message);
                break;
            case INFO:
                log.info(message);
                break;
            case ERROR:
                log.error(message);
                break;
            default:
                log.info(message);
        }
    }
    
    /**
     * Publish the message to the application message bus
     *
     * @param message The message to publish
     */
    private void publishToBus(String message) {
        try {
            MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
            messageBus.syncPublisher(AppTopics.MCP_LOGGING_MSG)
                    .onMCPLoggingMessage(MCPMessage.builder()
                            .type(MCPType.LOG_MSG)
                            .content(message)
                            .build());
        } catch (Exception e) {
            log.error("Error publishing MCP log message to message bus", e);
        }
    }
}
