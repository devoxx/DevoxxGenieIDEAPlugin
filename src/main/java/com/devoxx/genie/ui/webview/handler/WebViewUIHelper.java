package com.devoxx.genie.ui.webview.handler;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class for common WebView UI operations.
 * Contains shared methods used by different WebView handler classes.
 */
@Slf4j
public class WebViewUIHelper {
    
    /**
     * Marks the MCP logs as completed but keeps them visible.
     * Instead of hiding the loading indicator, this method adds a CSS class to indicate completion.
     *
     * @param jsExecutor The JavaScript executor used to run the JavaScript code
     * @param messageId The ID of the message
     */
    public static void markMCPLogsAsCompleted(@NotNull WebViewJavaScriptExecutor jsExecutor, @Nullable String messageId) {
        if (!jsExecutor.isLoaded() || messageId == null) {
            return;
        }
        
        log.debug("Marking MCP logs as completed for message ID: {}", messageId);
        
        // We're not hiding the indicator anymore, as we want to preserve MCP logs
        // Instead, we'll just mark it as completed and ensure it's properly positioned
        String js = "try {\n" +
                    "  const indicator = document.getElementById('loading-" + jsExecutor.escapeJS(messageId) + "');\n" +
                    "  if (indicator) {\n" +
                    "    indicator.classList.add('mcp-completed');\n" +
                    "    // Ensure the MCP logs are properly positioned after the AI response content\n" +
                    "    const assistantMessage = indicator.parentElement;\n" +
                    "    if (assistantMessage) {\n" +
                    "      assistantMessage.appendChild(indicator);\n" +
                    "    }\n" +
                    "  } else {\n" +
                    "    const messagePair = document.getElementById('" + jsExecutor.escapeJS(messageId) + "');\n" +
                    "    if (messagePair) {\n" +
                    "      const loadingIndicator = messagePair.querySelector('.loading-indicator');\n" +
                    "      if (loadingIndicator) {\n" +
                    "        loadingIndicator.classList.add('mcp-completed');\n" +
                    "        // Ensure the MCP logs are properly positioned after the AI response content\n" +
                    "        const assistantMessage = loadingIndicator.parentElement;\n" +
                    "        if (assistantMessage) {\n" +
                    "          assistantMessage.appendChild(loadingIndicator);\n" +
                    "        }\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "} catch (error) {\n" +
                    "  console.error('Error updating loading indicator:', error);\n" +
                    "}\n";
        
        jsExecutor.executeJavaScript(js);
    }
}
