package com.devoxx.genie.ui.settings.copyproject;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
<<<<<<< HEAD
=======
import java.util.List;
>>>>>>> master

public class CopyProjectSettingsConfigurable implements Configurable {

    private CopyProjectSettingsComponent copyProjectSettingsComponent;
    private final DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Copy Project";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        copyProjectSettingsComponent = new CopyProjectSettingsComponent();
        return copyProjectSettingsComponent.createPanel();
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
        stateService.setExcludedDirectories(copyProjectSettingsComponent.getExcludedDirectories());
        stateService.setExcludedFiles(copyProjectSettingsComponent.getExcludedFiles());  // Save excluded files
        stateService.setIncludedFileExtensions(copyProjectSettingsComponent.getIncludedFileExtensions());
        stateService.setExcludeJavaDoc(copyProjectSettingsComponent.getExcludeJavadoc());
        stateService.setUseGitIgnore(copyProjectSettingsComponent.getUseGitIgnore());
    }

    @Override
<<<<<<< HEAD
=======
    public void reset() {
    }

    @Override
>>>>>>> master
    public void disposeUIResources() {
        copyProjectSettingsComponent = null;
    }
}
