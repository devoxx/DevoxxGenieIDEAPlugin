package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.ui.webview.JCEFChecker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Handles JavaScript execution in the WebView.
 * This class encapsulates the logic for executing JavaScript in the browser.
 */
@Slf4j
public class WebViewJavaScriptExecutor {

    private final JBCefBrowser browser;

    @Getter
    @Setter
    private boolean isLoaded = false;
    
    public WebViewJavaScriptExecutor(JBCefBrowser browser) {
        this.browser = browser;
    }

    /**
     * Execute JavaScript in the browser.
     *
     * @param script The JavaScript to execute
     */
    public void executeJavaScript(String script) {
        // Skip JavaScript execution if JCEF is not available or browser is null
        if (!JCEFChecker.isJCEFAvailable() || browser == null) {
            log.debug("JCEF is not available or browser is null, skipping JavaScript execution: {}", 
                    script != null && script.length() > 50 ? script.substring(0, 50) + "..." : script);
            return;
        }
        
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                if (isLoaded) {
                    browser.getCefBrowser().executeJavaScript(script, browser.getCefBrowser().getURL(), 0);
                } else {
                    log.warn("Browser not loaded or not initialized properly, cannot execute JavaScript");
                }
            } catch (Exception e) {
                log.error("Error executing JavaScript: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Escape JavaScript string literals.
     * This prevents issues when inserting HTML into JavaScript template literals.
     *
     * @param text The text to escape
     * @return Escaped text suitable for use in JavaScript
     */
    public @NotNull String escapeJS(@NotNull String text) {
        return text.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("${", "\\${");
    }
    
    /**
     * Escape HTML special characters.
     *
     * @param text The text to escape
     * @return Escaped HTML text
     */
    public @NotNull String escapeHtml(@NotNull String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
