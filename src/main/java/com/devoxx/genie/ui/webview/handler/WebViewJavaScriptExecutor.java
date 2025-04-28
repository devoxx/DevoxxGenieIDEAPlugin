package com.devoxx.genie.ui.webview.handler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.jcef.JBCefBrowser;
import org.jetbrains.annotations.NotNull;

/**
 * Handles JavaScript execution in the WebView.
 * This class encapsulates the logic for executing JavaScript in the browser.
 */
public class WebViewJavaScriptExecutor {
    private static final Logger LOG = Logger.getInstance(WebViewJavaScriptExecutor.class);
    
    private final JBCefBrowser browser;
    private boolean isLoaded = false;
    
    public WebViewJavaScriptExecutor(JBCefBrowser browser) {
        this.browser = browser;
    }
    
    /**
     * Set the loaded state of the browser.
     * 
     * @param loaded true if the browser is loaded, false otherwise
     */
    public void setLoaded(boolean loaded) {
        this.isLoaded = loaded;
    }
    
    /**
     * Check if the browser is loaded.
     * 
     * @return true if the browser is loaded, false otherwise
     */
    public boolean isLoaded() {
        return isLoaded;
    }
    
    /**
     * Execute JavaScript in the browser.
     *
     * @param script The JavaScript to execute
     */
    public void executeJavaScript(String script) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (isLoaded) {
                browser.getCefBrowser().executeJavaScript(script, browser.getCefBrowser().getURL(), 0);
            } else {
                LOG.warn("Browser not loaded, cannot execute JavaScript");
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