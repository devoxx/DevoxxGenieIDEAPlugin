package com.devoxx.genie.ui.util;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.tdg.CodeGeneratorService;
import com.devoxx.genie.ui.component.JHoverButton;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
<<<<<<< HEAD
=======
import com.intellij.openapi.vfs.VirtualFile;
>>>>>>> master
import org.commonmark.node.FencedCodeBlock;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.*;

public class CodeSnippetAction {

    private final ChatMessageContext chatMessageContext;

    public CodeSnippetAction(ChatMessageContext chatMessageContext) {
        this.chatMessageContext = chatMessageContext;
    }

    public JPanel createClipBoardButtonPanel(ChatMessageContext chatMessageContext, FencedCodeBlock fencedCodeBlock) {
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

        String commandName = chatMessageContext.getCommandName();
        if (commandName != null && commandName.equalsIgnoreCase("tdg")) {
            JHoverButton createButton = new JHoverButton(CreateIcon, true);
            createButton.setToolTipText("Create class");
            createButton.addActionListener(e -> createClass(fencedCodeBlock.getLiteral()));
            buttonPanel.add(createButton);
        }

        return buttonPanel;
    }

    /**
     * Copy the given code snippet to the clipboard.
     * @param codeSnippet The code snippet to copy
     */
    private void copyToClipboard(String codeSnippet) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(codeSnippet), null);
        NotificationUtil.sendNotification(chatMessageContext.getProject(), "Code copied to clipboard");
    }

    /**
     * Insert the given code snippet into the editor.
     * @param codeSnippet The code snippet to insert
     */
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

    /**
     * Create a class from the given code snippet.
     * @param codeSnippet The code snippet to create the class from
     */
    private void createClass(String codeSnippet) {
        if (chatMessageContext.getCommandName().equalsIgnoreCase("tdg")) {
            CodeGeneratorService.createClassFromCodeSnippet(chatMessageContext, codeSnippet);
        }
    }
}
