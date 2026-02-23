package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.service.activity.ActivityLoggingMessage;
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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Unified handler for both MCP and Agent activity display in the WebView.
 * Replaces the separate WebViewMCPLogHandler and WebViewAgentActivityHandler.
 *
 * Note: All content inserted into the DOM is HTML-escaped via escapeHtml()
 * to prevent XSS. The HTML structure itself uses only hardcoded class names
 * and trusted static content. This follows the same pattern used by the
 * previous separate handlers.
 */
@Slf4j
public class WebViewActivityHandler implements ActivityLoggingMessage {

    // Agent icon constants
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
    private volatile long generation = 0;

    // MCP accumulators
    private final List<MCPMessage> mcpLogs = new ArrayList<>();
    private boolean hasToolActivity;

    // Agent accumulators
    private final List<ActivityMessage> agentLogs = new CopyOnWriteArrayList<>();
    private final List<String> intermediateTexts = new CopyOnWriteArrayList<>();

    public WebViewActivityHandler(WebViewJavaScriptExecutor jsExecutor) {
        this.jsExecutor = jsExecutor;
    }

    /**
     * Set the active message ID that will receive activity logs.
     * Resets the deactivated flag so the handler is ready for a new message.
     */
    public void setActiveMessageId(String messageId) {
        this.generation++;
        this.activeMessageId = messageId;
        this.deactivated = false;
        mcpLogs.clear();
        hasToolActivity = false;
        agentLogs.clear();
        intermediateTexts.clear();
        log.info("Activity handler activated: messageId={}, generation={}", messageId, generation);
    }

    /**
     * Deactivates this handler so it ignores any further log messages.
     * Called during cancel/stop to prevent stale events from re-showing the indicator.
     */
    public void deactivate() {
        this.deactivated = true;
        this.activeMessageId = null;
        log.info("Activity handler deactivated");
    }

    @Override
    public void onActivityMessage(@NotNull ActivityMessage message) {
        if (deactivated) {
            log.info("Activity handler deactivated, ignoring {} message", message.getSource());
            return;
        }

        if (message.getSource() == ActivitySource.MCP) {
            handleMCPMessage(message);
        } else {
            handleAgentMessage(message);
        }
    }

    // ===== MCP message handling (ported from WebViewMCPLogHandler) =====

    private void handleMCPMessage(@NotNull ActivityMessage message) {
        final long capturedGeneration = this.generation;

        log.info(">>> MCP message (type={}): {}", message.getMcpType(), message.getContent());

        // Skip AI_MSG — these are full AI responses (redundant with assistant-message)
        if (message.getMcpType() == MCPType.AI_MSG) {
            log.debug("Skipping AI_MSG in MCP Activity display");
            return;
        }

        // Track whether actual MCP tool interaction has occurred
        if (message.getMcpType() == MCPType.TOOL_MSG) {
            hasToolActivity = true;
        }

        if (this.generation != capturedGeneration) return;
        mcpLogs.add(MCPMessage.builder()
                .type(message.getMcpType())
                .content(message.getContent())
                .projectLocationHash(message.getProjectLocationHash())
                .build());

        // Only render MCP activity to the UI when actual tool interaction is detected
        if (!hasToolActivity) {
            log.debug("No tool activity yet, skipping MCP UI update");
            return;
        }

        if (this.generation != capturedGeneration) return;
        String formattedHtml = buildMcpFormattedLogsHtml();

        updateMcpThinkingIndicator(activeMessageId, formattedHtml);
    }

    /**
     * Builds formatted HTML for all stored MCP log entries.
     * All dynamic content is HTML-escaped via escapeHtml() before insertion.
     */
    private @NotNull String buildMcpFormattedLogsHtml() {
        StringBuilder formattedLogs = new StringBuilder();

        formattedLogs.append("<div class=\"mcp-outer-container\">");
        formattedLogs.append("<div class=\"mcp-header\">MCP Activity</div>");

        Parser markdownParser = Parser.builder().build();
        HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

        for (MCPMessage logEntry : mcpLogs) {
            String cssClass = logEntry.getType() == MCPType.TOOL_MSG
                    ? "mcp-tool-message"
                    : "mcp-regular";

            formattedLogs.append("<div class=\"mcp-log-entry\">");
            formattedLogs.append("<div class=\"").append(cssClass).append("\">");

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

    private void updateMcpThinkingIndicator(String messageId, String content) {
        log.debug("updateMcpThinkingIndicator with {} and {}", messageId, content);

        if (!jsExecutor.isLoaded() || messageId == null) {
            log.debug("updateMcpThinkingIndicator not loaded, skipping update");
            return;
        }

        boolean isStreaming = Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getStreamMode());

        String targetId = isStreaming
                ? "loading-" + jsExecutor.escapeJS(messageId)
                : "mcp-" + jsExecutor.escapeJS(messageId);

        String escapedContent = jsExecutor.escapeJS(content);
        String escapedMessageId = jsExecutor.escapeJS(messageId);
        boolean isDark = ThemeDetector.isDarkTheme();

        String js = buildMcpUpdateScript(targetId, escapedContent, escapedMessageId, isDark);

        jsExecutor.executeJavaScript(js);
        log.debug("updateMcpThinkingIndicator executed");
    }

    /**
     * Builds the JavaScript to update MCP log content in the target element.
     * Content is pre-escaped (HTML-escaped then JS-escaped) by the caller before being
     * embedded in the template literal, preventing XSS.
     */
    @SuppressWarnings("java:S6035") // innerHTML usage is safe — all dynamic content is pre-escaped
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
                // SECURITY: escapedContent is pre-escaped (HTML entities + JS string escaping)
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
                "    setTimeout(function() { window.scrollTo(0, document.body.scrollHeight); }, 0);\n" +
                "    if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }\n" +
                "  }\n" +
                "} catch (error) {\n" +
                "}\n";
    }

