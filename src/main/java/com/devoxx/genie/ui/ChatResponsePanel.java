package com.devoxx.genie.ui;

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

import static com.devoxx.genie.ui.util.DevoxxGenieColors.PROMPT_BG_COLOR;
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

        setName(chatMessageContext.getName());

        this.chatMessageContext = chatMessageContext;

        add(new ResponseHeaderPanel(chatMessageContext)
            .withBackground(PROMPT_BG_COLOR));

        addResponsePane(chatMessageContext);

        if (chatMessageContext.hasFiles()) {
            ExpandablePanel fileListPanel = new ExpandablePanel(chatMessageContext);
            add(fileListPanel);
        }
    }

    /**
     * Get the response pane with rendered HTML.
     * @param chatMessageContext the chat message context
     */
    private void addResponsePane(@NotNull ChatMessageContext chatMessageContext) {
        String markDownResponse = chatMessageContext.getAiMessage().text();
        Node document = Parser.builder().build().parse(markDownResponse);
        addDocumentNodes(document);
    }

    /**
     * Add document nodes to the panel.
     * @param document the document
     */
    private void addDocumentNodes(@NotNull Node document) {
        JPanel jPanel = createPanel();

        Node node = document.getFirstChild();

        while (node != null) {
            if (node instanceof FencedCodeBlock fencedCodeBlock) {
                addFencedCodeBlockNode(jPanel, fencedCodeBlock);
            } else if (node instanceof IndentedCodeBlock indentedCodeBlock) {
                addIndentedCodeBlock(jPanel, indentedCodeBlock);
            } else {
                addBlockNode(jPanel, node);
            }
            node = node.getNext();
        }

        add(jPanel);
    }

    private @NotNull JPanel createPanel() {

        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
        jPanel.setOpaque(false);
        jPanel.setFont(SourceCodeProFontPlan14);
        return jPanel;
    }

    private void addFencedCodeBlockNode(@NotNull JPanel panel, FencedCodeBlock fencedCodeBlock) {
        panel.add(processBlock(fencedCodeBlock));
    }

    private void addIndentedCodeBlock(@NotNull JPanel panel, IndentedCodeBlock indentedCodeBlock) {
        panel.add(processBlock(indentedCodeBlock));
    }

    private void addBlockNode(@NotNull JPanel panel, Node node) {
        panel.add(processBlock((Block) node));
    }

    /**
     * Process a block and return a panel.
     * @param theBlock the block
     * @return the panel
     */
    private JPanel processBlock(Block theBlock) {
        return NodeProcessorFactory.createProcessor(chatMessageContext, theBlock).process();
    }
}
