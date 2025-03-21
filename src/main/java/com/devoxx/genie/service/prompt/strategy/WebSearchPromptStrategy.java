package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.error.WebSearchException;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.websearch.WebSearchPromptExecutionService;
import com.devoxx.genie.service.prompt.threading.PromptTaskTracker;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Strategy for executing web search based prompts.
 */
@Slf4j
public class WebSearchPromptStrategy implements PromptExecutionStrategy {

    private final ChatMemoryManager chatMemoryManager;
    private final ThreadPoolManager threadPoolManager;
    private final PromptTaskTracker taskTracker;
    private final Project project;
    private final String taskId;

    public WebSearchPromptStrategy(@NotNull Project project) {
        this.project = project;
        this.chatMemoryManager = ChatMemoryManager.getInstance();
        this.threadPoolManager = ThreadPoolManager.getInstance();
        this.taskTracker = PromptTaskTracker.getInstance();
        this.taskId = project.getLocationHash() + "-websearch-" + System.currentTimeMillis();
    }

    /**
     * Execute a prompt using web search.
     */
    @Override
    public CompletableFuture<Void> execute(@NotNull ChatMessageContext chatMessageContext,
                                          @NotNull PromptOutputPanel promptOutputPanel) {

        log.debug("Web search with query: {}", chatMessageContext.getUserMessage().singleText());

        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        promptOutputPanel.addUserPrompt(chatMessageContext);

        // Add user message to context and memory
        chatMemoryManager.prepareMemory(chatMessageContext);
        chatMemoryManager.addUserMessage(chatMessageContext);

        // Track execution time
        long startTime = System.currentTimeMillis();
        
        // Create cancellable task
        PromptTaskTracker.CancellableTask task = () -> {
            // Just need to cancel the future if not already done
            if (!resultFuture.isDone()) {
                resultFuture.cancel(true);
                promptOutputPanel.removeLastUserPrompt(chatMessageContext);
            }
        };
        
        // Register task for tracking
        taskTracker.registerTask(project, taskId, task);
        
        // Execute web search using thread pool
        CompletableFuture.supplyAsync(() -> {
            try {
                return WebSearchPromptExecutionService.getInstance().searchWeb(chatMessageContext);
            } catch (Exception e) {
                // Wrap in runtime exception to propagate
                throw new RuntimeException(e);
            }
        }, threadPoolManager.getPromptExecutionPool())
        .thenAcceptAsync(aiMessageOptional -> {
            aiMessageOptional.ifPresent(aiMessage -> {
                chatMessageContext.setAiMessage(aiMessage);
                chatMessageContext.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                
                // Add the AI response to memory
                chatMemoryManager.addAiResponse(chatMessageContext);
                
                // Display the response
                promptOutputPanel.addChatResponse(chatMessageContext);
                resultFuture.complete(null);
            });
            
            // Mark task as completed
            taskTracker.taskCompleted(project, taskId);
        }, threadPoolManager.getPromptExecutionPool())
        .exceptionally(throwable -> {
            // Create a specific web search exception and handle it with our standardized handler
            WebSearchException webSearchError = new WebSearchException("Error in web search prompt execution", throwable);
            PromptErrorHandler.handleException(project, webSearchError, chatMessageContext);
            resultFuture.completeExceptionally(webSearchError);
            
            // Mark task as completed
            taskTracker.taskCompleted(project, taskId);
            return null;
        });
        
        return resultFuture;
    }

    /**
     * Cancel the current execution.
     */
    @Override
    public void cancel() {
        taskTracker.cancelTask(project, taskId);
    }
}
