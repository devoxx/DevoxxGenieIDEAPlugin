package com.devoxx.genie.ui.settings.copyproject;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CopyProjectSettingsConfigurable implements Configurable {

    private CopyProjectSettingsComponent copyProjectSettingsComponent;

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
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        return !copyProjectSettingsComponent.getExcludedDirectories().equals(settings.getExcludedDirectories()) ||
               !copyProjectSettingsComponent.getIncludedFileExtensions().equals(settings.getIncludedFileExtensions()) ||
                copyProjectSettingsComponent.getExcludeJavadoc() != settings.getExcludeJavaDoc();
    }

    @Override
    public void apply() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        settings.setExcludedDirectories(copyProjectSettingsComponent.getExcludedDirectories());
        settings.setIncludedFileExtensions(copyProjectSettingsComponent.getIncludedFileExtensions());
        settings.setExcludeJavaDoc(copyProjectSettingsComponent.getExcludeJavadoc());
    }

    @Override
    public void reset() {
    }

    @Override
    public void disposeUIResources() {
        copyProjectSettingsComponent = null;
    }
}
