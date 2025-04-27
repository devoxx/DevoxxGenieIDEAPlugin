package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.webview.ConversationWebViewController;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * A panel containing a single WebView that displays the entire conversation.
 * This replaces the previous approach of creating separate panels for each message.
 */
@Slf4j
public class ConversationPanel extends JBPanel<ConversationPanel> implements CustomPromptChangeListener {

    private final ConversationWebViewController webViewController = new ConversationWebViewController();

    private final ResourceBundle resourceBundle;

    /**
     * Creates a new conversation panel.
     *
     * @param resourceBundle The resource bundle for i18n
     */
    public ConversationPanel(ResourceBundle resourceBundle) {
        super(new BorderLayout());
        this.resourceBundle = resourceBundle;

        // Create the WebViewController for managing the conversation content
        JBCefBrowser browser = webViewController.getBrowser();

        // Add the browser component directly to the panel
        // This is critical - make sure the browser component fills the entire panel
        JComponent browserComponent = browser.getComponent();
        browserComponent.setOpaque(true);
        browserComponent.setBackground(Color.BLACK);
        
        // Set browser layout and sizing
        add(browserComponent, BorderLayout.CENTER);
        
        // Set sizes for the panel to ensure proper display
        setMinimumSize(new Dimension(400, 300));
        setPreferredSize(new Dimension(800, 600));
        
        // Ensure the component is visible
        setOpaque(true);
        setBackground(Color.BLACK);
        setVisible(true);
    }

    /**
     * Show the welcome content.
     */
    public void showWelcome() {
        webViewController.loadWelcomeContent(resourceBundle);
    }

    /**
     * Add a chat message to the conversation.
     *
     * @param chatMessageContext The chat message context
     */
    public void addChatMessage(ChatMessageContext chatMessageContext) {
        webViewController.addChatMessage(chatMessageContext);
    }

    /**
     * Clear the conversation content.
     */
    public void clear() {
        webViewController.clearConversation();
        showWelcome();
    }

    /**
     * Called when custom prompts change - updates the welcome content if it's visible.
     */
    @Override
    public void onCustomPromptsChanged() {
        webViewController.updateCustomPrompts(resourceBundle);
    }
}
