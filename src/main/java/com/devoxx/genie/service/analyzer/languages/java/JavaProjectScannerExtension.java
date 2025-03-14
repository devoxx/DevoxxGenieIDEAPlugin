package com.devoxx.genie.service.analyzer.languages.java;

import com.devoxx.genie.service.analyzer.ProjectAnalyzerExtension;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.LanguageLevel;

import java.util.Map;

// Java Extension Implementation
public class JavaProjectScannerExtension implements ProjectAnalyzerExtension {

    @Override
    public void enhanceProjectInfo(@NotNull Project project, @NotNull Map<String, Object> projectInfo) {
        // Check if Java module is available
        if (!isJavaAvailable()) return;

        // Add Java-specific project info
        try {
            // Detect Java version
            Map<String, Object> languages = (Map<String, Object>) projectInfo.get("languages");
            if (languages != null) {
                // TODO Get Java version from project
                // LanguageLevel languageLevel = LanguageLevelProjectExtension.getInstance(project).getLanguageLevel();
                // languages.put("javaVersion", languageLevel.toString());
            }

            // Detect Java build tools and frameworks more precisely
            try {
                // Check if Maven plugin is available
                Class<?> mavenManagerClass = Class.forName("org.jetbrains.idea.maven.project.MavenProjectsManager");
                Object mavenManager = mavenManagerClass.getMethod("getInstance", Project.class).invoke(null, project);
                boolean hasMavenProjects = (boolean) mavenManagerClass.getMethod("hasProjects").invoke(mavenManager);
                
                if (hasMavenProjects) {
                    enhanceMavenInfo(projectInfo);
                }
            } catch (Exception ex) {
                // Maven plugin not available or error accessing it
                // Just continue without Maven-specific enhancements
            }

            // TODO More Java-specific info like frameworks, libraries, etc.

        } catch (Exception e) {
            // Gracefully handle any errors if Java APIs aren't available
        }
    }

    private boolean isJavaAvailable() {
        try {
            Class.forName("com.intellij.psi.JavaPsiFacade");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private void enhanceMavenInfo(@NotNull Map<String, Object> projectInfo) {
        // Simple implementation - just note that it's Maven in the build system
        Map<String, Object> buildSystem = (Map<String, Object>) projectInfo.get("buildSystem");
        if (buildSystem != null) {
            buildSystem.put("mavenDetails", "Maven project detected");

            // TODO Add dependency resolution and other Maven-specific info
        }
    }
}