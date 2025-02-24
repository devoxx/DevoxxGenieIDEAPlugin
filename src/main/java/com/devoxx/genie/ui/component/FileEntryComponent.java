package com.devoxx.genie.ui.component;

import com.devoxx.genie.model.request.SemanticFile;
import com.devoxx.genie.ui.listener.FileRemoveListener;
import com.devoxx.genie.ui.util.DevoxxGenieIconsUtil;
import com.devoxx.genie.ui.util.FileTypeIconUtil;
import com.devoxx.genie.util.FileUtil;
import com.devoxx.genie.util.ImageUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.Gray;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.devoxx.genie.action.AddSnippetAction.*;
import static com.devoxx.genie.util.ImageUtil.isImageFile;

/**
 * Class uses to display a file entry in the list of files with label and remove button.
 */
@Getter
public class FileEntryComponent extends JPanel {
    private static final Color PATH_COLOR = Gray._128;
    private static final Font MONO_FONT = new Font("JetBrains Mono", Font.PLAIN, 12);

    private final VirtualFile virtualFile;

    /**
     * File entry component
     */
    public FileEntryComponent(Project project, @NotNull VirtualFile file, FileRemoveListener removeListener) {
        this.virtualFile = file;
        setLayout(new BorderLayout());

        // Create main content panel
        Box contentPanel = Box.createHorizontalBox();

        boolean isCodeSnippet = false;
        String selectedText = file.getUserData(SELECTED_TEXT_KEY);
        JButton fileNameButton;
        if (selectedText != null && !selectedText.isEmpty()) {
            fileNameButton = new JButton(file.getName(), DevoxxGenieIconsUtil.CodeSnippetIcon);
            isCodeSnippet = true;
        } else {
            fileNameButton = new JButton(file.getName(), FileTypeIconUtil.getFileTypeIcon(file));
        }

        fileNameButton.setBorder(JBUI.Borders.empty());
        fileNameButton.addActionListener(e -> openFileWithSelectedCode(project, virtualFile));
        fileNameButton.setFont(MONO_FONT);
        contentPanel.add(fileNameButton);

        // Path label
        JLabel pathLabel = new JLabel();
        String fullPath = FileUtil.getRelativePath(project, file);

        if (!fullPath.equals(file.getName())) {
            pathLabel.setText(fullPath);
            pathLabel.setFont(MONO_FONT);
            pathLabel.setForeground(PATH_COLOR);
        }

        // Create tooltip with full path
        String tooltipText = String.format("<html><body style='width: 300px'><pre>%s</pre></body></html>",
                file.getPath().replace("<", "&lt;").replace(">", "&gt;"));

        if (isCodeSnippet) {
            // Add start and end line information if available
            Integer startLine = file.getUserData(SELECTION_START_LINE_KEY);
            Integer endLine = file.getUserData(SELECTION_END_LINE_KEY);
            if (startLine != null && endLine != null) {
                JLabel lineInfoLabel = new JLabel(String.format(" (%d-%d)", startLine + 1, endLine + 1));
                lineInfoLabel.setFont(MONO_FONT);
                lineInfoLabel.setForeground(PATH_COLOR);
                contentPanel.add(lineInfoLabel);
            }
        }

        contentPanel.add(Box.createHorizontalStrut(5));
        contentPanel.add(pathLabel);
        contentPanel.add(Box.createHorizontalGlue());

        // Create and add remove button
        JButton removeButton = new JButton("×");
        removeButton.setFont(new Font(removeButton.getFont().getName(), Font.PLAIN, 16));
        removeButton.setBorderPainted(false);
        removeButton.setContentAreaFilled(false);
        removeButton.setFocusPainted(false);
        removeButton.addActionListener(e -> removeListener.onFileRemoved(file));

        // Add components to main panel
        add(contentPanel, BorderLayout.CENTER);
        add(removeButton, BorderLayout.EAST);

        // Set tooltip for the entire component
        setToolTipText(tooltipText);

        // Set preferred size
        setPreferredSize(new Dimension(0, 25));
    }

    public FileEntryComponent(Project project, @NotNull SemanticFile semanticFile) {
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

        private VirtualFile findVirtualFile (String filePath){
            return VirtualFileManager.getInstance().findFileByUrl("file://" + filePath);
        }

        private @NotNull String extractFileName (String filePath){
            return new File(filePath).getName();
        }

        private void openFileInEditor (Project project, VirtualFile file){
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
        private void openFileWithSelectedCode(@NotNull Project project, @NotNull VirtualFile virtualFile) {
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    // Check if the file is an image
                    if (isImageFile(virtualFile)) {
                        String filePath = virtualFile.getCanonicalPath();
                        VirtualFile freshVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(new File(filePath).toPath());
                        if (freshVirtualFile != null && freshVirtualFile.exists()) {
                            FileEditorManagerEx.getInstance(project).openFile(freshVirtualFile, true, true);
                        }
                    } else {
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
                } catch (Exception e) {
                    Messages.showErrorDialog("Error opening file: " + e.getMessage(), "Error");
                }
            });
        }

        /**
         * Highlight the selected text in the editor.
         *
         * @param virtualFile the virtual file
         * @param editor      the editor
         */
        private static void highlightSelectedText(@NotNull VirtualFile virtualFile, Editor editor){
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
        private @NotNull JButton createButton (@NotNull JButton button){
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            button.setMargin(JBUI.emptyInsets());
            return button;
        }
    }