    // ===== Agent message handling (ported from WebViewAgentActivityHandler) =====

    private void handleAgentMessage(@NotNull ActivityMessage message) {
        final long capturedGeneration = this.generation;

        log.info(">>> Agent activity (type={}, messageId={}, deactivated={}): {} - {}",
                message.getAgentType(), activeMessageId, deactivated, message.getToolName(), message.getCallNumber());

        boolean showToolActivity = Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getShowToolActivityInChat());

        // Intermediate LLM responses are always shown in the chat output
        if (message.getAgentType() == AgentType.INTERMEDIATE_RESPONSE) {
            if (this.generation != capturedGeneration) return;
            String text = message.getResult() != null ? message.getResult() : "";
            intermediateTexts.add(text);
            log.info("Intermediate response #{} for messageId={}, showToolActivity={}, text length={}",
                    intermediateTexts.size(), activeMessageId, showToolActivity, text.length());
            if (showToolActivity) {
                agentLogs.add(message);
                if (this.generation != capturedGeneration) return;
                updateAgentThinkingIndicator(activeMessageId, buildAgentFormattedLogsHtml());
            } else {
                if (this.generation != capturedGeneration) return;
                appendIntermediateTextBelowThinking(activeMessageId);
            }
            return;
        }

        // Tool calls: only show in chat when "Show tool activity in chat" is enabled
        if (!showToolActivity) {
            return;
        }

