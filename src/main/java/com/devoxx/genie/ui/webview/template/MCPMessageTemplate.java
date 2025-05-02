package com.devoxx.genie.ui.webview.template;

import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.ui.webview.WebServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template for generating HTML for MCP messages (tool outputs, AI responses, logs).
 * Formats messages in a style similar to Claude's UI/UX with clear delineation
 * between different message types and proper spacing and indentation.
 */
public class MCPMessageTemplate extends HtmlTemplate {
    
    private final MCPMessage mcpMessage;
    private static final Pattern JSON_PATTERN = Pattern.compile("^\\s*\\{.+\\}\\s*$", Pattern.DOTALL);
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("^\\s*function\\s+([a-zA-Z0-9_]+)\\s*\\(", Pattern.DOTALL);
    
    /**
     * Constructor with WebServer and MCPMessage dependencies.
     *
     * @param webServer The web server instance for resource URLs
     * @param mcpMessage The MCP message to render
     */
    public MCPMessageTemplate(WebServer webServer, MCPMessage mcpMessage) {
        super(webServer);
        this.mcpMessage = mcpMessage;
    }

    @Override
    public @NotNull String generate() {
        if (mcpMessage == null || mcpMessage.getContent() == null) {
            return "";
        }
        
        String content = mcpMessage.getContent();
        MCPType messageType = mcpMessage.getType();
        
        // Based on the message type, generate appropriate HTML
        switch (messageType) {
            case AI_MSG:
                return formatAIMessage(content);
            case TOOL_MSG:
                return formatToolMessage(content);
            case LOG_MSG:
            default:
                return formatLogMessage(content);
        }
    }
    
    /**
     * Format an AI message (responses from model)
     * 
     * @param content The message content
     * @return Formatted HTML
     */
    private @NotNull String formatAIMessage(@NotNull String content) {
        // Simple wrapper for AI messages
        return String.format(
            "<div class=\"ai-message\">%s</div>",
            escapeHtml(content)
        );
    }
    
    /**
     * Format a tool message (function call results)
     * 
     * @param content The message content
     * @return Formatted HTML
     */
    private @NotNull String formatToolMessage(@NotNull String content) {
        // Extract tool name and content if possible
        String toolName = detectToolName(content);
        String toolType = detectToolType(content);
        String badgeClass = getBadgeClassForToolType(toolType);
        
        // Generate HTML for the tool message with collapsible content
        StringBuilder htmlBuilder = new StringBuilder();
        
        htmlBuilder.append("<div class=\"tool-message\">");
        
        // Tool header with name and collapse toggle
        htmlBuilder.append("<div class=\"tool-message-header\">")
                  .append("<img src=\"/icons/tool.svg\" class=\"tool-icon\" alt=\"Tool\">")
                  .append(escapeHtml(toolName));
        
        // Add tool type badge if available
        if (toolType != null) {
            htmlBuilder.append("<span class=\"tool-badge ").append(badgeClass).append("\">")
                      .append(escapeHtml(toolType))
                      .append("</span>");
        }
        
        // Add toggle arrow
        htmlBuilder.append("<span class=\"toggle-icon\" onclick=\"toggleToolContent(this)\">▼</span>")
                  .append("</div>");
                  
        // Tool content (collapsible)
        htmlBuilder.append("<div class=\"tool-content\">")
                  .append("<pre class=\"tool-result\">").append(escapeHtml(content)).append("</pre>");
                  
        // Add copy button
        htmlBuilder.append("<button class=\"copy-tool-button\" onclick=\"copyToolOutput(this)\">")
                  .append("<img src=\"/icons/copy.svg\" alt=\"Copy\" class=\"copy-icon\">")
                  .append("</button>");
                  
        htmlBuilder.append("</div></div>");
        
        return htmlBuilder.toString();
    }
    
    /**
     * Format a log message
     * 
     * @param content The message content
     * @return Formatted HTML
     */
    private @NotNull String formatLogMessage(@NotNull String content) {
        // Check if this is an input or output log
        boolean isInput = content.startsWith("<");
        boolean isOutput = content.startsWith(">");
        
        String direction = isInput ? "Input" : (isOutput ? "Output" : "Log");
        String directionClass = isInput ? "input-log" : (isOutput ? "output-log" : "general-log");
        
        // Strip the direction marker if present
        if (isInput || isOutput) {
            content = content.substring(1).trim();
        }
        
        // Generate HTML for the log message
        StringBuilder htmlBuilder = new StringBuilder();
        
        htmlBuilder.append("<div class=\"tool-message ").append(directionClass).append("\">");
        
        // Log header with direction
        htmlBuilder.append("<div class=\"tool-message-header\">")
                  .append("<img src=\"/icons/log.svg\" class=\"tool-icon\" alt=\"Log\">")
                  .append(direction)
                  .append("<span class=\"tool-badge badge-log\">Log</span>")
                  .append("<span class=\"toggle-icon\" onclick=\"toggleToolContent(this)\">▼</span>")
                  .append("</div>");
                  
        // Log content (collapsible)
        htmlBuilder.append("<div class=\"tool-content\">")
                  .append("<pre class=\"tool-result\">").append(escapeHtml(content)).append("</pre>");
                  
        // Add copy button
        htmlBuilder.append("<button class=\"copy-tool-button\" onclick=\"copyToolOutput(this)\">")
                  .append("<img src=\"/icons/copy.svg\" alt=\"Copy\" class=\"copy-icon\">")
                  .append("</button>");
                  
        htmlBuilder.append("</div></div>");
        
        return htmlBuilder.toString();
    }
    
    /**
     * Attempt to detect the tool name from the message content
     * 
     * @param content The message content
     * @return Detected tool name or default
     */
    private @NotNull String detectToolName(@NotNull String content) {
        // Default tool name
        String toolName = "Tool Output";
        
        // Try to extract name from function calls
        Matcher functionMatcher = FUNCTION_PATTERN.matcher(content);
        if (functionMatcher.find()) {
            toolName = functionMatcher.group(1);
        }
        
        return toolName;
    }
    
    /**
     * Detect the tool type based on content analysis
     * 
     * @param content The message content
     * @return Tool type string or null
     */
    private @Nullable String detectToolType(@NotNull String content) {
        // Check if it's likely JSON data
        if (JSON_PATTERN.matcher(content).matches()) {
            return "Data";
        }
        
        // Check if it contains function definitions
        if (content.contains("function ") || content.contains("def ")) {
            return "Function";
        }
        
        // Default to generic tool
        return "Tool";
    }
    
    /**
     * Get the CSS class for the tool type badge
     * 
     * @param toolType The detected tool type
     * @return CSS class name
     */
    private @NotNull String getBadgeClassForToolType(@Nullable String toolType) {
        if (toolType == null) {
            return "badge-tool";
        }
        
        return switch (toolType) {
            case "Data" -> "badge-data";
            case "Function" -> "badge-function";
            default -> "badge-tool";
        };
    }
}
