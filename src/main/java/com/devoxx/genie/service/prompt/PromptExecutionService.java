package com.devoxx.genie.service.prompt;

import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.command.PromptCommandProcessor;
import com.devoxx.genie.service.prompt.strategy.PromptExecutionStrategy;
import com.devoxx.genie.service.prompt.strategy.PromptExecutionStrategyFactory;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified service for executing prompts with various strategies.
 */
@Slf4j
public class PromptExecutionService {

    private final PromptCommandProcessor commandProcessor;
    private final PromptExecutionStrategyFactory strategyFactory;
    private final ConcurrentHashMap<Project, PromptExecutionStrategy> activeStrategies = new ConcurrentHashMap<>();

    public static PromptExecutionService getInstance(@NotNull Project project) {
        return project.getService(PromptExecutionService.class);
    }

    public PromptExecutionService(Project project) {
        this.commandProcessor = PromptCommandProcessor.getInstance();
        this.strategyFactory = PromptExecutionStrategyFactory.getInstance();
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
        
        // Cancel any running execution for this project
        if (activeStrategies.containsKey(project)) {
            stopExecution(project);
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
                activeStrategies.put(project, strategy);
                
                // Execute the prompt
                strategy.execute(chatMessageContext, promptOutputPanel, () ->
                    ApplicationManager.getApplication().invokeLater(() -> {
                        cleanupAfterExecution(project, enableButtons);
                    }));
            }

            @Override
            public void onCancel() {
                super.onCancel();
                log.info("Prompt execution was cancelled.");
                stopExecution(project);
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                super.onThrowable(error);
                if (!(error instanceof CancellationException)) {
                    log.error("Error occurred while processing chat message", error);
                    ErrorHandler.handleError(project, error);
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
        PromptExecutionStrategy strategy = activeStrategies.get(project);
        if (strategy != null) {
            strategy.cancel();
            activeStrategies.remove(project);
        }
    }

    /**
     * Clean up after execution completes.
     *
     * @param project The project that finished execution
     * @param enableButtons Callback to enable UI elements
     */
    private void cleanupAfterExecution(Project project, @NotNull Runnable enableButtons) {
        activeStrategies.remove(project);
        enableButtons.run();
        FileListManager.getInstance().storeAddedFiles(project);
    }
}
