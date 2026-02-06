package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.service.mcp.MCPLoggingMessage;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.CodeLanguageUtil;
import com.devoxx.genie.ui.util.ThemeDetector;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

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
    private final List<MCPMessage> mcpLogs = new ArrayList<>();
    private boolean hasToolActivity;

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
        hasToolActivity = false;
    }

    /**
     * Implements the MCPLoggingMessage interface to receive MCP log messages.
     * Only TOOL_MSG and LOG_MSG are displayed in MCP Activity.
     * AI_MSG is skipped because it contains full AI responses that are already
     * rendered in the assistant-message area.
     *
     * @param message The MCP message received
     */
    @Override
    public void onMCPLoggingMessage(@NotNull MCPMessage message) {
        log.info(">>> MCP message (type={}): {}", message.getType(), message.getContent());

        // Skip AI_MSG — these are full AI responses from MCPListenerService (redundant
        // with assistant-message) or raw JSON-RPC server responses from MCPLogMessageHandler
        if (message.getType() == MCPType.AI_MSG) {
            log.debug("Skipping AI_MSG in MCP Activity display");
            return;
        }

        // Track whether actual MCP tool interaction has occurred
        if (message.getType() == MCPType.TOOL_MSG) {
            hasToolActivity = true;
        }

        mcpLogs.add(message);

        // Only render MCP activity to the UI when actual tool interaction is detected
        if (!hasToolActivity) {
            log.debug("No tool activity yet, skipping MCP UI update");
            return;
        }

        // Format the logs for display
        String formattedHtml = buildFormattedLogsHtml();

        // Update the UI with the formatted logs
        updateThinkingIndicator(activeMessageId, formattedHtml);
    }

    /**
     * Builds formatted HTML for all stored MCP log entries.
     */
    private @NotNull String buildFormattedLogsHtml() {
        StringBuilder formattedLogs = new StringBuilder();

        formattedLogs.append("<div class=\"mcp-outer-container\">");
        formattedLogs.append("<div class=\"mcp-header\">MCP Activity</div>");

        Parser markdownParser = Parser.builder().build();
        HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

        for (MCPMessage logEntry : mcpLogs) {
            // Style each entry based on its own type
            String cssClass = logEntry.getType() == MCPType.TOOL_MSG
                    ? "mcp-tool-message"
                    : "mcp-regular";

            formattedLogs.append("<div class=\"mcp-log-entry\">");
            formattedLogs.append("<div class=\"").append(cssClass).append("\">");

            // Parse and render markdown content
            Node document = markdownParser.parse(logEntry.getContent());
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

        formattedLogs.append("</div>");
        return formattedLogs.toString();
    }

    /**
     * Updates the MCP activity display with formatted MCP logs.
     * Routes to different target elements based on streaming mode:
     * - Non-streaming: targets mcp-{messageId} (separate section between user and assistant)
     * - Streaming: targets loading-{messageId} (inside assistant-message)
     *
     * @param messageId The ID of the message to update
     * @param content The HTML content to show
     */
    private void updateThinkingIndicator(String messageId, String content) {
        log.debug("updateThinkingIndicator with {} and {}", messageId, content);

        if (!jsExecutor.isLoaded() || messageId == null) {
            log.debug("updateThinkingIndicator not loaded, skipping update");
            return;
        }

        log.debug("Updating indicator for message ID: {}", messageId);

        boolean isStreaming = Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getStreamMode());

        // Determine the target element ID based on streaming mode
        String targetId = isStreaming
                ? "loading-" + jsExecutor.escapeJS(messageId)
                : "mcp-" + jsExecutor.escapeJS(messageId);

        String escapedContent = jsExecutor.escapeJS(content);
        String escapedMessageId = jsExecutor.escapeJS(messageId);
        boolean isDark = ThemeDetector.isDarkTheme();

        String js = buildMcpUpdateScript(targetId, escapedContent, escapedMessageId, isDark);

        jsExecutor.executeJavaScript(js);
        log.debug("updateThinkingIndicator executed");
    }

    /**
     * Builds the JavaScript to update MCP log content in the target element.
     * Content is pre-escaped by the caller before being embedded in the template literal.
     */
    private @NotNull String buildMcpUpdateScript(String targetId, String escapedContent,
                                                  String escapedMessageId, boolean isDark) {
        String bgColor = isDark ? "#2d2d2d" : "#f8f8f8";
        String preBgColor = isDark ? "#1e1e1e" : "#f0f0f0";

        return "try {\n" +
                "  var target = document.getElementById('" + targetId + "');\n" +
                "  if (!target) {\n" +
                "    const messagePair = document.getElementById('" + escapedMessageId + "');\n" +
                "    if (messagePair) {\n" +
                "      target = messagePair.querySelector('.mcp-activity-section') || messagePair.querySelector('.loading-indicator');\n" +
                "    }\n" +
                "  }\n" +
                "  if (target) {\n" +
                "    target.innerHTML = `" + escapedContent + "`;\n" +
                "    target.style.display = 'block';\n" +
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
                "          background-color: " + bgColor + ";\n" +
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
                "          background-color: " + preBgColor + ";\n" +
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
                "        .mcp-completed { padding-top: 8px; }\n" +
                "        .loading-indicator { display: block !important; }\n" +
                "      `;\n" +
                "      document.head.appendChild(styleEl);\n" +
                "    }\n" +
                "    window.scrollTo(0, document.body.scrollHeight);\n" +
                "    if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }\n" +
                "  }\n" +
                "} catch (error) {\n" +
                "}\n";
    }
}
