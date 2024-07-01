package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.processor.NodeProcessorFactory;
import org.commonmark.node.Block;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.ui.util.DevoxxGenieFonts.SourceCodeProFontPlan14;

public class ChatResponsePanel extends BackgroundPanel {

    private final ChatMessageContext chatMessageContext;

    /**
     * Create a new chat response panel.
     * @param chatMessageContext the chat message context
     */
    public ChatResponsePanel(@NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getName());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.chatMessageContext = chatMessageContext;

        add(new ResponseHeaderPanel(chatMessageContext));
        addResponsePane(chatMessageContext);

        if (chatMessageContext.hasFiles()) {
            ExpandablePanel fileListPanel = new ExpandablePanel(chatMessageContext);
            add(fileListPanel);
        }
    }

    /**
     * Get the response pane with rendered HTML.
     *
     * @param chatMessageContext the chat message context
     */
    private void addResponsePane(@NotNull ChatMessageContext chatMessageContext) {
        String markDownResponse = chatMessageContext.getAiMessage().text();
        Node document = Parser.builder().build().parse(markDownResponse);
        addDocumentNodesToPanel(document);
    }

    /**
     * Add document nodes to the panel.
     *
     * @param document the document
     */
    private void addDocumentNodesToPanel(@NotNull Node document) {
        JPanel jPanel = createPanel();

        Node node = document.getFirstChild();

        while (node != null) {
            JPanel panel;
            if (node instanceof FencedCodeBlock fencedCodeBlock) {
                panel = processBlock(fencedCodeBlock);
            } else if (node instanceof IndentedCodeBlock indentedCodeBlock) {
                panel = processBlock(indentedCodeBlock);
            } else {
                panel = processBlock((Block) node);
            }

            setFullWidth(panel);
            jPanel.add(panel);
            node = node.getNext();
        }

        add(jPanel);
    }

    private void setFullWidth(@NotNull JPanel panel) {
        Dimension maximumSize = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        panel.setMaximumSize(maximumSize);
        panel.setMinimumSize(new Dimension(panel.getPreferredSize().width, panel.getPreferredSize().height));
    }

    /**
     * Create a panel.
     *
     * @return the panel
     */
    private @NotNull JPanel createPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
        jPanel.setOpaque(false);
        jPanel.setFont(SourceCodeProFontPlan14);
        return jPanel;
    }

    /**
     * Process a block and return a panel.
     *
     * @param theBlock the block
     * @return the panel
     */
    private JPanel processBlock(Block theBlock) {
        return NodeProcessorFactory.createProcessor(chatMessageContext, theBlock).processNode();
    }
}
