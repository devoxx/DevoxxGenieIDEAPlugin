package com.devoxx.genie.ui.util;

import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.util.FileUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
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
    public static @NotNull EditorInfo getEditorInfo(Editor editor) {

        EditorInfo editorInfo = new EditorInfo();

        ApplicationManager.getApplication().runReadAction(() -> {

            Document document = editor.getDocument();

            // This is the place where We can also include more details from the document editor
            // and pass them in EditorInfo.

            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);

            if (virtualFile != null) {
                editorInfo.setSelectedFiles(List.of(virtualFile));
                SelectionModel selectionModel = editor.getSelectionModel();
                String selectedText = selectionModel.getSelectedText();
                if (selectedText != null && !selectedText.isEmpty()) {
                    editorInfo.setSelectedText(selectedText);
                    String filePath = virtualFile.getCanonicalPath();
                    editorInfo.setSelectionFilePath(filePath != null ? filePath : virtualFile.getPath());

                    int selectionStart = selectionModel.getSelectionStart();
                    int selectionEnd = selectionModel.getSelectionEnd();
                    editorInfo.setSelectionStartLine(document.getLineNumber(selectionStart) + 1);
                    editorInfo.setSelectionEndLine(document.getLineNumber(Math.max(selectionStart, selectionEnd - 1)) + 1);
                }

                editorInfo.setLanguage(FileUtil.getFileType(virtualFile));
            }
        });

        return editorInfo;
    }
}
