package com.devoxx.genie.ui.panel.chatresponse;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.service.gitdiff.GitMergeService;
import com.devoxx.genie.ui.processor.NodeProcessorFactory;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.vfs.VirtualFile;
import org.commonmark.node.Block;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ResponseDocumentPanel extends JPanel {

    private final transient ChatMessageContext chatMessageContext;

    public ResponseDocumentPanel(@NotNull ChatMessageContext chatMessageContext) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.chatMessageContext = chatMessageContext;

        String markDownResponse = chatMessageContext.getAiMessage().text();
        Node document = Parser.builder().build().parse(markDownResponse);
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        if (Boolean.TRUE.equals(stateService.getGitDiffActivated())) {
            processGitDiff(chatMessageContext, document);
        }

        addDocumentNodesToPanel(document);
    }

    private void processGitDiff(@NotNull ChatMessageContext chatMessageContext, @NotNull Node document) {
        // Get original file info
        EditorInfo editorInfo = chatMessageContext.getEditorInfo();
        if (editorInfo == null) {
            return;
        }

        if (editorInfo.getSelectedFiles() != null && !editorInfo.getSelectedFiles().isEmpty()) {
            List<VirtualFile> files = editorInfo.getSelectedFiles();
            List<String> modifiedContents = new ArrayList<>();

            // Collect modified contents from code blocks
            Node node = document.getFirstChild();
            while (node != null) {
                if (node instanceof FencedCodeBlock codeBlock) {
                    modifiedContents.add(codeBlock.getLiteral());
                }
                node = node.getNext();
            }

            GitMergeService.getInstance().showDiffView(
                    chatMessageContext.getProject(),
                    files.get(0),
                    modifiedContents.isEmpty() ? "" : modifiedContents.get(0));
        }
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

    private @NotNull JPanel createPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
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
