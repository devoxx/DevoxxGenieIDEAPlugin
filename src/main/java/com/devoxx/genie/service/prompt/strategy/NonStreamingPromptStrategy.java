package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.service.prompt.error.ExecutionException;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.SemanticFile;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.nonstreaming.NonStreamingPromptExecutionService;
import com.devoxx.genie.service.rag.SearchResult;
import com.devoxx.genie.service.rag.SemanticSearchService;
import com.devoxx.genie.service.prompt.threading.PromptTaskTracker;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.devoxx.genie.model.Constant.FIND_COMMAND;
import static com.devoxx.genie.service.MessageCreationService.extractFileReferences;

/**
 * Strategy for executing non-streaming prompts.
 */
@Slf4j
public class NonStreamingPromptStrategy implements PromptExecutionStrategy {

    private final Project project;
    private final NonStreamingPromptExecutionService promptExecutionService;
    private final ChatMemoryManager chatMemoryManager;
    private final ThreadPoolManager threadPoolManager;
    private final PromptTaskTracker taskTracker;
    private final String taskId;

    public NonStreamingPromptStrategy(Project project) {
        this.project = project;
        this.promptExecutionService = NonStreamingPromptExecutionService.getInstance();
        this.chatMemoryManager = ChatMemoryManager.getInstance();
        this.threadPoolManager = ThreadPoolManager.getInstance();
        this.taskTracker = PromptTaskTracker.getInstance();
        this.taskId = project.getLocationHash() + "-" + System.currentTimeMillis();
    }

    /**
     * Execute the prompt using the non-streaming approach.
     */
    @Override
    public CompletableFuture<Void> execute(@NotNull ChatMessageContext chatMessageContext,
                                          @NotNull PromptOutputPanel promptOutputPanel) {
        log.debug("Executing non-streaming prompt, command name: " + chatMessageContext.getCommandName());
        promptOutputPanel.addUserPrompt(chatMessageContext);

        if (FIND_COMMAND.equalsIgnoreCase(chatMessageContext.getCommandName())) {
            log.debug("Executing find prompt");
            semanticSearch(chatMessageContext, promptOutputPanel);
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        
        // Prepare the memory with system message if needed
        chatMemoryManager.prepareMemory(chatMessageContext);
        
        // Add user message to context and memory
        chatMemoryManager.addUserMessage(chatMessageContext);

        // Create the task that will handle cancellation
        PromptTaskTracker.CancellableTask task = () -> {
            // Cancel any running query in the execution service
            promptExecutionService.cancelExecutingQuery();
            
            // Remove the user prompt from the UI if needed
            promptOutputPanel.removeLastUserPrompt(chatMessageContext);
            
            // Mark the future as cancelled
            if (!resultFuture.isDone()) {
                resultFuture.cancel(true);
            }
        };
        
        // Register the task for tracking
        taskTracker.registerTask(project, taskId, task);

        // Execute the prompt using the centralized thread pool
        CompletableFuture.supplyAsync(() -> {
            try {
                return promptExecutionService.executeQuery(chatMessageContext).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new CompletionException(e);
            } catch (java.util.concurrent.ExecutionException e) {
                throw new RuntimeException(e);
            }
                }, threadPoolManager.getPromptExecutionPool())
        .thenAcceptAsync(response -> {
            if (response != null) {
                log.debug("Adding AI message to prompt output panel");
                chatMessageContext.setAiMessage(response.aiMessage());

                // Set token usage and cost
                chatMessageContext.setTokenUsageAndCost(response.tokenUsage());
                
                // Add AI response to memory
                chatMemoryManager.addAiResponse(chatMessageContext);

                // Add the conversation to the chat service
                project.getMessageBus()
                        .syncPublisher(AppTopics.CONVERSATION_TOPIC)
                        .onNewConversation(chatMessageContext);

                promptOutputPanel.addChatResponse(chatMessageContext);
                resultFuture.complete(null);
            }
            // Task is completed either way
            taskTracker.taskCompleted(project, taskId);
        }, threadPoolManager.getPromptExecutionPool())
        .exceptionally(throwable -> {
            if (!(throwable instanceof CompletionException && throwable.getCause() instanceof CancellationException)) {
                // Create a specific execution exception and handle it with our standardized handler
                ExecutionException executionError = new ExecutionException("Error occurred while processing chat message", throwable);
                PromptErrorHandler.handleException(chatMessageContext.getProject(), executionError, chatMessageContext);
            }
            resultFuture.completeExceptionally(throwable);
            taskTracker.taskCompleted(project, taskId);
            return null;
        });
                
        return resultFuture;
    }

    /**
     * Cancel the current prompt execution.
     */
    @Override
    public void cancel() {
        taskTracker.cancelTask(project, taskId);
    }

    /**
     * Perform semantic search for the FIND command.
     */
    private static void semanticSearch(ChatMessageContext chatMessageContext,
                                       @NotNull PromptOutputPanel promptOutputPanel) {
        try {
            SemanticSearchService semanticSearchService = SemanticSearchService.getInstance();
            Map<String, SearchResult> searchResults = semanticSearchService.search(
                    chatMessageContext.getProject(),
                    chatMessageContext.getUserPrompt()
            );

            if (!searchResults.isEmpty()) {
                List<SemanticFile> fileReferences = extractFileReferences(searchResults);
                chatMessageContext.setSemanticReferences(fileReferences);
                promptOutputPanel.addChatResponse(chatMessageContext);
            } else {
                NotificationUtil.sendNotification(chatMessageContext.getProject(),
                        "No relevant files found for your search query.");
            }
        } catch (Exception e) {
            // Create a specific execution exception for semantic search errors
            ExecutionException searchError = new ExecutionException("Error performing semantic search", e, 
                    com.devoxx.genie.service.prompt.error.PromptException.ErrorSeverity.WARNING, true);
            PromptErrorHandler.handleException(chatMessageContext.getProject(), searchError, chatMessageContext);
        }
    }
}
