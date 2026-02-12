package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.model.agent.AgentMessage;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.service.agent.AgentLoggingMessage;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.ThemeDetector;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles displaying agent tool execution activity in the WebView.
 * Updates the "Thinking..." loading indicator with real-time tool call info
 * so users can see what the agent is doing.
 * Note: All content inserted into the DOM is HTML-escaped via escapeHtml()
 * to prevent XSS. The HTML structure itself uses only hardcoded class names
 * and trusted static content. This follows the same pattern as WebViewMCPLogHandler.
 */
@Slf4j
public class WebViewAgentActivityHandler implements AgentLoggingMessage {

    private static final String ICON_PLAY = "&#9654;";
    private static final String ICON_CHECKMARK = "&#10004;";
    private static final String ICON_X_MARK = "&#10006;";
    private static final String ICON_WARNING = "&#9888;";
    private static final String ICON_QUESTION = "&#10067;";
    private static final String ICON_SPEECH = "&#128172;";

    private static final String COLOR_GREEN_DARK = "#81C784";
    private static final String COLOR_GREEN_LIGHT = "#388E3C";
    private static final String COLOR_RED_DARK = "#FF8A80";
    private static final String COLOR_RED_LIGHT = "#D32F2F";
    private static final String COLOR_BLUE_DARK = "#64B5F6";
    private static final String COLOR_BLUE_LIGHT = "#1976D2";

    private static final String STRONG_OPEN = "<strong>";
    private static final String STRONG_CLOSE = "</strong>";

    private final WebViewJavaScriptExecutor jsExecutor;
    private volatile String activeMessageId;
    private volatile boolean deactivated = false;
    private final List<AgentMessage> agentLogs = new CopyOnWriteArrayList<>();

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
            html.append("<div class=\"agent-log-entry ").append(getCssClass(entry.getType())).append("\">");
            html.append("<span class=\"agent-counter\">[").append(entry.getCallNumber())
                    .append("/").append(entry.getMaxCalls()).append("]</span> ");
            if (entry.getSubAgentId() != null) {
                html.append("<span class=\"agent-subagent-id\">[").append(escapeHtml(entry.getSubAgentId())).append("]</span> ");
            }
            html.append(getIcon(entry.getType())).append(" ");
            appendEntryContent(html, entry);
            html.append("</div>\n");
        }

        html.append("</div>");
        return html.toString();
    }

    private void appendEntryContent(@NotNull StringBuilder html, @NotNull AgentMessage entry) {
        switch (entry.getType()) {
            case TOOL_REQUEST:
                appendBoldToolName(html, "", entry.getToolName(), "");
                appendDetail(html, entry.getArguments(), "agent-args");
                break;
            case TOOL_RESPONSE:
                appendBoldToolName(html, "", entry.getToolName(), " completed");
                appendDetail(html, entry.getResult(), "agent-result");
                break;
            case TOOL_ERROR:
                appendBoldToolName(html, "", entry.getToolName(), " failed");
                appendDetail(html, entry.getResult(), "agent-error");
                break;
            case LOOP_LIMIT:
                html.append(STRONG_OPEN).append("Loop limit reached").append(STRONG_CLOSE)
                        .append(" (").append(entry.getMaxCalls()).append(" calls)");
                break;
            case APPROVAL_REQUESTED:
                appendBoldToolName(html, "Waiting for approval: ", entry.getToolName(), "");
                break;
            case APPROVAL_GRANTED:
                appendBoldToolName(html, "Approved: ", entry.getToolName(), "");
                break;
            case APPROVAL_DENIED:
                appendBoldToolName(html, "Denied: ", entry.getToolName(), "");
                break;
            case INTERMEDIATE_RESPONSE:
                html.append("LLM intermediate response");
                break;
            case SUB_AGENT_STARTED:
                appendBoldToolName(html, "Sub-agent started: ", entry.getToolName(), "");
                break;
            case SUB_AGENT_COMPLETED:
                appendBoldToolName(html, "Sub-agent completed: ", entry.getToolName(), "");
                appendDetail(html, entry.getResult(), "agent-result");
                break;
            case SUB_AGENT_ERROR:
                appendBoldToolName(html, "Sub-agent failed: ", entry.getToolName(), "");
                appendDetail(html, entry.getResult(), "agent-error");
                break;
        }
    }

    private void appendBoldToolName(@NotNull StringBuilder html, String prefix, String toolName, String suffix) {
        html.append(prefix).append(STRONG_OPEN).append(escapeHtml(toolName)).append(STRONG_CLOSE).append(suffix);
    }

    private void appendDetail(@NotNull StringBuilder html, String text, String cssClass) {
        if (text != null) {
            html.append("<div class=\"").append(cssClass).append("\">")
                    .append(escapeHtml(truncate(text))).append("</div>");
        }
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
            case SUB_AGENT_STARTED -> "agent-subagent-started";
            case SUB_AGENT_COMPLETED -> "agent-subagent-completed";
            case SUB_AGENT_ERROR -> "agent-subagent-error";
        };
    }

    private String getIcon(AgentType type) {
        return switch (type) {
            case TOOL_REQUEST, SUB_AGENT_STARTED -> ICON_PLAY;
            case TOOL_RESPONSE, APPROVAL_GRANTED, SUB_AGENT_COMPLETED -> ICON_CHECKMARK;
            case TOOL_ERROR, APPROVAL_DENIED, SUB_AGENT_ERROR -> ICON_X_MARK;
            case LOOP_LIMIT -> ICON_WARNING;
            case APPROVAL_REQUESTED -> ICON_QUESTION;
            case INTERMEDIATE_RESPONSE -> ICON_SPEECH;
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
                "        .agent-subagent-id {\n" +
                "          color: #FF5400;\n" +
                "          font-weight: bold;\n" +
                "          font-size: 11px;\n" +
                "        }\n" +
                "        .agent-request, .agent-subagent-started { color: " + (isDark ? COLOR_BLUE_DARK : COLOR_BLUE_LIGHT) + "; }\n" +
                "        .agent-response, .agent-approved, .agent-subagent-completed { color: " + (isDark ? COLOR_GREEN_DARK : COLOR_GREEN_LIGHT) + "; }\n" +
                "        .agent-error-entry, .agent-denied, .agent-subagent-error { color: " + (isDark ? COLOR_RED_DARK : COLOR_RED_LIGHT) + "; }\n" +
                "        .agent-limit { color: " + (isDark ? "#FFB74D" : "#F57C00") + "; }\n" +
                "        .agent-approval { color: " + (isDark ? "#CE93D8" : "#7B1FA2") + "; }\n" +
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
                "    var agentContainer = target.querySelector('.agent-outer-container');\n" +
                "    if (agentContainer) { agentContainer.scrollTop = agentContainer.scrollHeight; }\n" +
                "    setTimeout(function() { window.scrollTo(0, document.body.scrollHeight); }, 0);\n" +
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

    private @NotNull String truncate(@NotNull String text) {
        if (text.length() > 500) {
            return text.substring(0, 500) + "...";
        }
        return text;
    }
}
