package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.component.StyleSheetsFactory;
import com.devoxx.genie.ui.panel.chatresponse.ResponseHeaderPanel;
import com.devoxx.genie.ui.renderer.CodeBlockNodeRenderer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.impl.EditorCssFontResolver;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.devoxx.genie.ui.util.DevoxxGenieColorsUtil.PROMPT_BG_COLOR;
import static com.devoxx.genie.ui.util.DevoxxGenieColorsUtil.PROMPT_TEXT_COLOR;
import static com.devoxx.genie.ui.util.DevoxxGenieFontsUtil.SOURCE_CODE_PRO_FONT;

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
