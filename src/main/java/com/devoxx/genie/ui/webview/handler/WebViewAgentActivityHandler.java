package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.model.agent.AgentMessage;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.service.agent.AgentLoggingMessage;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.ThemeDetector;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles displaying agent tool execution activity in the WebView.
 * Updates the "Thinking..." loading indicator with real-time tool call info
 * so users can see what the agent is doing.
 *
 * Note: All content inserted into the DOM is HTML-escaped via escapeHtml()
 * to prevent XSS. The HTML structure itself uses only hardcoded class names
 * and trusted static content. This follows the same pattern as WebViewMCPLogHandler.
 */
@Slf4j
public class WebViewAgentActivityHandler implements AgentLoggingMessage {

    private final WebViewJavaScriptExecutor jsExecutor;
    private volatile String activeMessageId;
    private volatile boolean deactivated = false;
    private final List<AgentMessage> agentLogs = new ArrayList<>();
    private boolean hasToolActivity;

    public WebViewAgentActivityHandler(WebViewJavaScriptExecutor jsExecutor) {
        this.jsExecutor = jsExecutor;
    }

    /**
     * Set the active message ID that will receive agent activity logs.
     * Resets the deactivated flag so the handler is ready for a new message.
     */
    public void setActiveMessageId(String messageId) {
        this.activeMessageId = messageId;
        this.deactivated = false;
        agentLogs.clear();
        hasToolActivity = false;
    }

    /**
     * Deactivates this handler so it ignores any further log messages.
     * Called during cancel/stop to prevent stale events from re-showing the indicator.
     */
    public void deactivate() {
        this.deactivated = true;
        this.activeMessageId = null;
        log.info("Agent activity handler deactivated");
    }

    @Override
    public void onAgentLoggingMessage(@NotNull AgentMessage message) {
        if (deactivated) {
            log.debug("Agent activity handler deactivated, ignoring message: {}", message.getType());
            return;
        }
        log.debug(">>> Agent message (type={}): {} - {}", message.getType(), message.getToolName(), message.getCallNumber());

        hasToolActivity = true;
        agentLogs.add(message);

        String formattedHtml = buildFormattedLogsHtml();
        updateThinkingIndicator(activeMessageId, formattedHtml);
    }

    /**
     * Builds formatted HTML for all stored agent log entries.
     * All dynamic content is HTML-escaped before insertion.
     */
    private @NotNull String buildFormattedLogsHtml() {
        StringBuilder html = new StringBuilder();

        html.append("<div class=\"agent-outer-container\">");
        html.append("<div class=\"agent-header\">Agent Activity</div>");

        for (AgentMessage entry : agentLogs) {
            String cssClass = getCssClass(entry.getType());
            String icon = getIcon(entry.getType());

            html.append("<div class=\"agent-log-entry ").append(cssClass).append("\">");
            html.append("<span class=\"agent-counter\">[").append(entry.getCallNumber())
                    .append("/").append(entry.getMaxCalls()).append("]</span> ");
            html.append(icon).append(" ");

            switch (entry.getType()) {
                case TOOL_REQUEST:
                    html.append("<strong>").append(escapeHtml(entry.getToolName())).append("</strong>");
                    if (entry.getArguments() != null) {
                        String args = truncate(entry.getArguments(), 500);
                        html.append("<div class=\"agent-args\">").append(escapeHtml(args)).append("</div>");
                    }
                    break;
                case TOOL_RESPONSE:
                    html.append("<strong>").append(escapeHtml(entry.getToolName())).append("</strong> completed");
                    if (entry.getResult() != null) {
                        String result = truncate(entry.getResult(), 500);
                        html.append("<div class=\"agent-result\">").append(escapeHtml(result)).append("</div>");
                    }
                    break;
                case TOOL_ERROR:
                    html.append("<strong>").append(escapeHtml(entry.getToolName())).append("</strong> failed");
                    if (entry.getResult() != null) {
                        html.append("<div class=\"agent-error\">").append(escapeHtml(entry.getResult())).append("</div>");
                    }
                    break;
                case LOOP_LIMIT:
                    html.append("<strong>Loop limit reached</strong> (").append(entry.getMaxCalls()).append(" calls)");
                    break;
                case APPROVAL_REQUESTED:
                    html.append("Waiting for approval: <strong>").append(escapeHtml(entry.getToolName())).append("</strong>");
                    break;
                case APPROVAL_GRANTED:
                    html.append("Approved: <strong>").append(escapeHtml(entry.getToolName())).append("</strong>");
                    break;
                case APPROVAL_DENIED:
                    html.append("Denied: <strong>").append(escapeHtml(entry.getToolName())).append("</strong>");
                    break;
                case INTERMEDIATE_RESPONSE:
                    html.append("LLM intermediate response");
                    break;
            }

            html.append("</div>\n");
        }

        html.append("</div>");
        return html.toString();
    }

    private String getCssClass(AgentType type) {
        return switch (type) {
            case TOOL_REQUEST -> "agent-request";
            case TOOL_RESPONSE -> "agent-response";
            case TOOL_ERROR -> "agent-error-entry";
            case LOOP_LIMIT -> "agent-limit";
            case APPROVAL_REQUESTED -> "agent-approval";
            case APPROVAL_GRANTED -> "agent-approved";
            case APPROVAL_DENIED -> "agent-denied";
            case INTERMEDIATE_RESPONSE -> "agent-intermediate";
        };
    }

