package com.devoxx.genie.action;

import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

public class AddFileAction extends AnAction {

    /**
     * Add file to the window context.
     * @param e the action event
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        ensureToolWindowVisible(project);
        FileListManager fileListManager = FileListManager.getInstance();
        VirtualFile selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (selectedFile != null && !fileListManager.contains(selectedFile)) {
            fileListManager.addFile(selectedFile);
        } else {
            NotificationUtil.sendNotification(project, "File already added");
        }
    }

    /**
     * Open the tool window if it is not visible.
     * @param project the project
     */
    public void ensureToolWindowVisible(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("DevoxxGenie");

        if (toolWindow != null && !toolWindow.isVisible()) {
            toolWindow.show(null);
        }
    }
}
