package com.devoxx.genie.action;

import com.devoxx.genie.service.FileListManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.devoxx.genie.ui.util.WindowPluginUtil.ensureToolWindowVisible;

public class AddSnippetAction extends DumbAwareAction {

    public static final String CODE_SNIPPET = "codeSnippet";
    public static final Key<VirtualFile> ORIGINAL_FILE_KEY = Key.create("ORIGINAL_FILE");
    public static final Key<String> SELECTED_TEXT_KEY = Key.create("SELECTED_TEXT");
    public static final Key<Integer> SELECTION_START_KEY = Key.create("SELECTION_START");
    public static final Key<Integer> SELECTION_END_KEY = Key.create("SELECTION_END");

    // We use an unknown file type to represent code snippets
    private final FileType fileType =
        FileTypeManager.getInstance().getFileTypeByExtension(CODE_SNIPPET);

    /**
     * Add a snippet to the tool window.
     *
     * @param e the action event
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (editor != null && selectedFile != null) {

            ensureToolWindowVisible(e.getProject());

            SelectionModel selectionModel = editor.getSelectionModel();
            String selectedText = selectionModel.getSelectedText();
            if (selectedText != null) {
                createAndAddVirtualFile(e.getProject(), selectedFile, selectionModel, selectedText);
            } else {
                // No text selected, add complete file
                addSelectedFile(e.getProject(), selectedFile);
            }
        }
    }

    /**
     * Add the selected file to the file list manager.
     *
     * @param selectedFile the selected file
     */
    private static void addSelectedFile(Project project, VirtualFile selectedFile) {
        FileListManager fileListManager = FileListManager.getInstance();
        if (fileListManager.contains(project, selectedFile)) {
            return;
        }
        fileListManager.addFile(project, selectedFile);
    }

    /**
     * Create a virtual file and add it to the file list manager.
     *
     * @param project
     * @param originalFile   the original file
     * @param selectionModel the selection model
     * @param selectedText   the selected text
     */
    private void createAndAddVirtualFile(@Nullable Project project, @NotNull VirtualFile originalFile,
                                         @NotNull SelectionModel selectionModel,
                                         String selectedText) {
        LightVirtualFile virtualFile = new LightVirtualFile(originalFile.getName(), selectedText);
        virtualFile.setFileType(fileType);
        virtualFile.putUserData(ORIGINAL_FILE_KEY, originalFile);
        virtualFile.putUserData(SELECTED_TEXT_KEY, selectedText);
        virtualFile.putUserData(SELECTION_START_KEY, selectionModel.getSelectionStart());
        virtualFile.putUserData(SELECTION_END_KEY, selectionModel.getSelectionEnd());
        FileListManager.getInstance().addFile(project, virtualFile);
    }


    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }
}
