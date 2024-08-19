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

import java.util.ArrayList;
import java.util.List;

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
        VirtualFile[] selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);

        if (selectedFiles != null && selectedFiles.length > 0) {
            List<VirtualFile> filesToAdd = new ArrayList<>();
            for (VirtualFile file : selectedFiles) {
                if (!file.isDirectory() && !fileListManager.contains(file)) {
                    filesToAdd.add(file);
                }
            }

            if (!filesToAdd.isEmpty()) {
                fileListManager.addFiles(filesToAdd);
                NotificationUtil.sendNotification(project, "Added " + filesToAdd.size() + " file(s) to prompt context");
            } else {
                NotificationUtil.sendNotification(project, "No new files to add or only directories selected");
            }
        } else {
            NotificationUtil.sendNotification(project, "No files selected");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        e.getPresentation().setEnabledAndVisible(files != null && files.length > 0);
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
