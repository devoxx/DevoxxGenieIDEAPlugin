package com.devoxx.genie.service.analyzer;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface ProjectAnalyzerExtension {
    ExtensionPointName<ProjectAnalyzerExtension> EP_NAME =
            ExtensionPointName.create("com.devoxx.genie.projectScannerExtension");

    void enhanceProjectInfo(@NotNull Project project, @NotNull Map<String, Object> projectInfo);
}