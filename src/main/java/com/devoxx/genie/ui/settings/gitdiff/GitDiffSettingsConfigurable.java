package com.devoxx.genie.ui.settings.gitdiff;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GitDiffSettingsConfigurable implements Configurable {

    private final GitDiffSettingsComponent diffSettingsComponent;

    public GitDiffSettingsConfigurable() {
        diffSettingsComponent = new GitDiffSettingsComponent();
    }

    /**
     * Get the display name
     * @return the display name
     */
    @Nls
    @Override
    public String getDisplayName() {
        return "LLM Git Diff";
    }

    /**
     * Get the Prompt Settings component
     *
     * @return the component
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        return diffSettingsComponent.createPanel();
    }

    @Override
    public boolean isModified() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        return diffSettingsComponent.getEnableGitDiffCheckBox().isSelected() != stateService.getGitDiffEnabled();
    }

    @Override
    public void apply() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        boolean oldValue = stateService.getGitDiffEnabled();
        boolean newValue = diffSettingsComponent.getEnableGitDiffCheckBox().isSelected();

        stateService.setGitDiffEnabled(diffSettingsComponent.getEnableGitDiffCheckBox().isSelected());

        if (oldValue != newValue) {
            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(AppTopics.GITDIFF_STATE_TOPIC)
                    .onGitDiffStateChange(newValue);
        }
    }

    @Override
    public void reset() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        diffSettingsComponent.getEnableGitDiffCheckBox().setSelected(stateService.getGitDiffEnabled());
    }
}
