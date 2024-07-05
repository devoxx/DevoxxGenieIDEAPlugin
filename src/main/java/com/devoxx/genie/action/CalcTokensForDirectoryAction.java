package com.devoxx.genie.action;

import com.devoxx.genie.service.ProjectContentService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.util.WindowContextFormatterUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class CalcTokensForDirectoryAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile selectedDir = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null || selectedDir == null || !selectedDir.isDirectory()) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Calculating Tokens", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ProjectContentService.getInstance()
                    .getDirectoryContentAndTokens(project, selectedDir, Integer.MAX_VALUE, true)
                    .thenAccept(result -> {
                        String message = String.format("Directory '%s' contains approximately %s tokens",
                            selectedDir.getName(),
                            WindowContextFormatterUtil.format(result.getTokenCount()));
                        NotificationUtil.sendNotification(project, message);
                    });
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        e.getPresentation().setEnabledAndVisible(file != null && file.isDirectory());
    }
}
