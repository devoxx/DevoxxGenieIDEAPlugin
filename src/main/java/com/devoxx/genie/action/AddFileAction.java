package com.devoxx.genie.action;

import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.devoxx.genie.ui.util.WindowPluginUtil.ensureToolWindowVisible;

public class AddFileAction extends DumbAwareAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        ensureToolWindowVisible(project);

        FileListManager fileListManager = FileListManager.getInstance();
        VirtualFile selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (selectedFile != null && !selectedFile.isDirectory() && !fileListManager.contains(selectedFile)) {
            fileListManager.addFile(selectedFile);
        } else if (selectedFile != null && selectedFile.isDirectory()) {
            NotificationUtil.sendNotification(project, "Cannot add directories. Please select a file.");
        } else {
            NotificationUtil.sendNotification(project, "File already added or no file selected");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        e.getPresentation().setEnabledAndVisible(file != null && !file.isDirectory());
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
