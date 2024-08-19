package com.devoxx.genie.action;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.TokenCalculationService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class CalcTokensForDirectoryAction extends DumbAwareAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile selectedDir = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null || selectedDir == null || !selectedDir.isDirectory()) {
            return;
        }

        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        ModelProvider selectedProvider = ModelProvider.fromString(stateService.getSelectedProvider(project.getLocationHash()));

        int maxTokens = stateService.getDefaultWindowContext();

        new TokenCalculationService()
            .calculateTokensAndCost(project, selectedDir, maxTokens, selectedProvider, null, false);
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
