package com.devoxx.genie.ui.trace.view;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

/** The read-only editor tab that hosts an {@link AgentRunPanel} (TASK-283). */
final class AgentRunEditor extends UserDataHolderBase implements FileEditor {

    private final AgentRunFile file;
    private final AgentRunPanel panel;

    AgentRunEditor(@NotNull Project project, @NotNull AgentRunFile file) {
        this.file = file;
        this.panel = new AgentRunPanel(project, file.conversation(), file.run());
    }

    @Override
    public @NotNull JComponent getComponent() {
        return panel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return panel.focusComponent();
    }

    @Override
    public @NotNull String getName() {
        return "Agent Run";
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        // Nothing to restore: the view is rebuilt from the trace each time it opens.
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        // Read-only view: no properties change.
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        // Read-only view: no properties change.
    }

    @Override
    public void dispose() {
        // The panel holds no listeners or resources beyond Swing components.
    }
}
