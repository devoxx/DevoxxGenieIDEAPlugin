package com.devoxx.genie.ui.processor;

import com.devoxx.genie.model.Constant;
import com.devoxx.genie.service.analyzer.DevoxxGenieGenerator;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Command processor that handles special commands entered in the input area
 * that don't need to be sent to the LLM service.
 */
@Slf4j
public class CommandProcessor {
    private CommandProcessor() {
        /* This utility class should not be instantiated */
    }

    /**
     * Process the command and return true if the command was processed and handled here,
     * false if the command should be processed normally (sent to the LLM service).
     *
     * @param project the project
     * @param promptText the prompt text
     * @return true if the command was handled, false otherwise
     */
    public static boolean processCommand(@NotNull Project project, @NotNull String promptText) {
        // Check if this is a command
        if (!promptText.startsWith(Constant.COMMAND_PREFIX)) {
            return false;
        }
        
        // Get the command without the prefix
        String command = promptText.substring(Constant.COMMAND_PREFIX.length()).trim();
        
        // Handle specific commands
        if (Constant.INIT_COMMAND.equals(command)) {
            handleInitCommand(project);
            return true;
        }
        
        // Not a special command, let it pass through to normal processing
        return false;
    }
    
    private static void handleInitCommand(@NotNull Project project) {
        log.info("Processing /init command to generate DEVOXXGENIE.md file");
        
        // Ensure the setting is temporarily enabled to generate the file

        boolean includeProjectTree = DevoxxGenieStateService.getInstance().getIncludeProjectTree();
        Integer treeDepth = DevoxxGenieStateService.getInstance().getProjectTreeDepth();
        
        // Use ProgressManager to run in a background task
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating DEVOXXGENIE.md", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Generating DEVOXXGENIE.md file...");
                    
                    // Create the generator and generate the file
                    DevoxxGenieGenerator devoxxGenieGenerator = 
                            new DevoxxGenieGenerator(project, includeProjectTree, treeDepth, indicator);
                    devoxxGenieGenerator.generate();
                } catch (Exception e) {
                    log.error("Error generating DEVOXXGENIE.md file", e);
                    
                    // Show error notification
                    ApplicationManager.getApplication().invokeLater(() ->
                        NotificationUtil.sendNotification(
                                project, 
                                "Error generating DEVOXXGENIE.md file: " + e.getMessage())
                    );
                }
            }
        });
    }
}
