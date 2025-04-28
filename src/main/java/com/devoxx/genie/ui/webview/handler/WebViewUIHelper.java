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
     * Hides the loading indicator for a message when no activity is detected.
     *
     * @param jsExecutor The JavaScript executor used to run the JavaScript code
     * @param messageId The ID of the message
     */
    public static void hideLoadingIndicator(@NotNull WebViewJavaScriptExecutor jsExecutor, @Nullable String messageId) {
        if (!jsExecutor.isLoaded() || messageId == null) {
            return;
        }
        
        log.debug("Hiding loading indicator for message ID: {}", messageId);
        
        String js = "try {\n" +
                    "  const indicator = document.getElementById('loading-" + jsExecutor.escapeJS(messageId) + "');\n" +
                    "  if (indicator) {\n" +
                    "    indicator.style.display = 'none';\n" +
                    "  } else {\n" +
                    "    const messagePair = document.getElementById('" + jsExecutor.escapeJS(messageId) + "');\n" +
                    "    if (messagePair) {\n" +
                    "      const loadingIndicator = messagePair.querySelector('.loading-indicator');\n" +
                    "      if (loadingIndicator) {\n" +
                    "        loadingIndicator.style.display = 'none';\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "} catch (error) {\n" +
                    "  console.error('Error hiding loading indicator:', error);\n" +
                    "}\n";
        
        jsExecutor.executeJavaScript(js);
    }
}
