package com.devoxx.genie.action;

import com.devoxx.genie.service.FileListManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

public class AddSnippetAction extends AnAction {

    public static final String CODE_SNIPPET = "codeSnippet";
    public static final Key<VirtualFile> ORIGINAL_FILE_KEY = Key.create("ORIGINAL_FILE");
    public static final Key<String> SELECTED_TEXT_KEY = Key.create("SELECTED_TEXT");
    public static final Key<Integer> SELECTION_START_KEY = Key.create("SELECTION_START");
    public static final Key<Integer> SELECTION_END_KEY = Key.create("SELECTION_END");
    public static final String TOOL_WINDOW_ID = "DevoxxGenie";

    // We use an unknown file type to represent code snippets
    private final FileType fileType =
        FileTypeManager.getInstance().getFileTypeByExtension(CODE_SNIPPET);

    /**
     * Add a snippet to the tool window.
     * @param e the action event
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile originalFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (editor != null && originalFile != null) {
            SelectionModel selectionModel = editor.getSelectionModel();
            String selectedText = selectionModel.getSelectedText();
            if (selectedText != null) {
                ensureToolWindowIsVisible(e);
                createAndAddVirtualFile(originalFile, selectionModel, selectedText);
            }
        }
    }

    /**
     * Ensure that the tool window is visible.
     * @param e the action event
     */
    private void ensureToolWindowIsVisible(@NotNull AnActionEvent e) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(e.getProject()).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow != null && !toolWindow.isVisible()) {
            toolWindow.show();
        }
    }

    /**
     * Create a virtual file and add it to the file list manager.
     * @param originalFile the original file
     * @param selectionModel the selection model
     * @param selectedText the selected text
     */
    private void createAndAddVirtualFile(VirtualFile originalFile,
                                         SelectionModel selectionModel,
                                         String selectedText) {
        LightVirtualFile virtualFile = new LightVirtualFile(originalFile.getName(), selectedText);
        virtualFile.setFileType(fileType);
        virtualFile.putUserData(ORIGINAL_FILE_KEY, originalFile);
        virtualFile.putUserData(SELECTED_TEXT_KEY, selectedText);
        virtualFile.putUserData(SELECTION_START_KEY, selectionModel.getSelectionStart());
        virtualFile.putUserData(SELECTION_END_KEY, selectionModel.getSelectionEnd());
        FileListManager.getInstance().addFile(virtualFile);
    }
}
