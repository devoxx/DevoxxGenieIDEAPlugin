package com.devoxx.genie.ui.processor;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.component.StyleSheetsFactory;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.ui.components.JBPanel;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import static com.devoxx.genie.ui.util.DevoxxGenieColors.CODE_BG_COLOR;
import static com.devoxx.genie.ui.util.DevoxxGenieColors.CODE_BORDER_BG_COLOR;
import static com.devoxx.genie.ui.util.DevoxxGenieIcons.CopyIcon;
import static com.devoxx.genie.ui.util.DevoxxGenieIcons.InsertCodeIcon;

public class FencedCodeBlockProcessor implements NodeProcessor {

    private final ChatMessageContext chatMessageContext;
    private final FencedCodeBlock fencedCodeBlock;

    public FencedCodeBlockProcessor(ChatMessageContext chatMessageContext,
                                    FencedCodeBlock fencedCodeBlock) {
        this.chatMessageContext = chatMessageContext;
        this.fencedCodeBlock = fencedCodeBlock;
    }

    /**
     * Process the fenced code block.
     * @return the panel
     */
    @Override
    public JPanel process() {
        HtmlRenderer htmlRenderer = createHtmlRenderer(chatMessageContext.getProject());
        String htmlOutput = htmlRenderer.render(fencedCodeBlock);
        JEditorPane editorPane = createEditorPane(htmlOutput, StyleSheetsFactory.createCodeStyleSheet());
        editorPane.setBorder(BorderFactory.createLineBorder(CODE_BORDER_BG_COLOR, 1));

        // Initialize the overlay panel and set the OverlayLayout correctly
        JPanel overlayPanel = new JPanel(new BorderLayout());
        overlayPanel.setBackground(CODE_BG_COLOR);
        overlayPanel.setOpaque(true);

        // Add components to the overlay panel in the correct order
        overlayPanel.add(editorPane, BorderLayout.CENTER);  // Editor pane at the bottom
        overlayPanel.add(createClipBoardButtonPanel(fencedCodeBlock, editorPane), BorderLayout.NORTH); // Button panel on top

        return overlayPanel;
    }

    /**
     * Add a clipboard button to the panel.
     * @param fencedCodeBlock the fenced code block
     */
    private JPanel createClipBoardButtonPanel(FencedCodeBlock fencedCodeBlock, JEditorPane editorPane) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        buttonPanel.setOpaque(true);
        buttonPanel.setVisible(true);

        JHoverButton copyButton = new JHoverButton(CopyIcon, true);
        copyButton.setToolTipText("Copy to clipboard");
        copyButton.addActionListener(e -> copyToClipboard(fencedCodeBlock.getLiteral()));
        copyButton.setVisible(true);
        copyButton.setOpaque(true);
        buttonPanel.add(copyButton);

        JHoverButton insertButton = new JHoverButton(InsertCodeIcon, true);
        insertButton.setToolTipText("Insert code");
        insertButton.addActionListener(e -> insertCode(fencedCodeBlock.getLiteral()));
        insertButton.setVisible(true);
        insertButton.setOpaque(true);
        buttonPanel.add(insertButton);
        return buttonPanel;
    }

    /**
     * Copy the text to the clipboard.
     * @param codeSnippet the code snippet to copy
     */
    private void copyToClipboard(String codeSnippet) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(codeSnippet), null);
        NotificationUtil.sendNotification(chatMessageContext.getProject(), "Code copied to clipboard");
    }

    /**
     * Insert the code snippet into the editor.
     * @param codeSnippet the code snippet to insert
     */
    private void insertCode(String codeSnippet) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(chatMessageContext.getProject());
        Editor selectedTextEditor = fileEditorManager.getSelectedTextEditor();
        Editor editor = fileEditorManager.getSelectedTextEditor();
        if (editor != null && selectedTextEditor != null) {
            Document document = selectedTextEditor.getDocument();
            CaretModel caretModel = editor.getCaretModel();
            WriteCommandAction.runWriteCommandAction(chatMessageContext.getProject(), () -> {
                try {
                    document.insertString(caretModel.getOffset(), codeSnippet.subSequence(0, codeSnippet.length()));
                } catch (Exception e) {
                    Logger.getInstance(getClass()).error(e.getMessage());
                }
            });
        }
    }
}
