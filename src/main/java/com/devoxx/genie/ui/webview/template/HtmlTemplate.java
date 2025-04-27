package com.devoxx.genie.ui.webview.template;

import com.devoxx.genie.ui.webview.WebServer;
import org.jetbrains.annotations.NotNull;

/**
 * Base abstract class for HTML template generation.
 * This class provides common functionality for HTML template creation.
 */
public abstract class HtmlTemplate {
    
    protected final WebServer webServer;
    
    /**
     * Constructor with WebServer dependency.
     * 
     * @param webServer The web server instance for resource URLs
     */
    protected HtmlTemplate(WebServer webServer) {
        this.webServer = webServer;
    }
    
    /**
     * Generate complete HTML content.
     * 
     * @return Complete HTML content as a string
     */
    public abstract @NotNull String generate();
    
    /**
     * Escape HTML special characters in a string.
     *
     * @param text The text to escape
     * @return Escaped text
     */
    protected @NotNull String escapeHtml(@NotNull String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
    
    /**
     * Escape JavaScript string literals.
     * This prevents issues when inserting HTML into JavaScript template literals.
     *
     * @param text The text to escape
     * @return Escaped text suitable for use in JavaScript
     */
    protected String escapeJS(String text) {
        return text.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("${", "\\${");
    }
}
