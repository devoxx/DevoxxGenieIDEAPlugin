package com.devoxx.genie.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class EditorFileButtonManager {

    private final FileEditorManager fileEditorManager;
    private final JButton addFileBtn;

    public EditorFileButtonManager(Project project, JButton addFileBtn) {
        this.fileEditorManager = FileEditorManager.getInstance(project);
        this.addFileBtn = addFileBtn;
        handleFileOpenClose();
    }

    private void handleFileOpenClose() {
        if (fileEditorManager.getSelectedFiles().length == 0) {
            addFileBtn.setEnabled(false);
            addFileBtn.setToolTipText("No files open in the editor");
        }

        ApplicationManager.getApplication().getMessageBus().connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                @Override
                public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    addFileBtn.setEnabled(true);
                    addFileBtn.setToolTipText("Select file(s) for prompt context");
                }

                @Override
                public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    if (fileEditorManager.getSelectedFiles().length == 0) {
                        addFileBtn.setEnabled(false);
                        addFileBtn.setToolTipText("No files open in the editor");
                    }
                }
            });
    }

    public Editor getSelectedTextEditor() {
        return fileEditorManager.getSelectedTextEditor();
    }
}
