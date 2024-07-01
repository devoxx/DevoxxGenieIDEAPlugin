package com.devoxx.genie.action;

import com.devoxx.genie.service.ProjectContentService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class AddDirectoryToClipBoardAction extends DumbAwareAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        VirtualFile selectedDir = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (selectedDir != null && selectedDir.isDirectory()) {
            addDirectoryContentToClipboard(project, selectedDir);
        } else {
            NotificationUtil.sendNotification(project, "Please select a directory");
        }
    }

    private void addDirectoryContentToClipboard(Project project, VirtualFile directory) {
        // Because we copy the content to the clipboard, we can set the limit to a high number
        ProjectContentService.getInstance().getDirectoryContent(project, directory, 1_000_000, false)
            .thenAccept(content ->
                NotificationUtil.sendNotification(project, "Directory content added to clipboard: " + directory.getName()));
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
