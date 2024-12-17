package com.devoxx.genie.ui;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class EditorFileButtonManager {

    private final Project project;
    private final FileEditorManager fileEditorManager;
    private final JButton addFileBtn;

    public EditorFileButtonManager(Project project, JButton addFileBtn) {
        this.project = project;
        this.fileEditorManager = FileEditorManager.getInstance(project);
        this.addFileBtn = addFileBtn;
        handleFileOpenClose();
    }

    private void handleFileOpenClose() {
        project.getMessageBus().connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                @Override
                public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    if (addFileBtn == null) {
                        return;
                    }
                    addFileBtn.setEnabled(true);
                    addFileBtn.setToolTipText("Select file(s) for prompt context");
                }
            });
    }

    public Editor getSelectedTextEditor() {
        return fileEditorManager.getSelectedTextEditor();
    }

    public List<VirtualFile> getOpenFiles() {
        return Arrays.asList(fileEditorManager.getOpenFiles());
    }
}
