package com.devoxx.genie.ui;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class EditorFileButtonManager {

    private final FileEditorManager fileEditorManager;
    @Getter
    private final JButton addFileBtn;

    public EditorFileButtonManager(Project project, JButton addFileBtn) {
        this.fileEditorManager = FileEditorManager.getInstance(project);
        this.addFileBtn = addFileBtn;
    }

    public Editor getSelectedTextEditor() {
        return fileEditorManager.getSelectedTextEditor();
    }

    public List<VirtualFile> getOpenFiles() {
        return Arrays.asList(fileEditorManager.getOpenFiles());
    }
}
