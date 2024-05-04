package com.devoxx.genie.ui.component;

import com.devoxx.genie.ui.listener.FileRemoveListener;
import com.devoxx.genie.ui.util.FileTypeIconUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
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
     * @param fontToUse          the font to use
     */
    public FileEntryComponent(Project project,
                              VirtualFile virtualFile,
                              FileRemoveListener fileRemoveListener,
                              Font fontToUse) {
        this.virtualFile = virtualFile;

        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        Icon fileTypeIcon = FileTypeIconUtil.getFileTypeIcon(project, virtualFile);
        JButton fileNameButton = new JButton(virtualFile.getName(), fileTypeIcon);
        if (fontToUse != null) {
            fileNameButton.setFont(fontToUse);
        }
        JButton fileNameBtn = createButton(fileNameButton);
        fileNameBtn.addActionListener(e -> {
            VirtualFile originalFile = virtualFile.getUserData(ORIGINAL_FILE_KEY);
            if (originalFile != null) {
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                fileEditorManager.openFile(originalFile, true);
                Editor editor = fileEditorManager.getSelectedTextEditor();
                if (editor != null) {
                    String selectedText = virtualFile.getUserData(SELECTED_TEXT_KEY);
                    Integer selectionStart = virtualFile.getUserData(SELECTION_START_KEY);
                    Integer selectionEnd = virtualFile.getUserData(SELECTION_END_KEY);
                    if (selectedText != null && selectionStart != null && selectionEnd != null) {
                        editor.getSelectionModel().setSelection(selectionStart, selectionEnd);
                    }
                }
            } else {
                FileEditorManager.getInstance(project).openFile(virtualFile, true);
            }
        });
        add(fileNameBtn);

        if (fileRemoveListener != null) {
            JButton removeBtn = createButton(new JHoverButton(CloseSmalllIcon, true));
            removeBtn.addActionListener(e -> fileRemoveListener.onFileRemoved(virtualFile));
            add(removeBtn);
        }
    }

    private @NotNull JButton createButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(JBUI.emptyInsets());
        return button;
    }
}
