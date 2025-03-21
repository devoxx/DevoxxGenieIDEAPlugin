package com.devoxx.genie.service.prompt;

import com.devoxx.genie.service.prompt.error.ExecutionException;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.command.PromptCommandProcessor;
import com.devoxx.genie.service.prompt.strategy.PromptExecutionStrategy;
import com.devoxx.genie.service.prompt.strategy.PromptExecutionStrategyFactory;
import com.devoxx.genie.service.prompt.threading.PromptTaskTracker;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CancellationException;

/**
 * Unified service for executing prompts with various strategies.
 */
@Slf4j
public class PromptExecutionService {

    private final PromptCommandProcessor commandProcessor;
    private final PromptExecutionStrategyFactory strategyFactory;
    private final PromptTaskTracker taskTracker;
    private final Project project;

    public static PromptExecutionService getInstance(@NotNull Project project) {
        return project.getService(PromptExecutionService.class);
    }

    public PromptExecutionService(Project project) {
        this.project = project;
        this.commandProcessor = PromptCommandProcessor.getInstance();
        this.strategyFactory = PromptExecutionStrategyFactory.getInstance();
        this.taskTracker = PromptTaskTracker.getInstance();
    }

    /**
     * Execute a prompt using the appropriate strategy.
     *
     * @param chatMessageContext The context for the prompt
     * @param promptOutputPanel The panel for displaying output
     * @param enableButtons Callback to run when execution completes
     */
    public void executePrompt(@NotNull ChatMessageContext chatMessageContext,
                             @NotNull PromptOutputPanel promptOutputPanel,
                             @NotNull Runnable enableButtons) {
                             
        Project project = chatMessageContext.getProject();
        
        // Cancel any running executions for this project
        if (taskTracker.cancelAllTasks(project) > 0) {
            log.debug("Cancelled all existing tasks for project");
            return;
        }

        // Process commands
        Optional<String> processedPrompt = commandProcessor.processCommands(chatMessageContext, promptOutputPanel);
        if (processedPrompt.isEmpty()) {
            // Command processing indicated we should stop
            enableButtons.run();
            return;
        }

        // Run in background task
        new Task.Backgroundable(project, "Working...", true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                // Create appropriate strategy
                PromptExecutionStrategy strategy = strategyFactory.createStrategy(chatMessageContext);
                
                // Execute the prompt
                strategy.execute(chatMessageContext, promptOutputPanel)
                    .whenComplete((result, error) -> 
                        ApplicationManager.getApplication().invokeLater(() -> {
                            cleanupAfterExecution(project, enableButtons);
                        }));
            }

            @Override
            public void onCancel() {
                super.onCancel();
                log.info("Prompt execution was cancelled.");
                taskTracker.cancelAllTasks(project);
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                super.onThrowable(error);
                if (!(error instanceof CancellationException)) {
                    // Create a specific execution exception and handle it with our standardized handler
                    ExecutionException executionError = new ExecutionException(
                        "Error occurred during prompt execution", error, 
                        com.devoxx.genie.service.prompt.error.PromptException.ErrorSeverity.ERROR, true);
                    PromptErrorHandler.handleException(project, executionError, chatMessageContext);
                }
                cleanupAfterExecution(project, enableButtons);
            }
        }.queue();
    }

    /**
     * Stop the execution for a specific project.
     *
     * @param project The project to stop execution for
     */
    public void stopExecution(Project project) {
        taskTracker.cancelAllTasks(project);
    }

    /**
     * Clean up after execution completes.
     *
     * @param project The project that finished execution
     * @param enableButtons Callback to enable UI elements
     */
    private void cleanupAfterExecution(Project project, @NotNull Runnable enableButtons) {
        enableButtons.run();
        FileListManager.getInstance().storeAddedFiles(project);
    }
}
