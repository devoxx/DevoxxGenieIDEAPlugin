package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.service.mcp.MCPLoggingMessage;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.util.ThemeDetector;
import com.intellij.openapi.diagnostic.Logger;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles MCP logging in the WebView.
 * This class is responsible for displaying MCP logs in the conversation.
 */
@Slf4j
public class WebViewMCPLogHandler implements MCPLoggingMessage {

    private final WebViewJavaScriptExecutor jsExecutor;
    private String activeMessageId; // Track the active message that is currently thinking
    private final List<String> mcpLogs = new ArrayList<>(); // Store MCP logs for the current thinking process
    
    public WebViewMCPLogHandler(WebViewJavaScriptExecutor jsExecutor) {
        this.jsExecutor = jsExecutor;
    }
    
    /**
     * Set the active message ID that will receive MCP logs.
     * 
     * @param messageId The ID of the active message
     */
    public void setActiveMessageId(String messageId) {
        this.activeMessageId = messageId;
        // Clear previous MCP logs when starting a new conversation
        mcpLogs.clear();
    }
    
    /**
     * Implements the MCPLoggingMessage interface to receive MCP log messages.
     * Updates the thinking indicator in the UI with the log content.
     *
     * @param message The MCP message received
     */
    @Override
    public void onMCPLoggingMessage(MCPMessage message) {
        log.info(">>> MCP message: {}", message.getContent());

        // Always store the log message regardless of debug setting
        String logContent = message.getContent();
        mcpLogs.add(logContent);
        
        // Format the logs for display with individual containers
        StringBuilder formattedLogs = new StringBuilder();
        
        // Filter to get only the desired log messages
        List<String> filteredLogs = mcpLogs.stream()
                .filter(log -> !log.startsWith("<") && !log.startsWith(">") && !log.startsWith("{"))
                .toList();
        
        // Create container for all logs
//        formattedLogs.append("<div class=\"mcp-outer-container\">\n");
//        formattedLogs.append("<div class=\"mcp-header\">MCP Actions</div>\n");
        
        // Add filtered logs to display
        if (!filteredLogs.isEmpty()) {
            // Create individual entry for each log
            for (String log : filteredLogs) {
                formattedLogs.append("<div class=\"mcp-log-entry\">");
                formattedLogs.append("<span class=\"mcp-regular\">");
                formattedLogs.append(jsExecutor.escapeHtml(log));
                formattedLogs.append("</span>");
                formattedLogs.append("</div>\n");
            }
        } else {
            // Show a message when no logs are available
            formattedLogs.append("<div class=\"mcp-log-entry\">");
            formattedLogs.append("<span class=\"mcp-counter\">MCP activity in progress (no action messages yet)</span>");
            formattedLogs.append("</div>\n");
        }
        
//        formattedLogs.append("</div>");
        
        // Update the UI with the formatted logs
        updateThinkingIndicator(activeMessageId, formattedLogs.toString());
    }
    
    /**
     * Updates the thinking indicator with formatted MCP logs
     *
     * @param messageId The ID of the message to update
     * @param content The HTML content to show in the thinking indicator
     */
    private void updateThinkingIndicator(String messageId, String content) {
        log.debug("updateThinkingIndicator with {} and {}", messageId, content);

        if (!jsExecutor.isLoaded() || messageId == null) {
            log.debug("updateThinkingIndicator not loaded, skipping update");
            return;
        }
        
        // JavaScript to update the thinking indicator - log current messageId to help debug
        log.debug("Updating indicator for message ID: {}", messageId);
        
        String js = "try {\n" +
                    "  console.log('Looking for loading indicator with ID: loading-" + messageId + "');\n" +
                    "  const indicator = document.getElementById('loading-" + jsExecutor.escapeJS(messageId) + "');\n" +
                    "  if (indicator) {\n" +
                    "    console.log('Found indicator, updating content');\n" +
                    "    indicator.innerHTML = `" + jsExecutor.escapeJS(content) + "`;\n" +
                    "    indicator.style.display = 'block';\n" +
                    "    if (!document.getElementById('mcp-logs-styles')) {\n" +
                    "      const styleEl = document.createElement('style');\n" +
                    "      styleEl.id = 'mcp-logs-styles';\n" +
                    "      styleEl.textContent = `\n" +
                    "        .mcp-outer-container { \n" +
                    "          margin-top: 12px;\n" +
                    "          max-height: 300px;\n" +
                    "          overflow-y: auto;\n" +
                    "          font-family: monospace;\n" +
                    "          font-size: 13px;\n" +
                    "          padding: 8px;\n" +
                    "          background-color: " + (ThemeDetector.isDarkTheme() ? "#2d2d2d" : "#f8f8f8") + ";\n" +
                    "          border-radius: 6px;\n" +
                    "          border: none;\n" +
                    "        }\n" +
                    "        .mcp-header { \n" +
                    "          font-size: 14px;\n" +
                    "          font-weight: bold;\n" +
                    "          color: #FF5400;\n" +
                    "          margin-bottom: 10px;\n" +
                    "        }\n" +
                    "        .mcp-log-entry { \n" +
                    "          margin-bottom: 8px;\n" +
                    "          padding: 8px;\n" +
//                    "          background-color: " + (ThemeDetector.isDarkTheme() ? "#1e1e1e" : "#ffffff") + ";\n" +
//                    "          border-radius: 4px;\n" +
//                    "          border-left: 4px solid #2196F3;\n" +
                    "        }\n" +
                    "        .mcp-regular { color: #FF9800; font-weight: bold; }\n" +
                    "        .mcp-counter { color: #757575; font-style: italic; }\n" +
                    "      `;\n" +
                    "      document.head.appendChild(styleEl);\n" +
                    "    }\n" +
                    "    window.scrollTo(0, document.body.scrollHeight);\n" +
                    "  } else {\n" +
                    "    console.error('Thinking indicator not found for message: " + jsExecutor.escapeJS(messageId) + "');\n" +
                    "    // Fall back to trying to find by class if not found by ID\n" +
                    "    console.log('Trying to find indicator by class instead');\n" +
                    "    const messagePair = document.getElementById('" + jsExecutor.escapeJS(messageId) + "');\n" +
                    "    if (messagePair) {\n" +
                    "      const loadingIndicator = messagePair.querySelector('.loading-indicator');\n" +
                    "      if (loadingIndicator) {\n" +
                    "        console.log('Found indicator by class, updating content');\n" +
                    "        loadingIndicator.innerHTML = `" + jsExecutor.escapeJS(content) + "`;\n" +
                    "        loadingIndicator.style.display = 'block';\n" +
                    "      } else {\n" +
                    "        console.error('No loading indicator found even by class in message: " + jsExecutor.escapeJS(messageId) + "');\n" +
                    "      }\n" +
                    "    } else {\n" +
                    "      console.error('Message pair not found: " + jsExecutor.escapeJS(messageId) + "');\n" +
                    "    }\n" +
                    "  }\n" +
                    "} catch (error) {\n" +
                    "  console.error('Error updating thinking indicator:', error);\n" +
                    "}\n";
        
        jsExecutor.executeJavaScript(js);
        log.debug("updateThinkingIndicator executed");
    }
}
