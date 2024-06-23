package com.devoxx.genie.ui.component;

import com.devoxx.genie.ui.listener.FileRemoveListener;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.action.AddSnippetAction.*;
import static com.devoxx.genie.ui.util.DevoxxGenieIcons.CloseSmalllIcon;

/**
 * Class uses to display a file entry in the list of files with label and remove button.
 */
@Getter
public class FileEntryComponent extends JPanel {

    private final VirtualFile virtualFile;

    /**
     * File entry component
     *
     * @param project            the project
     * @param virtualFile        the virtual file
     * @param fileRemoveListener the file remove listener
     */
    public FileEntryComponent(Project project,
                              VirtualFile virtualFile,
                              FileRemoveListener fileRemoveListener) {
        this.virtualFile = virtualFile;

        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        // Icon fileTypeIcon = FileTypeIconUtil.getFileTypeIcon(project, virtualFile);
        JButton fileNameButton = new JButton(virtualFile.getName());

        JButton fileNameBtn = createButton(fileNameButton);
        fileNameBtn.addActionListener(e -> openFileWithSelectedCode(project, virtualFile));
        add(fileNameBtn);

        if (fileRemoveListener != null) {
            JButton removeBtn = createButton(new JHoverButton(CloseSmalllIcon, true));
            removeBtn.addActionListener(e -> fileRemoveListener.onFileRemoved(virtualFile));
            add(removeBtn);
        }
    }

    /**
     * Open the file with selected code and highlight the selected text in the editor when applicable.
     *
     * @param project     the project
     * @param virtualFile the virtual file
     */
    private static void openFileWithSelectedCode(Project project, @NotNull VirtualFile virtualFile) {
        VirtualFile originalFile = virtualFile.getUserData(ORIGINAL_FILE_KEY);
        if (originalFile != null) {
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            fileEditorManager.openFile(originalFile, true);
            Editor editor = fileEditorManager.getSelectedTextEditor();
            if (editor != null) {
                highlightSelectedText(virtualFile, editor);
            }
        } else {
            FileEditorManager.getInstance(project).openFile(virtualFile, true);
        }
    }

    /**
     * Highlight the selected text in the editor.
     *
     * @param virtualFile the virtual file
     * @param editor      the editor
     */
    private static void highlightSelectedText(@NotNull VirtualFile virtualFile, Editor editor) {
        String selectedText = virtualFile.getUserData(SELECTED_TEXT_KEY);
        Integer selectionStart = virtualFile.getUserData(SELECTION_START_KEY);
        Integer selectionEnd = virtualFile.getUserData(SELECTION_END_KEY);
        if (selectedText != null && selectionStart != null && selectionEnd != null) {
            editor.getSelectionModel().setSelection(selectionStart, selectionEnd);
        }
    }

    /**
     * Create a button.
     *
     * @param button the button
     * @return the button
     */
    @Contract("_ -> param1")
    private @NotNull JButton createButton(@NotNull JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(JBUI.emptyInsets());
        return button;
    }
}
