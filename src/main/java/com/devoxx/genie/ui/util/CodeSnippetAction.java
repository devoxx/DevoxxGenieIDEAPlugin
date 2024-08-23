package com.devoxx.genie.ui.util;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.JHoverButton;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import org.commonmark.node.FencedCodeBlock;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.CopyIcon;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.InsertCodeIcon;

public class CodeSnippetAction {

    private final ChatMessageContext chatMessageContext;

    public CodeSnippetAction(ChatMessageContext chatMessageContext) {
        this.chatMessageContext = chatMessageContext;
    }

    public JPanel createClipBoardButtonPanel(FencedCodeBlock fencedCodeBlock) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        buttonPanel.setOpaque(true);

        JHoverButton copyButton = new JHoverButton(CopyIcon, true);
        copyButton.setToolTipText("Copy to clipboard");
        copyButton.addActionListener(e -> copyToClipboard(fencedCodeBlock.getLiteral()));
        buttonPanel.add(copyButton);

        JHoverButton insertButton = new JHoverButton(InsertCodeIcon, true);
        insertButton.setToolTipText("Insert code");
        insertButton.addActionListener(e -> insertCode(fencedCodeBlock.getLiteral()));
        buttonPanel.add(insertButton);

        return buttonPanel;
    }

    private void copyToClipboard(String codeSnippet) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(codeSnippet), null);
        NotificationUtil.sendNotification(chatMessageContext.getProject(), "Code copied to clipboard");
    }

    private void insertCode(String codeSnippet) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(chatMessageContext.getProject());
        Editor editor = fileEditorManager.getSelectedTextEditor();
        if (editor != null) {
            Document document = editor.getDocument();
            CaretModel caretModel = editor.getCaretModel();
            WriteCommandAction.runWriteCommandAction(chatMessageContext.getProject(), () -> {
                try {
                    document.insertString(caretModel.getOffset(), codeSnippet);
                } catch (Exception e) {
                    Logger.getInstance(getClass()).error(e.getMessage());
                }
            });
        }
    }
}
