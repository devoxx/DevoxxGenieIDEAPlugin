package com.devoxx.genie.ui.settings.gitmerge;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GitMergeSettingsConfigurable implements Configurable {

    private final GitMergeSettingsComponent diffSettingsComponent;

    public GitMergeSettingsConfigurable() {
        diffSettingsComponent = new GitMergeSettingsComponent();
    }

    /**
     * Get the display name
     * @return the display name
     */
    @Nls
    @Override
    public String getDisplayName() {
        return "LLM Git Merge";
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
        GitDiffMode currentMode = determineCurrentMode(stateService);
        return currentMode != diffSettingsComponent.getGitDiffModeComboBox().getSelectedItem();
    }

    @Override
    public void apply() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        GitDiffMode selectedMode = (GitDiffMode) diffSettingsComponent.getGitDiffModeComboBox().getSelectedItem();

        stateService.setUseSimpleDiff(selectedMode == GitDiffMode.SIMPLE_DIFF);
    }

    @Override
    public void reset() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        GitDiffMode currentMode = determineCurrentMode(stateService);
        diffSettingsComponent.getGitDiffModeComboBox().setSelectedItem(currentMode);
    }

    private GitDiffMode determineCurrentMode(@NotNull DevoxxGenieStateService stateService) {
        if (stateService.getUseSimpleDiff()) {
            return GitDiffMode.SIMPLE_DIFF;
        }
        return GitDiffMode.DISABLED;
    }
}
