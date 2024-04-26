package com.devoxx.genie.ui.util;

import com.devoxx.genie.model.PromptContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class EditorUtil {

    public static final String DEFAULT_LANGUAGE = "Java";

    private EditorUtil() {
    }

    /**
     * Get the language and text from the editor.
     *
     * @param editor the editor
     * @return the language and text
     */
    public static PromptContext getPromptContextFromEditor(String userPrompt, Editor editor) {
        String languageName = DEFAULT_LANGUAGE;
        Document document = editor.getDocument();
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);

        AtomicReference<String> selectedTextRef = new AtomicReference<>();
        ApplicationManager.getApplication().runReadAction(() ->
            selectedTextRef.set(editor.getSelectionModel().getSelectedText()));

        String selectedText = selectedTextRef.get();

        if (selectedText == null && virtualFile != null) {
            FileType fileType = virtualFile.getFileType();
            languageName = fileType.getName();
            selectedText = document.getText();
            return new PromptContext(userPrompt, languageName, selectedText, List.of(virtualFile));
        }
        else {
            return new PromptContext(userPrompt, languageName, selectedText, List.of());
        }
    }
}
