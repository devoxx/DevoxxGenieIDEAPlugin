package com.devoxx.genie.ui.util;

import com.devoxx.genie.model.LanguageTextPair;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;

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
    public static LanguageTextPair getEditorLanguageAndText(Editor editor) {
        String languageName = DEFAULT_LANGUAGE;
        Document document = editor.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);

        AtomicReference<String> selectedTextRef = new AtomicReference<>();
        ApplicationManager.getApplication().runReadAction(() ->
            selectedTextRef.set(editor.getSelectionModel().getSelectedText()));

        String selectedText = selectedTextRef.get();

        if (selectedText == null && file != null) {
            FileType fileType = file.getFileType();
            languageName = fileType.getName();
            selectedText = document.getText();
        }

        return new LanguageTextPair(languageName, selectedText);
    }
}
