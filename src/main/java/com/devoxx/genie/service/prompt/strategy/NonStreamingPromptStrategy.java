package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.service.prompt.error.ExecutionException;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.error.PromptException;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.SemanticFile;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.nonstreaming.NonStreamingPromptExecutionService;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
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

    public NonStreamingPromptStrategy(Project project) {
        this.project = project;
        this.promptExecutionService = NonStreamingPromptExecutionService.getInstance();
        this.chatMemoryManager = ChatMemoryManager.getInstance();
        this.threadPoolManager = ThreadPoolManager.getInstance();
    }

    /**
     * Execute the prompt using the non-streaming approach.
     */
    @Override
    public PromptTask<PromptResult> execute(@NotNull ChatMessageContext context,
                                          @NotNull PromptOutputPanel panel) {
        log.debug("Executing non-streaming prompt, command name: {}", context.getCommandName());
        panel.addUserPrompt(context);

        // Create a self-managed prompt task
        PromptTask<PromptResult> resultTask = new PromptTask<>(project);

        // Handle FIND command separately
        if (FIND_COMMAND.equalsIgnoreCase(context.getCommandName())) {
            log.debug("Executing find prompt");
            executeSemanticSearch(context, panel, resultTask);
            return resultTask;
        }

        // Prepare the memory with system message if needed
        chatMemoryManager.prepareMemory(context);
        
        // Add user message to context and memory
        chatMemoryManager.addUserMessage(context);

        // Execute the prompt using the centralized thread pool
        threadPoolManager.getPromptExecutionPool().execute(() -> {
            try {
                // Record start time
                long startTime = System.currentTimeMillis();
                
                // Execute the query
                var response = promptExecutionService.executeQuery(context).get();
                
                if (response == null) {
                    resultTask.complete(PromptResult.failure(context, 
                        new ExecutionException("Null response received")));
                    return;
                }
                
                log.debug("Adding AI message to prompt output panel for context {}", context.getId());
                context.setAiMessage(response.aiMessage());
                context.setExecutionTimeMs(System.currentTimeMillis() - startTime);

                // Set token usage and cost
                context.setTokenUsageAndCost(response.tokenUsage());
                
                // Add AI response to memory
                chatMemoryManager.addAiResponse(context);

                // Add the conversation to the chat service
                project.getMessageBus()
                        .syncPublisher(AppTopics.CONVERSATION_TOPIC)
                        .onNewConversation(context);

                panel.addChatResponse(context);
                resultTask.complete(PromptResult.success(context));
            } catch (Exception e) {
                if (e instanceof CancellationException || 
                    e.getCause() instanceof CancellationException || 
                    Thread.currentThread().isInterrupted()) {
                    log.info("Prompt execution cancelled for context {}", context.getId());
                    resultTask.cancel(true);
                } else {
                    log.error("Error in non-streaming prompt execution", e);
                    // Create a specific execution exception and handle it
                    ExecutionException executionError = new ExecutionException(
                        "Error occurred while processing chat message", e);
                    PromptErrorHandler.handleException(context.getProject(), executionError, context);
                    resultTask.complete(PromptResult.failure(context, executionError));
                }
            }
        });
        
        // Handle cancellation by cancelling the underlying execution
        resultTask.whenComplete((result, error) -> {
            if (resultTask.isCancelled()) {
                log.debug("Task cancelled, cancelling prompt execution");
                promptExecutionService.cancelExecutingQuery();
                panel.removeLastUserPrompt(context);
            }
        });
                
        return resultTask;
    }

    /**
     * Cancel the current prompt execution.
     */
    @Override
    public void cancel() {
        promptExecutionService.cancelExecutingQuery();
    }

    /**
     * Perform semantic search for the FIND command.
     */
    private void executeSemanticSearch(
            @NotNull ChatMessageContext context,
            @NotNull PromptOutputPanel panel,
            @NotNull PromptTask<PromptResult> resultTask) {
        
        threadPoolManager.getPromptExecutionPool().execute(() -> {
            try {
                SemanticSearchService semanticSearchService = SemanticSearchService.getInstance();
                Map<String, SearchResult> searchResults = semanticSearchService.search(
                        context.getProject(),
                        context.getUserPrompt()
                );

                if (!searchResults.isEmpty()) {
                    List<SemanticFile> fileReferences = extractFileReferences(searchResults);
                    context.setSemanticReferences(fileReferences);
                    panel.addChatResponse(context);
                    resultTask.complete(PromptResult.success(context));
                } else {
                    NotificationUtil.sendNotification(context.getProject(),
                            "No relevant files found for your search query.");
                    resultTask.complete(PromptResult.failure(context, 
                        new ExecutionException("No relevant files found")));
                }
            } catch (Exception e) {
                // Create a specific execution exception for semantic search errors
                ExecutionException searchError = new ExecutionException(
                    "Error performing semantic search", e, 
                    PromptException.ErrorSeverity.WARNING, true);
                PromptErrorHandler.handleException(context.getProject(), searchError, context);
                resultTask.complete(PromptResult.failure(context, searchError));
            }
        });
    }
}
