package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
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
        // Determine message direction based on content analysis
        MCPType messageType = classifyMessageType(message);
        
        // Apply formatting based on message type
        return switch (messageType) {
            case AI_MSG -> "< " + message; // AI response (incoming)
            case TOOL_MSG -> "> " + message; // Tool request (outgoing)
            default -> message;      // General log
        };
    }
    
    /**
     * Classify the message type based on content analysis
     * 
     * @param message The message content
     * @return Appropriate MCPType
     */
    private MCPType classifyMessageType(@NotNull String message) {
        // Use content pattern matching to determine message type
        if (message.startsWith("{") || message.startsWith("[")) {
            // JSON content is typically a response
            if (message.contains("\"content\":") || message.contains("\"response\":")) {
                return MCPType.AI_MSG;
            } else if (message.contains("\"function\":") || message.contains("\"name\":")) {
                return MCPType.TOOL_MSG;
            }
            return MCPType.AI_MSG; // Default for JSON
        } else if (message.startsWith("POST") || message.startsWith("GET") || message.contains("function(")) {
            // HTTP requests or function definitions are tool related
            return MCPType.TOOL_MSG;
        } else {
            // Other messages are treated as general logs
            return MCPType.LOG_MSG;
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
            // Strip direction markers for cleaner display
            String cleanMessage = message;
            if (message.startsWith("< ") || message.startsWith("> ")) {
                cleanMessage = message.substring(2);
            }

            // Determine message type based on content
            MCPType messageType = MCPType.LOG_MSG;
            if (message.startsWith("< ")) {
                messageType = MCPType.AI_MSG;
            } else if (message.startsWith("> ")) {
                messageType = MCPType.TOOL_MSG;
            }

            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(AppTopics.ACTIVITY_LOG_MSG)
                    .onActivityMessage(ActivityMessage.builder()
                            .source(ActivitySource.MCP)
                            .mcpType(messageType)
                            .content(cleanMessage)
                            .build());
        } catch (Exception e) {
            log.error("Error publishing MCP log message to message bus", e);
        }
    }
}
