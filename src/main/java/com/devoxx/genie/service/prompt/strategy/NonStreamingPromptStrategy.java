package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.service.prompt.error.ExecutionException;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.SemanticFile;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.nonstreaming.NonStreamingPromptExecutionService;
import com.devoxx.genie.service.rag.SearchResult;
import com.devoxx.genie.service.rag.SemanticSearchService;
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
import java.util.concurrent.Future;

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
    private volatile Future<?> currentTask;
    private volatile boolean isCancelled;

    public NonStreamingPromptStrategy(Project project) {
        this.project = project;
        this.promptExecutionService = NonStreamingPromptExecutionService.getInstance();
        this.chatMemoryManager = ChatMemoryManager.getInstance();
    }

    /**
     * Execute the prompt using the non-streaming approach.
     */
    @Override
    public CompletableFuture<Void> execute(@NotNull ChatMessageContext chatMessageContext,
                                          @NotNull PromptOutputPanel promptOutputPanel,
                                          @NotNull Runnable onComplete) {
        log.debug("Executing non-streaming prompt, command name: " + chatMessageContext.getCommandName());
        promptOutputPanel.addUserPrompt(chatMessageContext);
        isCancelled = false;

        if (FIND_COMMAND.equalsIgnoreCase(chatMessageContext.getCommandName())) {
            log.debug("Executing find prompt");
            semanticSearch(chatMessageContext, promptOutputPanel);
            onComplete.run();
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        
        // Prepare the memory with system message if needed
        chatMemoryManager.prepareMemory(chatMessageContext);
        
        // Add user message to context and memory
        chatMemoryManager.addUserMessage(chatMessageContext);

        // Execute the prompt
        currentTask = promptExecutionService.executeQuery(chatMessageContext)
                .thenAccept(response -> {
                    if (!isCancelled && response != null) {
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
                    } else if (isCancelled) {
                        log.debug(">>>> Prompt execution cancelled");
                        promptOutputPanel.removeLastUserPrompt(chatMessageContext);
                        resultFuture.cancel(true);
                    }
                })
                .exceptionally(throwable -> {
                    if (!(throwable.getCause() instanceof CancellationException)) {
                        // Create a specific execution exception and handle it with our standardized handler
                        ExecutionException executionError = new ExecutionException("Error occurred while processing chat message", throwable);
                        PromptErrorHandler.handleException(chatMessageContext.getProject(), executionError, chatMessageContext);
                    }
                    resultFuture.completeExceptionally(throwable);
                    return null;
                })
                .whenComplete((result, throwable) -> onComplete.run());
                
        return resultFuture;
    }

    /**
     * Cancel the current prompt execution.
     */
    @Override
    public void cancel() {
        if (currentTask != null && !currentTask.isDone()) {
            isCancelled = true;
            currentTask.cancel(true);
        }
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
