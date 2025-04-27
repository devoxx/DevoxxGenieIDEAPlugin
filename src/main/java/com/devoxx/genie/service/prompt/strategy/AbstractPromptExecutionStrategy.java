package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.service.prompt.error.ExecutionException;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.UserMessage;
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
    protected ChatMemoryManager chatMemoryManager;
    protected ThreadPoolManager threadPoolManager;
    protected MessageCreationService messageCreationService;
    
    /**
     * Constructor for AbstractPromptExecutionStrategy.
     *
     * @param project The IntelliJ project
     */
    protected AbstractPromptExecutionStrategy(@NotNull Project project) {
        this.project = project;
        this.chatMemoryManager = ChatMemoryManager.getInstance();
        this.threadPoolManager = ThreadPoolManager.getInstance();
        this.messageCreationService = MessageCreationService.getInstance();
    }
    
    /**
     * Constructor for AbstractPromptExecutionStrategy that allows dependency injection.
     * Primarily used for testing.
     *
     * @param project The IntelliJ project
     * @param chatMemoryManager The chat memory manager
     * @param threadPoolManager The thread pool manager
     */
    protected AbstractPromptExecutionStrategy(@NotNull Project project,
                                           @NotNull ChatMemoryManager chatMemoryManager,
                                           @NotNull ThreadPoolManager threadPoolManager) {
        this.project = project;
        this.chatMemoryManager = chatMemoryManager;
        this.threadPoolManager = threadPoolManager;
        this.messageCreationService = MessageCreationService.getInstance();
    }
    
    /**
     * Constructor for AbstractPromptExecutionStrategy that allows full dependency injection.
     * Primarily used for testing.
     *
     * @param project The IntelliJ project
     * @param chatMemoryManager The chat memory manager
     * @param threadPoolManager The thread pool manager
     * @param messageCreationService The message creation service
     */
    protected AbstractPromptExecutionStrategy(@NotNull Project project,
                                           @NotNull ChatMemoryManager chatMemoryManager,
                                           @NotNull ThreadPoolManager threadPoolManager,
                                           @NotNull MessageCreationService messageCreationService) {
        this.project = project;
        this.chatMemoryManager = chatMemoryManager;
        this.threadPoolManager = threadPoolManager;
        this.messageCreationService = messageCreationService;
    }
    
    @Override
    public PromptTask<PromptResult> execute(@NotNull ChatMessageContext context, 
                                         @NotNull PromptOutputPanel panel) {
        log.debug("Executing {} for context: {}", getStrategyName(), context.getId());
        
        // Create a self-managed prompt task
        PromptTask<PromptResult> resultTask = new PromptTask<>(project);
        resultTask.putUserData(PromptTask.CONTEXT_KEY, context);
        
        // Add user prompt to UI
        // panel.addUserPrompt(context);
        
        // Execute strategy-specific logic
        try {
            executeStrategySpecific(context, panel, resultTask);
        } catch (Exception e) {
            handleExecutionError(e, context, resultTask);
        }
        
        // Common post-execution handling
        handleTaskCompletion(resultTask, context);
        
        return resultTask;
    }
    
    /**
     * Template method to be implemented by concrete strategies for specific execution logic.
     *
     * @param context The chat message context
     * @param panel The UI panel
     * @param resultTask The task to complete with results
     */
    protected abstract void executeStrategySpecific(ChatMessageContext context,
                                                    PromptOutputPanel panel,
                                                    PromptTask<PromptResult> resultTask);
    
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
    public void prepareMemory(ChatMessageContext context) {
        // Prepare memory with system message if needed and add user message
        log.debug("Before memory preparation - context ID: {}", context.getId());

        chatMemoryManager.prepareMemory(context);

        // Add context information to the user message before adding to memory
        messageCreationService.addUserMessageToContext(context);

        // Check if user message was properly created
        if (context.getUserMessage() == null) {
            log.error("Failed to create user message for context ID: {}", context.getId());
            // Create a fallback user message if needed
            context.setUserMessage(UserMessage.from(context.getUserPrompt()));
        }
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
     */
    protected void handleTaskCompletion(@NotNull PromptTask<PromptResult> task, 
                                     @NotNull ChatMessageContext context) {
        task.whenComplete((result, error) -> {
            if (task.isCancelled()) {
                // TODO Check if we can actually remove context from memory?!
                // panel.removeLastUserPrompt(context);
                chatMemoryManager.removeLastUserMessage(context);
                log.debug("Task for context {} was cancelled, cleaned up UI and memory", context.getId());
            }
        });
    }
}
