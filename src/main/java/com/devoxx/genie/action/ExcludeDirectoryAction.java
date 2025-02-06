package com.devoxx.genie.action;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ExcludeDirectoryAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Get the current project
        Project project = e.getProject();
        if (project == null) {
            Messages.showErrorDialog("No project found.", "Error");
            return;
        }

        VirtualFile selectedDir = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (selectedDir == null || !selectedDir.isDirectory()) {
            NotificationUtil.sendNotification(project, "Please select a directory to exclude");
            return;
        }

        // Get the absolute path of the directory
        String directoryPath = selectedDir.getPath();

        // Access the state service
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        // Get current excluded directories
        List<String> excludedDirectories = stateService.getExcludedDirectories();

        // Check if already excluded
        if (excludedDirectories.contains(directoryPath)) {
            Messages.showInfoMessage(project, "Directory is already excluded.", "Information");
            return;
        }

        // Add to excluded directories
        excludedDirectories.add(directoryPath);
        stateService.setExcludedDirectories(excludedDirectories);

        // Optionally, refresh the settings UI if it's open
        // This can be achieved by notifying listeners or using a messaging system

        // Provide feedback to the user
        Messages.showInfoMessage(project, "Directory excluded successfully.", "Success");
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
