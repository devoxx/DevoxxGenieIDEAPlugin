package com.devoxx.genie.ui.settings.copyproject;

import com.devoxx.genie.ui.settings.ProjectScanSettingsService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CopyProjectSettingsConfigurable implements Configurable {

    private CopyProjectSettingsComponent copyProjectSettingsComponent;
    private final Project project;
    private final ProjectScanSettingsService stateService;

    public CopyProjectSettingsConfigurable(Project project) {
        this.project = project;
        this.stateService = ProjectScanSettingsService.getInstance(project);
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Copy Project";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        copyProjectSettingsComponent = new CopyProjectSettingsComponent(project);
        return copyProjectSettingsComponent.createPanelWithHelp();
    }

    @Override
    public boolean isModified() {
        return !copyProjectSettingsComponent.getExcludedDirectories().equals(stateService.getExcludedDirectories()) ||
            !copyProjectSettingsComponent.getExcludedFiles().equals(stateService.getExcludedFiles()) ||  // Add check for excluded files
            !copyProjectSettingsComponent.getIncludedFileExtensions().equals(stateService.getIncludedFileExtensions()) ||
            copyProjectSettingsComponent.getExcludeJavadoc() != stateService.getExcludeJavaDoc() ||
            copyProjectSettingsComponent.getUseGitIgnore() != stateService.getUseGitIgnore();
    }

    @Override
    public void apply() {
        stateService.setExcludedDirectories(new java.util.ArrayList<>(copyProjectSettingsComponent.getExcludedDirectories()));
        stateService.setExcludedFiles(new java.util.ArrayList<>(copyProjectSettingsComponent.getExcludedFiles()));  // Save excluded files
        stateService.setIncludedFileExtensions(new java.util.ArrayList<>(copyProjectSettingsComponent.getIncludedFileExtensions()));
        stateService.setExcludeJavaDoc(copyProjectSettingsComponent.getExcludeJavadoc());
        stateService.setUseGitIgnore(copyProjectSettingsComponent.getUseGitIgnore());
    }

    @Override
    public void reset() {
        if (copyProjectSettingsComponent != null) {
            copyProjectSettingsComponent.reset(stateService);
        }
    }

    @Override
    public void disposeUIResources() {
        copyProjectSettingsComponent = null;
    }
}
