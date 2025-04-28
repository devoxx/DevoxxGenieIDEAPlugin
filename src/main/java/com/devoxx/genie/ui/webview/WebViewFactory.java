package com.devoxx.genie.ui.webview;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import lombok.extern.slf4j.Slf4j;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Factory for creating JCef browser instances.
 * This class handles the initialization of JCEF components for the plugin.
 */
@Slf4j
public class WebViewFactory {

    private WebViewFactory() {
        // Utility class, no instances needed
    }
    
    /**
     * Creates a new JBCefBrowser instance and loads the specified URL.
     * 
     * @param url URL to load in the browser
     * @return A new JBCefBrowser instance
     */
    public static @NotNull JBCefBrowser createBrowser(String url) {
        // Ensure web server is running
        WebServer webServer = WebServer.getInstance();
        if (!webServer.isRunning()) {
            webServer.start();
        }
        
        // Create a simple browser without recursive builder calls
        JBCefBrowser browser = new JBCefBrowser();
        
        // Make sure the browser component takes up all available space
        browser.getComponent().setMinimumSize(new Dimension(100, 100));
        
        // Add load handler to detect load completion
        JBCefClient client = browser.getJBCefClient();
        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                log.debug("Browser loaded: " + url + " with status " + httpStatusCode);
            }
        }, browser.getCefBrowser());
        
        // Load the URL
        browser.loadURL(url);
        
        return browser;
    }
}
