package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.webview.WebViewController;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * A panel that uses JCEF WebView to display formatted response content with PrismJS syntax highlighting.
 * This replaces the previous Swing-based approach for displaying markdown and code blocks.
 */
public class WebViewResponsePanel extends JPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(WebViewResponsePanel.class);
    
    private final JBCefBrowser browser;
    private final WebViewController controller;
    private final Project project;
    
    /**
     * Creates a new WebViewResponsePanel.
     *
     * @param chatMessageContext the chat message context containing the response
     */
    public WebViewResponsePanel(@NotNull ChatMessageContext chatMessageContext) {
        this.project = chatMessageContext.getProject();
        
        setLayout(new BorderLayout());
        
        // Initialize the web view controller
        controller = new WebViewController(chatMessageContext);
        browser = controller.getBrowser();
        
        // Add the browser component to the panel
        add(browser.getComponent(), BorderLayout.CENTER);
    }
    
    @Override
    public void dispose() {
        if (browser != null) {
            browser.dispose();
        }
    }
    
    /**
     * Returns the browser component.
     *
     * @return the JBCefBrowser instance
     */
    public JBCefBrowser getBrowser() {
        return browser;
    }
    
    /**
     * Returns the web view controller.
     *
     * @return the WebViewController instance
     */
    public WebViewController getController() {
        return controller;
    }
}
