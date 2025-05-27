package com.devoxx.genie.ui.webview.handler;

import com.intellij.ide.BrowserUtil;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.extern.slf4j.Slf4j;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Handler for managing external links in the webview.
 * Intercepts navigation requests and opens external URLs in the system browser
 * while allowing internal navigation within the chat interface.
 */
@Slf4j
public class WebViewExternalLinkHandler extends CefRequestHandlerAdapter {
    
    private final String internalServerUrl;
    private final WebViewJavaScriptExecutor jsExecutor;
    
    /**
     * Creates new external link handler.
     * 
     * @param internalServerUrl The URL of the internal web server (e.g., "http://localhost:8090")
     * @param jsExecutor JavaScript executor for injecting link handling scripts
     */
    public WebViewExternalLinkHandler(@NotNull String internalServerUrl, 
                                     @NotNull WebViewJavaScriptExecutor jsExecutor) {
        this.internalServerUrl = internalServerUrl;
        this.jsExecutor = jsExecutor;
    }
    
    /**
     * Intercepts navigation requests to handle external links.
     * External URLs are opened in the system browser and blocked from loading in the webview.
     */
    @Override
    public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean userGesture, boolean isRedirect) {
        String url = request.getURL();
        
        log.debug("Navigation request: URL={}, userGesture={}, isRedirect={}, frame={}", 
                 url, userGesture, isRedirect, frame.isMain() ? "main" : "sub");
        
        // Allow internal server resources and data URLs to load normally
        if (isInternalUrl(url)) {
            log.debug("Allowing internal URL to load: {}", url);
            return false; // Allow the request to proceed
        }
        
        // Only handle external URLs if it's a user gesture in the main frame
        // This prevents interference with iframes, redirects, and programmatic navigation
        if (isExternalUrl(url) && userGesture && frame.isMain() && !isRedirect) {
            log.info("Intercepting external URL for system browser: {}", url);
            
            try {
                // Use ApplicationManager to ensure we're on the correct thread
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        BrowserUtil.browse(url);
                        log.debug("Successfully opened external URL: {}", url);
                    } catch (Exception e) {
                        log.error("Failed to open external URL: {}", url, e);
                    }
                });
            } catch (Exception e) {
                log.error("Error scheduling external URL opening: {}", url, e);
            }
            
            // Block the request from loading in the webview
            return true;
        }
        
        // Allow all other requests to proceed normally
        log.debug("Allowing URL to proceed: {}", url);
        return false;
    }
    
    /**
     * Checks if a URL is internal (should load in webview).
     */
    private boolean isInternalUrl(@NotNull String url) {
        return url.startsWith(internalServerUrl) || 
               url.startsWith("data:") || 
               url.startsWith("about:") ||
               url.startsWith("javascript:");
    }
    
    /**
     * Checks if a URL is external (should open in system browser).
     */
    private boolean isExternalUrl(@NotNull String url) {
        try {
            URL parsedUrl = new URL(url);
            String protocol = parsedUrl.getProtocol().toLowerCase();
            
            // Consider HTTP and HTTPS URLs as external if they're not from our internal server
            return ("http".equals(protocol) || "https".equals(protocol)) && 
                   !url.startsWith(internalServerUrl);
        } catch (MalformedURLException e) {
            log.debug("Malformed URL, treating as internal: {}", url);
            return false;
        }
    }
    
    /**
     * Inject JavaScript to handle link clicks client-side and add visual indicators.
     */
    public void injectLinkHandlingScript() {
        String script = """
            (function() {
                console.log('Initializing external link handler...');
                
                // Function to add visual indicators to external links
                function addExternalLinkIndicators() {
                    document.querySelectorAll('a[href^="http"]').forEach(function(link) {
                        const href = link.getAttribute('href');
                        if (href && !href.startsWith(window.location.origin) && !link.querySelector('.external-link-indicator')) {
                            const indicator = document.createElement('span');
                            indicator.className = 'external-link-indicator';
                            indicator.innerHTML = '↗';
                            indicator.title = 'Opens in external browser';
                            link.appendChild(indicator);
                        }
                    });
                }
                
                // Add indicators to existing links
                addExternalLinkIndicators();
                
                // Set up mutation observer for dynamically added links
                const observer = new MutationObserver(function(mutations) {
                    let shouldUpdate = false;
                    mutations.forEach(function(mutation) {
                        mutation.addedNodes.forEach(function(node) {
                            if (node.nodeType === Node.ELEMENT_NODE) {
                                if (node.tagName === 'A' || node.querySelector('a')) {
                                    shouldUpdate = true;
                                }
                            }
                        });
                    });
                    
                    if (shouldUpdate) {
                        addExternalLinkIndicators();
                    }
                });
                
                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });
                
                console.log('External link handler initialized successfully');
            })();
            """;
        
        jsExecutor.executeJavaScript(script);
    }
    
    /**
     * Set up the JavaScript bridge for external link handling.
     * This method is kept for compatibility but the main handling is done via onBeforeBrowse.
     */
    public void setupExternalLinkBridge(@NotNull JBCefBrowser browser) {
        try {
            log.debug("Setting up external link bridge (primarily for visual indicators)");
            // The main work is done by onBeforeBrowse interceptor, but we still inject 
            // the script for visual indicators
            injectLinkHandlingScript();
        } catch (Exception e) {
            log.error("Error setting up external link bridge", e);
        }
    }
}