        if (this.generation != capturedGeneration) return;
        agentLogs.add(message);
        if (this.generation != capturedGeneration) return;
        String formattedHtml = buildAgentFormattedLogsHtml();
        updateAgentThinkingIndicator(activeMessageId, formattedHtml);
    }

    /**
     * Appends intermediate LLM text below the "Thinking..." loading indicator.
     * All user-controlled text is HTML-escaped via the HtmlRenderer's escapeHtml(true) setting.
     */
    @SuppressWarnings("java:S6035") // innerHTML usage is safe — content is escaped by HtmlRenderer
    private void appendIntermediateTextBelowThinking(String messageId) {
        if (messageId == null || !jsExecutor.isLoaded()) {
            log.info("appendIntermediateTextBelowThinking skipped: messageId={}, loaded={}", messageId, jsExecutor.isLoaded());
            return;
        }

        log.info("appendIntermediateTextBelowThinking: messageId={}, intermediateTexts.size={}",
                messageId, intermediateTexts.size());

        String js = buildIntermediateTextScript(messageId);
        jsExecutor.executeJavaScript(js);
    }

    /**
     * Builds the JavaScript for inserting/updating intermediate text below the loading indicator.
     */
    private String buildIntermediateTextScript(String messageId) {
        boolean isDark = ThemeDetector.isDarkTheme();
        String textColor = isDark ? "#B0BEC5" : "#546E7A";
        String separatorColor = isDark ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)";

        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();

        StringBuilder htmlBuilder = new StringBuilder();
        for (int i = 0; i < intermediateTexts.size(); i++) {
            if (i > 0) {
                htmlBuilder.append("<hr style=\"border:none;border-top:1px solid ")
                        .append(separatorColor).append(";margin:8px 0;\">");
            }
            String rendered = renderer.render(parser.parse(intermediateTexts.get(i)));
            htmlBuilder.append("<div class=\"intermediate-msg\">").append(rendered).append("</div>");
        }
        String escapedContent = jsExecutor.escapeJS(htmlBuilder.toString());

        String loadingId = "loading-" + jsExecutor.escapeJS(messageId);
        String escapedMessageId = jsExecutor.escapeJS(messageId);
        String siblingId = "agent-intermediate-" + escapedMessageId;

        // Find the loading indicator or assistant-message container and insert/update the
        // intermediate text div. The assistant-message fallback handles the case where the
        // loading indicator element has not yet been rendered by JCEF (async JS execution).
        return "try {\n" +
                "  var loader = document.getElementById('" + loadingId + "');\n" +
                "  if (!loader) {\n" +
                "    var messagePair = document.getElementById('" + escapedMessageId + "');\n" +
                "    if (messagePair) {\n" +
                "      loader = messagePair.querySelector('.loading-indicator');\n" +
                "    }\n" +
                "  }\n" +
                "  var container = loader ? loader.parentNode : null;\n" +
                "  if (!container) {\n" +
                "    var mp = document.getElementById('" + escapedMessageId + "');\n" +
                "    if (mp) { container = mp.querySelector('.assistant-message'); }\n" +
                "  }\n" +
                "  if (container) {\n" +
                "    var sib = document.getElementById('" + siblingId + "');\n" +
                "    if (!sib) {\n" +
                "      sib = document.createElement('div');\n" +
                "      sib.id = '" + siblingId + "';\n" +
                "      sib.style.cssText = 'padding:4px 8px;margin-top:4px;color:" + textColor + ";font-style:italic;';\n" +
                "      if (loader) {\n" +
                "        loader.parentNode.insertBefore(sib, loader.nextSibling);\n" +
                "      } else {\n" +
                "        container.appendChild(sib);\n" +
                "      }\n" +
                "    }\n" +
                // SECURITY: escapedContent is pre-escaped via HtmlRenderer(escapeHtml=true) + escapeJS()
                "    sib.innerHTML = `" + escapedContent + "`;\n" +
                "    setTimeout(function() { window.scrollTo(0, document.body.scrollHeight); }, 0);\n" +
                "  } else {\n" +
                "    console.error('appendIntermediateText: no container found for messageId=" + escapedMessageId + "');\n" +
                "  }\n" +
                "} catch (error) {\n" +
                "  console.error('Error appending intermediate text:', error);\n" +
                "}\n";
    }

    /**
     * Builds formatted HTML for all stored agent log entries.
     * All dynamic content is HTML-escaped before insertion.
     */
    private @NotNull String buildAgentFormattedLogsHtml() {
        StringBuilder html = new StringBuilder();

        html.append("<div class=\"agent-outer-container\">");
        html.append("<div class=\"agent-header\">Agent Activity</div>");

        for (ActivityMessage entry : agentLogs) {
            html.append("<div class=\"agent-log-entry ").append(getAgentCssClass(entry.getAgentType())).append("\">");
            if (entry.getAgentType() != AgentType.INTERMEDIATE_RESPONSE) {
                html.append("<span class=\"agent-counter\">[").append(entry.getCallNumber())
                        .append("/").append(entry.getMaxCalls()).append("]</span> ");
            }
            if (entry.getSubAgentId() != null) {
                html.append("<span class=\"agent-subagent-id\">[").append(escapeHtml(entry.getSubAgentId())).append("]</span> ");
            }
            html.append(getAgentIcon(entry.getAgentType())).append(" ");
            appendAgentEntryContent(html, entry);
            html.append("</div>\n");
        }

        html.append("</div>");
        return html.toString();
    }

    private void appendAgentEntryContent(@NotNull StringBuilder html, @NotNull ActivityMessage entry) {
        switch (entry.getAgentType()) {
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
                if (entry.getResult() != null) {
                    html.append(escapeHtml(truncate(entry.getResult())));
                } else {
                    html.append("LLM intermediate response");
                }
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

    private String getAgentCssClass(AgentType type) {
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

    private String getAgentIcon(AgentType type) {
        return switch (type) {
            case TOOL_REQUEST, SUB_AGENT_STARTED -> ICON_PLAY;
            case TOOL_RESPONSE, APPROVAL_GRANTED, SUB_AGENT_COMPLETED -> ICON_CHECKMARK;
            case TOOL_ERROR, APPROVAL_DENIED, SUB_AGENT_ERROR -> ICON_X_MARK;
            case LOOP_LIMIT -> ICON_WARNING;
            case APPROVAL_REQUESTED -> ICON_QUESTION;
            case INTERMEDIATE_RESPONSE -> ICON_SPEECH;
        };
    }

    private void updateAgentThinkingIndicator(String messageId, String content) {
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
     */
    @SuppressWarnings("java:S6035") // innerHTML usage is safe — all dynamic content is pre-escaped
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
                // SECURITY: escapedContent is pre-escaped (HTML entities + JS string escaping)
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

    // ===== Shared utilities =====

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
