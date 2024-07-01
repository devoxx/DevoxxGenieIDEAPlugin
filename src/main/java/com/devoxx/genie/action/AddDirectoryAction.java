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

public class AddDirectoryAction extends DumbAwareAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        ensureToolWindowVisible(project);

        VirtualFile selectedDir = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (selectedDir != null && selectedDir.isDirectory()) {
            addDirectoryRecursively(project, selectedDir);
        } else {
            NotificationUtil.sendNotification(project, "Please select a directory");
        }
    }

    private void addDirectoryRecursively(Project project, @NotNull VirtualFile directory) {
        FileListManager fileListManager = FileListManager.getInstance();
        List<VirtualFile> filesToAdd = new ArrayList<>();

        VirtualFile[] children = directory.getChildren();
        for (VirtualFile child : children) {
            if (child.isDirectory()) {
                addDirectoryRecursively(project, child);
            } else if (!fileListManager.contains(child)) {
                filesToAdd.add(child);
            }
        }

        if (!filesToAdd.isEmpty()) {
            fileListManager.addFiles(filesToAdd);
            NotificationUtil.sendNotification(project, "Added " + filesToAdd.size() + " files from directory: " + directory.getName());
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        e.getPresentation().setEnabledAndVisible(file != null && file.isDirectory());
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
