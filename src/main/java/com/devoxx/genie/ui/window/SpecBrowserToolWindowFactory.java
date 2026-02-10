package com.devoxx.genie.ui.window;

import com.devoxx.genie.service.spec.SpecService;
import com.devoxx.genie.ui.panel.spec.SpecBrowserPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the Spec Browser tool window.
 * Available when the SDD feature is enabled and a backlog directory exists.
 */
public class SpecBrowserToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ApplicationManager.getApplication().invokeLater(() -> {
            SpecBrowserPanel specBrowserPanel = new SpecBrowserPanel(project);
            Content content = ContentFactory.getInstance().createContent(
                    specBrowserPanel,
                    "Task Specs",
                    false
            );
            toolWindow.getContentManager().addContent(content);
        });
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getSpecBrowserEnabled())
                || SpecService.getInstance(project).hasSpecDirectory();
    }

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setStripeTitle("DevoxxGenie Specs");
        toolWindow.setToHideOnEmptyContent(false);
    }
}
