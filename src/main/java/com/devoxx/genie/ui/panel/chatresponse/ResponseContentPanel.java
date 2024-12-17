package com.devoxx.genie.ui.panel.chatresponse;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.processor.NodeProcessorFactory;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.commonmark.node.Block;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

import javax.swing.*;
import java.awt.*;

// ResponseContentPanel.java
public class ResponseContentPanel extends JPanel {

    private final transient ChatMessageContext chatMessageContext;

    public ResponseContentPanel(ChatMessageContext chatMessageContext) {
        this.chatMessageContext = chatMessageContext;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        Node document = parseMarkdown(chatMessageContext.getAiMessage().text());
        processGitDiffIfEnabled(document);
        addDocumentNodesToPanel(document);
    }

    private Node parseMarkdown(String markdownText) {
        return Parser.builder().build().parse(markdownText);
    }

    private void processGitDiffIfEnabled(Node document) {
        if (isGitDiffEnabled()) {
            // ... (Implementation for processing Git diff, you can move the relevant code here)
        }
    }

    private boolean isGitDiffEnabled() {
        return Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getGitDiffActivated());
    }

    private void addDocumentNodesToPanel(Node document) {
        Node node = document.getFirstChild();
        while (node != null) {
            addNodeToPanel(node);
            node = node.getNext();
        }
    }

    private void addNodeToPanel(Node node) {
        JPanel nodePanel = processNode(node);
        setFullWidth(nodePanel);
        add(nodePanel);
    }

    private JPanel processNode(Node node) {
        if (node instanceof Block block) {
            return processBlock(block);
        }
        return new JPanel();
    }

    private JPanel processBlock(Block block) {
        return NodeProcessorFactory.createProcessor(chatMessageContext, block).processNode();
    }

    private void setFullWidth(JPanel panel) {
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        panel.setMinimumSize(new Dimension(panel.getPreferredSize().width, panel.getPreferredSize().height));
    }
}
