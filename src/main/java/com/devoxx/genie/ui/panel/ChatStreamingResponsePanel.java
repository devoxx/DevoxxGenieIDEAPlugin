package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for displaying streaming chat responses.
 * This implementation now uses a WebView for rendering responses with PrismJS.
 */
public class ChatStreamingResponsePanel extends BackgroundPanel {

    private final StreamingWebViewResponsePanel webViewPanel;
    
    /**
     * Create a new chat response panel.
     *
     * @param chatMessageContext the chat message context
     */
    public ChatStreamingResponsePanel(@NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getId());

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        // Create and add the WebView-based streaming response panel
        webViewPanel = new StreamingWebViewResponsePanel(chatMessageContext);
        add(webViewPanel);
        
        // Set maximum size for proper display
        Dimension maximumSize = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        setMaximumSize(maximumSize);
    }

    /**
     * Insert token into document stream
     *
     * @param token the LLM string token
     */
    public void insertToken(String token) {
        // Delegate token insertion to the WebView panel
        webViewPanel.insertToken(token);
    }
}
