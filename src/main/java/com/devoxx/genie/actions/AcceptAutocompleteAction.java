package com.devoxx.genie.actions;

import com.devoxx.genie.DevoxxGenieClient;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * EXPERIMENTAL - NOT YET IMPLEMENTED
 * Action to accept the autocomplete suggestion from the Genie.
 */
public class AcceptAutocompleteAction extends EditorAction {

    protected AcceptAutocompleteAction() {
        super(new EditorActionHandler() {
            @Override
            protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
                if (editor.getProject() != null) {
                    Document document = editor.getDocument();
                    int caretPosition = editor.getCaretModel().getOffset();
                    String textBeforeCaret = document.getText().substring(0, caretPosition);
                    String response = DevoxxGenieClient.getInstance().executeGenieContinuePrompt(textBeforeCaret);
                    safeWrite(editor.getProject(), editor, response);
                }
            }

            @Override
            protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
                // Define when the action should be enabled
                return editor.getProject() != null;
            }
        });
    }

    public static void safeWrite(Project project, Editor editor, String text) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                ApplicationManager.getApplication().runWriteAction(() -> insertString(project, editor, text));
            }
        });
    }

    public static void insertString(Project project, Editor editor, String stringToInsert) {
        Document document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        // Ensure modification is done within a command and in the event dispatch thread
        WriteCommandAction.runWriteCommandAction(project, () -> document.insertString(offset, stringToInsert));
    }
}
