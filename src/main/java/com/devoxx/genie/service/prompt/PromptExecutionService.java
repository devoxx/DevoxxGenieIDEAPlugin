package com.devoxx.genie.service.prompt;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.cancellation.PromptCancellationService;
import com.devoxx.genie.service.prompt.command.PromptCommandProcessor;
import com.devoxx.genie.service.prompt.error.ExecutionException;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.strategy.PromptExecutionStrategy;
import com.devoxx.genie.service.prompt.strategy.PromptExecutionStrategyFactory;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CancellationException;

/**
 * Unified service for executing prompts with various strategies.
 * Manages execution flow, strategy selection, and command processing.
 */
@Slf4j
public class PromptExecutionService {

    private final PromptCommandProcessor commandProcessor;
    private final PromptExecutionStrategyFactory strategyFactory;
    private final PromptCancellationService cancellationService;
    private final Project project;

    public static PromptExecutionService getInstance(@NotNull Project project) {
        return project.getService(PromptExecutionService.class);
    }

    public PromptExecutionService(Project project) {
        this.project = project;
        this.commandProcessor = PromptCommandProcessor.getInstance();
        this.strategyFactory = PromptExecutionStrategyFactory.getInstance();
        this.cancellationService = PromptCancellationService.getInstance();
    }

    /**
     * Execute a prompt using the appropriate strategy.
     *
     * @param context The context for the prompt
     * @param panel The panel for displaying output
     * @param enableButtons Callback to run when execution completes
     */
    public void executePrompt(@NotNull ChatMessageContext context,
                             @NotNull PromptOutputPanel panel,
                             @NotNull Runnable enableButtons) {
                             
        Project project = context.getProject();
        
        // Cancel any running executions for this project
        if (cancellationService.cancelAllExecutions(project) > 0) {
            log.debug("Cancelled all existing executions for project");
            enableButtons.run();
            return;
        }

        // Process commands
        Optional<String> processedPrompt = commandProcessor.processCommands(context, panel);
        if (processedPrompt.isEmpty()) {
            // Command processing indicated we should stop
            enableButtons.run();
            return;
        }

        // Start a background progress indicator
        ProgressManager.getInstance().run(
            new Task.Backgroundable(project, "Working...", true) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                }
                
                @Override
                public void onCancel() {
                    super.onCancel();
                    log.info("Prompt execution was cancelled by user.");
                    cancellationService.cancelAllExecutions(project);
                }
            }
        );

        // Create appropriate strategy
        PromptExecutionStrategy strategy = strategyFactory.createStrategy(context);
        
        // Register the strategy and panel with cancellation service
        cancellationService.registerExecution(project, context.getId(), strategy, panel);
        
        // Execute the prompt and handle completion
        PromptTask<PromptResult> task = strategy.execute(context, panel);
        
        // Store context with the task for cancellation handling
        task.putUserData(PromptTask.CONTEXT_KEY, context);
        
        task.whenComplete((result, error) -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (error != null) {
                    handleExecutionError(error, context);
                } else if (result != null) {
                    log.debug("Prompt execution completed with result: {}", result);
                }
                
                // Unregister from cancellation service upon completion
                cancellationService.unregisterExecution(project, context.getId());
                
                cleanupAfterExecution(project, enableButtons);
            });
        });
    }

    /**
     * Stop the execution for a specific project.
     *
     * @param project The project to stop execution for
     */
    public void stopExecution(Project project) {
        int count = cancellationService.cancelAllExecutions(project);
        log.debug("Cancelled {} executions for project {}", count, project.getName());
    }
    
    /**
     * Cancel a specific execution by context ID
     */
    public void cancelExecution(@NotNull String contextId) {
        cancellationService.cancelExecution(project, contextId);
        log.debug("Cancelled execution for context {}", contextId);
    }
    
    /**
     * Centralized method to handle cancellation of a prompt.
     * Ensures memory cleanup and UI updates in a consistent way.
     *
     * @param context The context of the prompt being cancelled
     * @param panel The UI panel to update
     */
    public void handleCancellation(@NotNull ChatMessageContext context, @NotNull PromptOutputPanel panel) {
        // Remove last user message from memory
        ChatMemoryManager.getInstance().removeLastUserMessage(context);
        
        // Update UI if needed
        panel.removeLastUserPrompt(context);
        
        log.debug("Handled cancellation for context {}", context.getId());
    }
    
    /**
     * Handle errors during execution.
     */
    private void handleExecutionError(Throwable error, ChatMessageContext context) {
        if (!(error instanceof CancellationException)) {
            // Create a specific execution exception and handle it
            ExecutionException executionError = new ExecutionException(
                "Error occurred during prompt execution", error, 
                com.devoxx.genie.service.prompt.error.PromptException.ErrorSeverity.ERROR, true);
            PromptErrorHandler.handleException(project, executionError, context);
        }
    }

    /**
     * Clean up after execution completes.
     */
    private void cleanupAfterExecution(Project project, @NotNull Runnable enableButtons) {
        enableButtons.run();
        FileListManager.getInstance().storeAddedFiles(project);
    }
}
