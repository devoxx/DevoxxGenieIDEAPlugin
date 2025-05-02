package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.service.mcp.MCPLoggingMessage;
import com.devoxx.genie.ui.util.CodeLanguageUtil;
import com.devoxx.genie.ui.util.ThemeDetector;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles MCP logging in the WebView.
 * This class is responsible for displaying MCP logs in the conversation.
 */
@Slf4j
public class WebViewMCPLogHandler implements MCPLoggingMessage {

    private final WebViewJavaScriptExecutor jsExecutor;
    private String activeMessageId;
    private final List<String> mcpLogs = new ArrayList<>();
    
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
        mcpLogs.clear();
    }
    
    /**
     * Implements the MCPLoggingMessage interface to receive MCP log messages.
     * Updates the thinking indicator in the UI with the log content.
     *
     * @param message The MCP message received
     */
    @Override
    public void onMCPLoggingMessage(@NotNull MCPMessage message) {
        log.info(">>> MCP message: {}", message.getContent());

        // Always store the log message regardless of debug setting
        String logContent = message.getContent();
        mcpLogs.add(logContent);
        
        // Format the logs for display with modern MCP formatting
        StringBuilder formattedLogs = new StringBuilder();
        
        // Create a container for all MCP logs
        formattedLogs.append("<div class=\"mcp-outer-container\">");
        formattedLogs.append("<div class=\"mcp-header\">MCP Activity</div>");
        
        // Process all logs
        boolean hasDisplayableLogs = false;

        // Initialize markdown parser and renderer
        Parser markdownParser = Parser.builder().build();
        HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

        for (String log : mcpLogs) {
            // Determine message type based on content
            String cssClass = "mcp-regular";

            // Strip direction markers and set appropriate CSS class
            if (message.getType() == MCPType.AI_MSG) {
                cssClass = "mcp-ai-message";
                hasDisplayableLogs = true;
            } else if (message.getType() == MCPType.TOOL_MSG) {
                cssClass = "mcp-tool-message";
                hasDisplayableLogs = true;
            }
            
            // Add the log entry with appropriate formatting
            formattedLogs.append("<div class=\"mcp-log-entry\">");
            formattedLogs.append("<div class=\"").append(cssClass).append("\">");

            // Parse and render markdown content
            Node document = markdownParser.parse(log);

            // Process the markdown nodes
            Node node = document.getFirstChild();
            while (node != null) {
                if (node instanceof FencedCodeBlock fencedCodeBlock) {
                    String code = fencedCodeBlock.getLiteral();
                    String language = fencedCodeBlock.getInfo();
                    formattedLogs.append("<pre><code class=\"language-")
                            .append(CodeLanguageUtil.mapLanguageToPrism(language))
                            .append("\">")
                            .append(jsExecutor.escapeHtml(code))
                            .append("</code></pre>\n");
                } else if (node instanceof IndentedCodeBlock indentedCodeBlock) {
                    String code = indentedCodeBlock.getLiteral();
                    formattedLogs.append("<pre><code class=\"language-plaintext\">")
                            .append(jsExecutor.escapeHtml(code))
                            .append("</code></pre>\n");
                } else {
                    formattedLogs.append(htmlRenderer.render(node));
                }
                node = node.getNext();
            }

            formattedLogs.append("</div>");
            formattedLogs.append("</div>\n");
        }
        
        // Close the container
        formattedLogs.append("</div>");
        
        // If no displayable logs, show a message or hide the indicator
        if (!hasDisplayableLogs) {
            boolean hasMcpActivity = mcpLogs.stream()
                    .anyMatch(log -> log.contains("tool") || log.contains("function"));
                    
            if (hasMcpActivity) {
                // Show the "in progress" message only if we have actual MCP activity
                formattedLogs = new StringBuilder();
                formattedLogs.append("<div class=\"mcp-outer-container\">");
                formattedLogs.append("<div class=\"mcp-header\">MCP Activity</div>");
                formattedLogs.append("<div class=\"mcp-log-entry\">");
                formattedLogs.append("<span class=\"mcp-counter\">MCP activity in progress (no action messages yet)</span>");
                formattedLogs.append("</div>\n");
                formattedLogs.append("</div>");
            } else {
                // Even if there's no actual MCP activity, we should still show something
                // for debugging purposes
                formattedLogs = new StringBuilder();
                formattedLogs.append("<div class=\"mcp-outer-container\">");
                formattedLogs.append("<div class=\"mcp-header\">MCP Activity</div>");
                formattedLogs.append("<div class=\"mcp-log-entry\">");
                formattedLogs.append("<span class=\"mcp-counter\">No MCP activity detected</span>");
                formattedLogs.append("</div>\n");
                formattedLogs.append("</div>");
                // Don't mark as completed or return early - let the logs be displayed
            }
        }

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
                    "  const indicator = document.getElementById('loading-" + jsExecutor.escapeJS(messageId) + "');\n" +
                    "  if (indicator) {\n" +
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
                    "          font-family: var(--font-family);\n" +
                    "          font-size: 13px;\n" +
                    "          padding: 8px;\n" +
                    "          background-color: " + (ThemeDetector.isDarkTheme() ? "#2d2d2d" : "#f8f8f8") + ";\n" +
                    "          border-radius: 6px;\n" +
                    "          border: none;\n" +
                    "          margin-bottom: 15px;\n" +
                    "          clear: both;\n" +
                    "          display: block;\n" +
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
                    "        }\n" +
                    "        .mcp-log-entry pre { \n" +
                    "          margin: 8px 0;\n" +
                    "          padding: 8px;\n" +
                    "          background-color: " + (ThemeDetector.isDarkTheme() ? "#1e1e1e" : "#f0f0f0") + ";\n" +
                    "          border-radius: 4px;\n" +
                    "          overflow-x: auto;\n" +
                    "        }\n" +
                    "        .mcp-log-entry code { \n" +
                    "          font-family: monospace;\n" +
                    "          font-size: 12px;\n" +
                    "        }\n" +
                    "        .mcp-log-entry p { \n" +
                    "          margin: 6px 0;\n" +
                    "        }\n" +
                    "        .mcp-regular { color: #FF9800; }\n" +
                    "        .mcp-counter { color: #757575; font-style: italic; }\n" +
                    "        .mcp-ai-message { color: #4CAF50; }\n" +
                    "        .mcp-tool-message { color: #2196F3; }\n" +
                    "        .mcp-completed { border-top: 1px solid #FF5400; padding-top: 8px; margin-top: 15px; }\n" +
                    "        .loading-indicator { display: block !important; }\n" +
                    "      `;\n" +
                    "      document.head.appendChild(styleEl);\n" +
                    "    }\n" +
                    "    window.scrollTo(0, document.body.scrollHeight);\n" +
                    "    if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }\n" +
                    "  } else {\n" +
                    "    const messagePair = document.getElementById('" + jsExecutor.escapeJS(messageId) + "');\n" +
                    "    if (messagePair) {\n" +
                    "      const loadingIndicator = messagePair.querySelector('.loading-indicator');\n" +
                    "      if (loadingIndicator) {\n" +
                    "        loadingIndicator.innerHTML = `" + jsExecutor.escapeJS(content) + "`;\n" +
                    "        loadingIndicator.style.display = 'block';\n" +
                    "        if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "} catch (error) {\n" +
                    "}\n";
        
        jsExecutor.executeJavaScript(js);
        log.debug("updateThinkingIndicator executed");
    }
}
