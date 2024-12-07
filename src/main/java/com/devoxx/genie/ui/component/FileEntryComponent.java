package com.devoxx.genie.ui.component;

import com.devoxx.genie.model.request.SemanticFile;
import com.devoxx.genie.ui.listener.FileRemoveListener;
import com.devoxx.genie.ui.util.DevoxxGenieIconsUtil;
import com.devoxx.genie.ui.util.FileTypeIconUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import static com.devoxx.genie.action.AddSnippetAction.*;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.CloseSmalllIcon;
import static org.intellij.plugins.relaxNG.validation.RngSchemaValidator.findVirtualFile;

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

        Icon fileTypeIcon = FileTypeIconUtil.getFileTypeIcon(virtualFile);
        JButton fileNameButton = new JButton(virtualFile.getName(), fileTypeIcon);

        JButton fileNameBtn = createButton(fileNameButton);
        fileNameBtn.addActionListener(e -> openFileWithSelectedCode(project, virtualFile));
        add(fileNameBtn);

        if (fileRemoveListener != null) {
            JButton removeBtn = createButton(new JHoverButton(CloseSmalllIcon, true));
            removeBtn.addActionListener(e -> fileRemoveListener.onFileRemoved(virtualFile));
            add(removeBtn);
        }
    }

    public FileEntryComponent(Project project, SemanticFile semanticFile) {
        this.virtualFile = findVirtualFile(semanticFile.filePath());

        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        Icon fileTypeIcon = virtualFile != null ?
                FileTypeIconUtil.getFileTypeIcon(virtualFile) :
                DevoxxGenieIconsUtil.CodeSnippetIcon;

        JButton fileNameButton = new JButton(
                extractFileName(semanticFile.filePath()) + " (relevance score " + String.format("%2.2f", semanticFile.score() * 100) + "%)", fileTypeIcon);

        JButton fileNameBtn = createButton(fileNameButton);
        if (virtualFile != null) {
            fileNameBtn.addActionListener(e -> openFileInEditor(project, virtualFile));
        }
        add(fileNameBtn);
    }

    private VirtualFile findVirtualFile(String filePath) {
        return VirtualFileManager.getInstance().findFileByUrl("file://" + filePath);
    }

    private @NotNull String extractFileName(String filePath) {
        return new File(filePath).getName();
    }

    private void openFileInEditor(Project project, VirtualFile file) {
        ApplicationManager.getApplication().invokeLater(() -> {
            FileEditorManager.getInstance(project).openFile(file, true);
        });
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