    private String getIcon(AgentType type) {
        return switch (type) {
            case TOOL_REQUEST -> "&#9654;";        // play triangle
            case TOOL_RESPONSE -> "&#10004;";      // checkmark
            case TOOL_ERROR -> "&#10006;";         // X mark
            case LOOP_LIMIT -> "&#9888;";          // warning triangle
            case APPROVAL_REQUESTED -> "&#10067;"; // question mark
            case APPROVAL_GRANTED -> "&#10004;";
            case APPROVAL_DENIED -> "&#10006;";
            case INTERMEDIATE_RESPONSE -> "&#128172;"; // speech bubble
        };
    }

    /**
     * Updates the loading indicator in the WebView with agent activity content.
     * Uses the same target element strategy as WebViewMCPLogHandler:
     * - Streaming mode: targets loading-{messageId} inside assistant-message
     * - Non-streaming mode: targets mcp-{messageId} activity section
     */
    private void updateThinkingIndicator(String messageId, String content) {
        if (!jsExecutor.isLoaded() || messageId == null) {
            return;
        }

        boolean isStreaming = Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getStreamMode());

        String targetId = isStreaming
                ? "loading-" + jsExecutor.escapeJS(messageId)
                : "mcp-" + jsExecutor.escapeJS(messageId);

        String escapedContent = jsExecutor.escapeJS(content);
        String escapedMessageId = jsExecutor.escapeJS(messageId);
        boolean isDark = ThemeDetector.isDarkTheme();

        String js = buildAgentUpdateScript(targetId, escapedContent, escapedMessageId, isDark);
        jsExecutor.executeJavaScript(js);
    }

    /**
     * Builds the JavaScript to update agent activity content in the target element.
     * Content is pre-escaped (HTML-escaped then JS-escaped) before being embedded.
     * This follows the same DOM update pattern as WebViewMCPLogHandler.
     */
    private @NotNull String buildAgentUpdateScript(String targetId, String escapedContent,
                                                    String escapedMessageId, boolean isDark) {
        String bgColor = isDark ? "#2a2520" : "#fff8f0";
        String borderColor = "#FF5400";

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
                "    target.classList.add('agent-active');\n" +
                "    if (!document.getElementById('agent-activity-styles')) {\n" +
                "      const styleEl = document.createElement('style');\n" +
                "      styleEl.id = 'agent-activity-styles';\n" +
                "      styleEl.textContent = `\n" +
                "        .agent-outer-container {\n" +
                "          margin-top: 12px;\n" +
                "          max-height: 600px;\n" +
                "          overflow-y: auto;\n" +
                "          font-family: var(--font-family);\n" +
                "          font-size: 13px;\n" +
                "          padding: 8px;\n" +
                "          background-color: " + bgColor + ";\n" +
                "          border-radius: 6px;\n" +
                "          border-left: 3px solid " + borderColor + ";\n" +
                "          margin-bottom: 15px;\n" +
                "          clear: both;\n" +
                "          display: block;\n" +
                "        }\n" +
                "        .agent-header {\n" +
                "          font-size: 14px;\n" +
                "          font-weight: bold;\n" +
                "          color: #FF5400;\n" +
                "          margin-bottom: 10px;\n" +
                "        }\n" +
                "        .agent-log-entry {\n" +
                "          margin-bottom: 6px;\n" +
                "          padding: 4px 8px;\n" +
                "          font-size: 12px;\n" +
                "          line-height: 1.4;\n" +
                "        }\n" +
                "        .agent-counter {\n" +
                "          color: #757575;\n" +
                "          font-style: italic;\n" +
                "        }\n" +
                "        .agent-request { color: " + (isDark ? "#64B5F6" : "#1976D2") + "; }\n" +
                "        .agent-response { color: " + (isDark ? "#81C784" : "#388E3C") + "; }\n" +
                "        .agent-error-entry { color: " + (isDark ? "#FF8A80" : "#D32F2F") + "; }\n" +
                "        .agent-limit { color: " + (isDark ? "#FFB74D" : "#F57C00") + "; }\n" +
                "        .agent-approval { color: " + (isDark ? "#CE93D8" : "#7B1FA2") + "; }\n" +
                "        .agent-approved { color: " + (isDark ? "#81C784" : "#388E3C") + "; }\n" +
                "        .agent-denied { color: " + (isDark ? "#FF8A80" : "#D32F2F") + "; }\n" +
                "        .agent-intermediate { color: " + (isDark ? "#B0BEC5" : "#546E7A") + "; }\n" +
                "        .agent-args, .agent-result, .agent-error {\n" +
                "          font-family: monospace;\n" +
                "          font-size: 11px;\n" +
                "          padding: 4px 8px;\n" +
                "          margin-top: 2px;\n" +
                "          background-color: " + (isDark ? "#1e1e1e" : "#f5f5f5") + ";\n" +
                "          border-radius: 3px;\n" +
                "          white-space: pre-wrap;\n" +
                "          word-break: break-all;\n" +
                "          max-height: 150px;\n" +
                "          overflow-y: auto;\n" +
                "        }\n" +
                "        .loading-indicator.agent-active { display: block; }\n" +
                "      `;\n" +
                "      document.head.appendChild(styleEl);\n" +
                "    }\n" +
                "    window.scrollTo(0, document.body.scrollHeight);\n" +
                "  }\n" +
                "} catch (error) {\n" +
                "  console.error('Error updating agent activity:', error);\n" +
                "}\n";
    }

    /**
     * Escapes HTML special characters to prevent XSS when inserting
     * dynamic content into the DOM.
     */
    private @NotNull String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private @NotNull String truncate(@NotNull String text, int maxLength) {
        if (text.length() > maxLength) {
            return text.substring(0, maxLength) + "...";
        }
        return text;
    }
}
