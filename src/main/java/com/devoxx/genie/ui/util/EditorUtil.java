package com.devoxx.genie.ui.util;

import com.devoxx.genie.model.request.EditorInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EditorUtil {

    private EditorUtil() {
    }

    /**
     * Get the editor info.
     *
     * @param editor the editor
     * @return the editor info
     */
    public static @NotNull EditorInfo getEditorInfo(Project project, Editor editor) {

        EditorInfo editorInfo = new EditorInfo();

        ApplicationManager.getApplication().runReadAction(() -> {

            Document document = editor.getDocument();

            // This is the place where We can also include more details from the document editor
            // and pass them in EditorInfo.

            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);

            if (virtualFile != null) {
                String selectedText = editor.getSelectionModel().getSelectedText();
                if (selectedText != null) {
                    editorInfo.setSelectedText(selectedText);
                } else {
                    editorInfo.setSelectedFiles(List.of(virtualFile));
                }

                // Get the file type of the file which represents the programming language (if any) of the file.
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if (psiFile != null) {
                    editorInfo.setLanguage(psiFile.getFileType().getName());
                }
            }
        });

        return editorInfo;
    }
}
