package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.error.ExecutionException;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;

/**
 * Abstract base implementation for prompt execution strategies.
 * Provides common functionality and standardizes error handling across implementations.
 */
@Slf4j
public abstract class AbstractPromptExecutionStrategy implements PromptExecutionStrategy {
    
    protected final Project project;
    protected final ChatMemoryManager chatMemoryManager;
    protected final ThreadPoolManager threadPoolManager;
    
    /**
     * Constructor for AbstractPromptExecutionStrategy.
     *
     * @param project The IntelliJ project
     */
    public AbstractPromptExecutionStrategy(@NotNull Project project) {
        this.project = project;
        this.chatMemoryManager = ChatMemoryManager.getInstance();
        this.threadPoolManager = ThreadPoolManager.getInstance();
    }
    
    @Override
    public PromptTask<PromptResult> execute(@NotNull ChatMessageContext context, 
                                         @NotNull PromptOutputPanel panel) {
        log.debug("Executing {} for context: {}", getStrategyName(), context.getId());
        
        // Create a self-managed prompt task
        PromptTask<PromptResult> resultTask = new PromptTask<>(project);
        resultTask.putUserData(PromptTask.CONTEXT_KEY, context);
        
        // Add user prompt to UI
        panel.addUserPrompt(context);
        
        // Execute strategy-specific logic
        try {
            executeStrategySpecific(context, panel, resultTask);
        } catch (Exception e) {
            handleExecutionError(e, context, resultTask);
        }
        
        // Common post-execution handling
        handleTaskCompletion(resultTask, context, panel);
        
        return resultTask;
    }
    
    /**
     * Template method to be implemented by concrete strategies for specific execution logic.
     *
     * @param context The chat message context
     * @param panel The UI panel
     * @param resultTask The task to complete with results
     */
    protected abstract void executeStrategySpecific(
        @NotNull ChatMessageContext context,
        @NotNull PromptOutputPanel panel,
        @NotNull PromptTask<PromptResult> resultTask);
    
    /**
     * Returns the name of the strategy for logging purposes.
     *
     * @return The strategy name
     */
    protected abstract String getStrategyName();
    
    /**
     * Prepares memory with system message if needed and adds the user message.
     *
     * @param context The chat message context
     */
    protected void prepareMemory(@NotNull ChatMessageContext context) {
        chatMemoryManager.prepareMemory(context);
        chatMemoryManager.addUserMessage(context);
    }
    
    /**
     * Standardized error handling for execution exceptions.
     *
     * @param error The exception thrown
     * @param context The chat message context
     * @param resultTask The task to complete with error result
     */
    protected void handleExecutionError(@NotNull Throwable error, 
                                     @NotNull ChatMessageContext context,
                                     @NotNull PromptTask<PromptResult> resultTask) {
        if (error instanceof CancellationException || 
            Thread.currentThread().isInterrupted()) {
            resultTask.cancel(true);
            return;
        }
        
        log.error("Error in {} execution: {}", getStrategyName(), error.getMessage(), error);
        ExecutionException executionError = new ExecutionException(
            "Error in " + getStrategyName() + " execution", error);
        PromptErrorHandler.handleException(context.getProject(), executionError, context);
        resultTask.complete(PromptResult.failure(context, executionError));
    }
    
    /**
     * Handles task completion and cleanup for cancelled tasks.
     *
     * @param task The prompt task
     * @param context The chat message context
     * @param panel The UI panel
     */
    protected void handleTaskCompletion(@NotNull PromptTask<PromptResult> task, 
                                     @NotNull ChatMessageContext context,
                                     @NotNull PromptOutputPanel panel) {
        task.whenComplete((result, error) -> {
            if (task.isCancelled()) {
                panel.removeLastUserPrompt(context);
                chatMemoryManager.removeLastUserMessage(context);
                log.debug("Task for context {} was cancelled, cleaned up UI and memory", context.getId());
            }
        });
    }
}
