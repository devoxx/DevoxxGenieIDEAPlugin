package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.service.prompt.error.ExecutionException;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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

        // Execute strategy-specific logic
        try {
            executeStrategySpecific(context, panel, resultTask);
        } catch (Exception e) {
            handleExecutionError(e, context, resultTask, panel);
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
     * Builds a prompt string that includes conversation history for strategies
     * that communicate via plain text (CLI/ACP runners).
     * Prepares memory, adds the user message, then formats prior exchanges
     * as a text preamble prepended to the current prompt.
     *
     * @param context The chat message context
     * @return The prompt string, optionally prefixed with conversation history
     */
    protected String buildPromptWithHistory(@NotNull ChatMessageContext context) {
        prepareMemory(context);
        chatMemoryManager.addUserMessage(context);

        List<ChatMessage> messages = chatMemoryManager.getMessages(project);

        // Collect prior exchanges (skip SystemMessage and the last UserMessage which is the current prompt)
        StringBuilder history = new StringBuilder();
        int messageCount = messages.size();
        // The last message is the current user message we just added — exclude it from history
        int historyEnd = messageCount - 1;

        for (int i = 0; i < historyEnd; i++) {
            ChatMessage msg = messages.get(i);
            if (msg instanceof SystemMessage) {
                continue;
            }
            if (msg instanceof UserMessage userMsg) {
                history.append("[user]: ").append(userMsg.singleText()).append("\n");
            } else if (msg instanceof AiMessage aiMsg) {
                history.append("[assistant]: ").append(aiMsg.text()).append("\n");
            }
        }

        // Build the full prompt including conversation history, file context, and user prompt
        StringBuilder fullPrompt = new StringBuilder();
        
        // Add conversation history if present
        if (!history.isEmpty()) {
            fullPrompt.append("<conversation_history>\n")
                      .append(history)
                      .append("</conversation_history>\n\n");
        }
        
        // Add file context if present (for ACP/CLI runners that need plain text)
        String filesContext = context.getFilesContext();
        if (filesContext != null && !filesContext.isEmpty()) {
            fullPrompt.append("<attached_files>\n")
                      .append(filesContext)
                      .append("</attached_files>\n\n");
        }
        
        // Add the user prompt
        fullPrompt.append(context.getUserPrompt());
        
        return fullPrompt.toString();
    }

    /**
     * Standardized error handling for execution exceptions.
     * Hides the "Thinking..." loading indicator when an error occurs.
     *
     * @param error The exception thrown
     * @param context The chat message context
     * @param resultTask The task to complete with error result
     * @param panel The UI panel to hide the loading indicator
     */
    protected void handleExecutionError(@NotNull Throwable error, 
                                     @NotNull ChatMessageContext context,
                                     @NotNull PromptTask<PromptResult> resultTask,
                                     @NotNull PromptOutputPanel panel) {
        if (error instanceof CancellationException || 
            Thread.currentThread().isInterrupted()) {
            resultTask.cancel(true);
            return;
        }
        
        log.error("Error in {} execution: {}", getStrategyName(), error.getMessage(), error);
        
        // Hide the "Thinking..." loading indicator to prevent it from staying active
        hideLoadingIndicator(panel, context.getId());
        
        ExecutionException executionError = new ExecutionException(
            "Error in " + getStrategyName() + " execution", error);
        PromptErrorHandler.handleException(context.getProject(), executionError, context);
        resultTask.complete(PromptResult.failure(context, executionError));
    }
    
    /**
     * Helper method to hide the "Thinking..." loading indicator.
     *
     * @param panel The UI panel containing the web view
     * @param messageId The message ID for the loading indicator to hide
     */
    protected void hideLoadingIndicator(@NotNull PromptOutputPanel panel, @NotNull String messageId) {
        if (panel.getConversationPanel() != null 
                && panel.getConversationPanel().viewController != null) {
            var viewController = panel.getConversationPanel().viewController;
            // Deactivate handlers first to prevent stale events from re-showing indicator
            viewController.deactivateActivityHandlers();
            // Then hide the loading indicator
            viewController.hideLoadingIndicator(messageId);
        }
    }
    
    /**
     * Handles task completion and cleanup for cancelled or failed tasks.
     * Hides the "Thinking..." loading indicator when the task fails.
     *
     * @param task The prompt task
     * @param context The chat message context
     * @param panel The UI panel to hide the loading indicator on error
     */
    protected void handleTaskCompletion(@NotNull PromptTask<PromptResult> task, 
                                     @NotNull ChatMessageContext context,
                                     @NotNull PromptOutputPanel panel) {
        task.whenComplete((result, error) -> {
            if (task.isCancelled()) {
                chatMemoryManager.removeLastUserMessage(context);
                log.debug("Task for context {} was cancelled, cleaned up UI and memory", context.getId());
            } else if (error != null) {
                // Hide the loading indicator when task completes with an error
                hideLoadingIndicator(panel, context.getId());
                log.debug("Task for context {} completed with error, hid loading indicator", context.getId());
            }
        });
    }
}
