package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
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
     * Handles both streaming and non-streaming modes:
     * - Non-streaming: adds .mcp-completed class to mcp-{messageId} (separate section between user and assistant)
     * - Streaming: adds .mcp-completed class to the loading indicator (at top of assistant-message)
     *
     * @param jsExecutor The JavaScript executor used to run the JavaScript code
     * @param messageId The ID of the message
     */
    public static void markMCPLogsAsCompleted(@NotNull WebViewJavaScriptExecutor jsExecutor, @Nullable String messageId) {
        if (!jsExecutor.isLoaded() || messageId == null) {
            return;
        }

        log.debug("Marking MCP logs as completed for message ID: {}", messageId);

        boolean isStreaming = Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getStreamMode());
        String escapedMessageId = jsExecutor.escapeJS(messageId);

        String js;
        if (isStreaming) {
            // Streaming: mark the loading indicator (which has mcp-inline class) as completed
            // Keep it at the top of assistant-message, do NOT reposition
            js = "try {\n" +
                    "  const indicator = document.getElementById('loading-" + escapedMessageId + "');\n" +
                    "  if (indicator && indicator.querySelector('.mcp-outer-container')) {\n" +
                    "    indicator.classList.add('mcp-completed');\n" +
                    "  }\n" +
                    "  if (!indicator) {\n" +
                    "    const messagePair = document.getElementById('" + escapedMessageId + "');\n" +
                    "    if (messagePair) {\n" +
                    "      const li = messagePair.querySelector('.loading-indicator');\n" +
                    "      if (li && li.querySelector('.mcp-outer-container')) {\n" +
                    "        li.classList.add('mcp-completed');\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "} catch (error) {\n" +
                    "  console.error('Error updating loading indicator:', error);\n" +
                    "}\n";
        } else {
            // Non-streaming: mark the separate mcp-{messageId} section as completed
            js = "try {\n" +
                    "  const mcpSection = document.getElementById('mcp-" + escapedMessageId + "');\n" +
                    "  if (mcpSection && mcpSection.querySelector('.mcp-outer-container')) {\n" +
                    "    mcpSection.classList.add('mcp-completed');\n" +
                    "  }\n" +
                    "  // Also hide the loading indicator since MCP is in separate section\n" +
                    "  const indicator = document.getElementById('loading-" + escapedMessageId + "');\n" +
                    "  if (indicator) {\n" +
                    "    indicator.style.display = 'none';\n" +
                    "  }\n" +
                    "} catch (error) {\n" +
                    "  console.error('Error updating MCP section:', error);\n" +
                    "}\n";
        }

        jsExecutor.executeJavaScript(js);
    }
}
