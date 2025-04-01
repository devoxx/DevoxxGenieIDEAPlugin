package com.devoxx.genie.service.analyzer;

import com.devoxx.genie.service.generator.content.ContentGenerator;
import com.devoxx.genie.service.generator.file.FileManager;
import com.devoxx.genie.service.generator.tree.ProjectTreeGenerator;
import com.devoxx.genie.service.projectscanner.FileScanner;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Main generator for DEVOXXGENIE.md file.
 * Orchestrates the content generation and file operations.
 */
@Slf4j
public class DevoxxGenieGenerator {

    public static final String DEVOXX_GENIE_MD = "DEVOXXGENIE.md";

    private final Project project;
    private final VirtualFile baseDir;
    private final Boolean includeTree;
    private final ProgressIndicator indicator;
    
    // Components
    private final FileScanner fileScanner;
    private final ContentGenerator contentGenerator;
    private final ProjectTreeGenerator treeGenerator;
    private final FileManager fileManager;

    /**
     * Initializes the DevoxxGenieGenerator with all required dependencies.
     * 
     * @param project The IntelliJ project
     * @param includeTree Whether to include project tree
     * @param treeDepth The depth of the project tree to include
     * @param indicator The progress indicator
     */
    public DevoxxGenieGenerator(@NotNull Project project,
                                boolean includeTree,
                                int treeDepth,
                                ProgressIndicator indicator) {
        this.project = project;
        this.includeTree = includeTree;
        this.baseDir = project.getBaseDir();
        this.indicator = indicator;
        
        // Initialize components
        this.fileScanner = new FileScanner();
        this.contentGenerator = new ContentGenerator();
        this.treeGenerator = new ProjectTreeGenerator(fileScanner);
        this.fileManager = new FileManager();
    }

    /**
     * Generates the DEVOXXGENIE.md file.
     * This method is executed within the Task.Backgroundable from the caller,
     * so it should avoid blocking the EDT.
     */
    public void generate() {
        indicator.setText("Scanning project structure...");
        indicator.setIndeterminate(true);

        try {
            // Initialize FileScanner's gitignore parser in a read action
            com.intellij.openapi.application.ReadAction.run(() -> 
                fileScanner.initGitignoreParser(project, baseDir)
            );

            // Step 1: Analyze the project and create prompt - computeInNonDispatchThread to avoid EDT blocking
            indicator.setText("Analyzing project structure...");
            Map<String, Object> projectInfo = com.intellij.openapi.application.ReadAction.nonBlocking(() -> {
                ProjectAnalyzer scanner = new ProjectAnalyzer(project, baseDir);
                return scanner.scanProject();
            }).executeSynchronously();
            
            // Step 2: Generate content - this can be done directly since it doesn't use IntelliJ APIs
            indicator.setText("Generating DEVOXXGENIE.md content...");
            String prompt = contentGenerator.createPromptFromProjectInfo(projectInfo);
            String generatedContent = contentGenerator.generateContent(prompt);
            
            // Step 3: Write the file - must be done in a write action
            indicator.setText("Writing DEVOXXGENIE.md file...");
            com.intellij.openapi.application.WriteAction.runAndWait(() -> {
                try {
                    fileManager.writeFile(baseDir, DEVOXX_GENIE_MD, generatedContent);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            // Step 4: Handle project tree generation if needed - also requires write action
            if (Boolean.TRUE.equals(includeTree)) {
                indicator.setText("Generating project tree...");
                com.intellij.openapi.application.WriteAction.runAndWait(() -> {
                    try {
                        treeGenerator.appendProjectTree(baseDir, DEVOXX_GENIE_MD);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            
            // Step 5: Show success notification on EDT
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                NotificationUtil.sendNotification(project, "DEVOXXGENIE.md generated successfully in " 
                        + baseDir.getPath());
            });
        } catch (Exception e) {
            log.error("Error generating DEVOXXGENIE.md", e);
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                NotificationUtil.sendNotification(project, "Error generating DEVOXXGENIE.md: " + e.getMessage());
            });
        }
    }

    /**
     * Scans the project for information.
     */
    private Map<String, Object> scanProject() {
        ProjectAnalyzer scanner = new ProjectAnalyzer(project, baseDir);
        indicator.setText("Analyzing project structure...");
        return ReadAction.compute(scanner::scanProject);
    }
}
