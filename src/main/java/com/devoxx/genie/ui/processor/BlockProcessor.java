package com.devoxx.genie.ui.processor;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.StyleSheetsFactory;
import org.commonmark.node.Block;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.*;
import java.awt.*;

public class BlockProcessor implements NodeProcessor {
    private final ChatMessageContext chatMessageContext;
    private final Block block;

    public BlockProcessor(ChatMessageContext chatMessageContext,
                          Block block) {
        this.chatMessageContext = chatMessageContext;
        this.block = block;
    }

    /**
     * Process the paragraph
     * @return the panel
     */
    @Override
    public JPanel processNode() {
        HtmlRenderer htmlRenderer = createHtmlRenderer(chatMessageContext.getProject());
        String htmlOutput = htmlRenderer.render(block);

        JEditorPane editorPane = createEditorPane(htmlOutput, StyleSheetsFactory.createParagraphStyleSheet());
        editorPane.setOpaque(false);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(editorPane, BorderLayout.CENTER);
        return panel;
    }
}
