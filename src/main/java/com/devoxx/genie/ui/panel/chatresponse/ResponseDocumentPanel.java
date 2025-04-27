package com.devoxx.genie.ui.panel.chatresponse;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.service.gitdiff.GitMergeService;
import com.devoxx.genie.ui.panel.WebViewResponsePanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import org.commonmark.node.FencedCodeBlock;
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
        setLayout(new BorderLayout());
        this.chatMessageContext = chatMessageContext;

        String markDownResponse = chatMessageContext.getAiMessage().text();
        Node document = Parser.builder().build().parse(markDownResponse);
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        if (Boolean.TRUE.equals(stateService.getGitDiffActivated())) {
            processGitDiff(chatMessageContext, document);
        }

        // Create the WebView-based response panel
        createWebViewPanel();
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
     * Create a WebView panel to display the response content
     */
    private void createWebViewPanel() {
        WebViewResponsePanel webViewResponsePanel = new WebViewResponsePanel(chatMessageContext);
        
        // Use a scrollable panel for the web view
        JBScrollPane scrollPane = new JBScrollPane(webViewResponsePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Add to the main panel
        add(scrollPane, BorderLayout.CENTER);
    }
}
