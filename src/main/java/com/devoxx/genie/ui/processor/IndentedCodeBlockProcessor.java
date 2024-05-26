package com.devoxx.genie.ui.processor;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.StyleSheetsFactory;
import com.intellij.ui.JBColor;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.*;
import java.awt.*;

public class IndentedCodeBlockProcessor implements NodeProcessor {

    private final ChatMessageContext chatMessageContext;
    private final IndentedCodeBlock indentedCodeBlock;

    public IndentedCodeBlockProcessor(ChatMessageContext chatMessageContext,
                                      IndentedCodeBlock indentedCodeBlock) {
        this.chatMessageContext = chatMessageContext;
        this.indentedCodeBlock = indentedCodeBlock;
    }

    /**
     * Process the fenced code block.
     *
     * @return the panel
     */
    @Override
    public JPanel process() {
        HtmlRenderer htmlRenderer = createHtmlRenderer(chatMessageContext.getProject());
        String htmlOutput = htmlRenderer.render(indentedCodeBlock);

        JEditorPane editorPane = createEditorPane(htmlOutput, StyleSheetsFactory.createCodeStyleSheet());
        editorPane.setOpaque(false);
        editorPane.setBackground(JBColor.BLACK);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBackground(JBColor.BLACK);

        panel.add(editorPane, BorderLayout.CENTER);
        return panel;
    }
}
