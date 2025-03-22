package com.devoxx.genie.ui.util;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.tdg.CodeGeneratorService;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.FencedCodeBlock;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieColorsUtil.PROMPT_BG_COLOR;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.*;
import static com.devoxx.genie.util.ClipboardUtil.copyToClipboard;

@Slf4j
public class CodeSnippetAction {

    private final ChatMessageContext chatMessageContext;

    public CodeSnippetAction(ChatMessageContext chatMessageContext) {
        this.chatMessageContext = chatMessageContext;
    }

    public JPanel createClipBoardButtonPanel(@NotNull ChatMessageContext chatMessageContext,
                                             FencedCodeBlock fencedCodeBlock) {

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        buttonPanel.setBackground(PROMPT_BG_COLOR);
        buttonPanel.add(
                createActionButton(CopyIcon,
                        "Copy to clipboard",
                        e -> {
                            copyToClipboard(fencedCodeBlock.getLiteral());
                            NotificationUtil.sendNotification(chatMessageContext.getProject(), "Code copied to clipboard");
                        })
        );

        buttonPanel.add(
                createActionButton(InsertCodeIcon,
                        "Insert code",
                        e -> insertCode(fencedCodeBlock.getLiteral()))
        );

        String commandName = chatMessageContext.getCommandName();
        if (commandName != null && commandName.equalsIgnoreCase("tdg")) {
            buttonPanel.add(
                    createActionButton(CreateIcon,
                            "Create class",
                            e -> createClass(fencedCodeBlock.getLiteral()))
            );
        }

        return buttonPanel;
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
                    log.error(e.getMessage());
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
