package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.webview.WelcomeWebViewController;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * A WebView-based welcome panel for displaying the plugin's welcome content.
 * This class uses JCef WebView instead of the JEditorPane previously used.
 */
@Slf4j
public class WelcomeWebViewPanel extends JBPanel<WelcomeWebViewPanel> implements CustomPromptChangeListener {

    private final WelcomeWebViewController webViewController;
    private final JBCefBrowser browser;

    /**
     * Creates a new WebView-based welcome panel.
     *
     * @param resourceBundle The resource bundle for i18n
     */
    public WelcomeWebViewPanel(ResourceBundle resourceBundle) {
        super(new BorderLayout());

        // Create the WebViewController for managing the welcome content
        webViewController = new WelcomeWebViewController(resourceBundle);
        browser = webViewController.getBrowser();

        // Add the browser component to the panel
        add(browser.getComponent(), BorderLayout.CENTER);

        // Set a preferred size for the panel
        setPreferredSize(new Dimension(800, 600));
        
        // Ensure the browser component fills the entire panel
        browser.getComponent().setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
    }

    /**
     * Show the welcome panel.
     */
    public void showMsg() {
        setVisible(true);
        browser.getComponent().setVisible(true);

        // Ensure the browser is displayed at the top
        SwingUtilities.invokeLater(() -> {
            browser.getComponent().revalidate();
            browser.getComponent().repaint();
        });
    }

    /**
     * Called when custom prompts change - delegates to web view controller.
     */
    @Override
    public void onCustomPromptsChanged() {
        webViewController.onCustomPromptsChanged();
    }
}
