package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.SemanticFile;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.service.prompt.error.ExecutionException;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.error.PromptException;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.PromptTaskTracker;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.service.rag.RAGEventPublisher;
import com.devoxx.genie.service.rag.SearchResult;
import com.devoxx.genie.service.rag.SemanticSearchService;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;

import static com.devoxx.genie.model.Constant.FIND_COMMAND;
import static com.devoxx.genie.service.MessageCreationService.extractFileReferences;

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
        
        // Create a self-managed prompt task with tab awareness
        PromptTask<PromptResult> resultTask = new PromptTask<>(project, context.getTabId());
        resultTask.putUserData(PromptTask.CONTEXT_KEY, context);
        // Re-index now that context is attached (fixes timing gap from self-registration)
        PromptTaskTracker.getInstance().indexByContextId(resultTask);

        // /find short-circuits ALL strategies — it's a pure semantic-search request, no LLM
        // call needed. Without this gate, streaming mode would run prepareMemory() + an
        // empty LLM call after the user already got their files popup, wasting the call
        // (and confusing users who see "the chat is already finished" but a model still ticks).
        if (FIND_COMMAND.equalsIgnoreCase(context.getCommandName())) {
            log.debug("Short-circuiting strategy for /find command on context {}", context.getId());
            executeSemanticSearch(context, panel, resultTask);
            handleTaskCompletion(resultTask, context, panel);
            return resultTask;
        }

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
     * Perform semantic search for the /find command and write the results back onto the
     * chat panel. Bypasses the strategy-specific LLM path entirely — see the gate in
     * {@link #execute(ChatMessageContext, PromptOutputPanel)}.
     */
    protected void executeSemanticSearch(@NotNull ChatMessageContext context,
                                         @NotNull PromptOutputPanel panel,
                                         @NotNull PromptTask<PromptResult> resultTask) {
        threadPoolManager.getPromptExecutionPool().execute(() -> {
            long startNanos = System.nanoTime();
            List<SearchResult> searchResults = Collections.emptyList();
            try {
                searchResults = SemanticSearchService.getInstance().search(
                        context.getProject(),
                        context.getUserPrompt(),
                        context.getChatModel());

                if (!searchResults.isEmpty()) {
                    List<SemanticFile> fileReferences = extractFileReferences(searchResults);
                    context.setSemanticReferences(fileReferences);
                    // Fill the pending AI bubble with a one-line summary so the chat history
                    // stays coherent — without this, /find leaves an empty unbordered bubble
                    // sitting under the user's prompt because no AI response was generated.
                    // The full hit list still pops up in the Find Results dialog.
                    long uniqueFiles = fileReferences.stream()
                            .map(SemanticFile::filePath).distinct().count();
                    context.setAiMessage(AiMessage.from(String.format(
                            "Found %d relevant file%s for `%s` — see the Find Results dialog.",
                            uniqueFiles,
                            uniqueFiles == 1 ? "" : "s",
                            context.getUserPrompt())));
                    panel.addChatResponse(context);
                    resultTask.complete(PromptResult.success(context));
                } else {
                    NotificationUtil.sendNotification(context.getProject(),
                            "No relevant files found for your search query.");
                    resultTask.complete(PromptResult.failure(context,
                            new ExecutionException("No relevant files found")));
                }
            } catch (Exception e) {
                ExecutionException searchError = new ExecutionException(
                        "Error performing semantic search", e,
                        PromptException.ErrorSeverity.WARNING, true);
                PromptErrorHandler.handleException(context.getProject(), searchError, context);
                resultTask.complete(PromptResult.failure(context, searchError));
            } finally {
                long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
                RAGEventPublisher.publish(
                        context.getProject(), context.getUserPrompt(), searchResults, durationMs);
            }
        });
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

        List<ChatMessage> messages = chatMemoryManager.getMessagesByKey(context.getMemoryKey());

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
