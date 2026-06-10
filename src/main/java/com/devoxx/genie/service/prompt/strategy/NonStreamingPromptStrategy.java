package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.error.ExecutionException;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.response.nonstreaming.NonStreamingPromptExecutionService;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.compose.model.TerminalState;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Strategy for executing non-streaming prompts.
 */
@Slf4j
public class NonStreamingPromptStrategy extends AbstractPromptExecutionStrategy {

    protected NonStreamingPromptExecutionService promptExecutionService;
    private final AtomicReference<PromptOutputPanel> currentPanel = new AtomicReference<>();
    private final AtomicReference<String> currentMessageId = new AtomicReference<>();
    private final AtomicReference<String> currentTabKey = new AtomicReference<>("default");

    public NonStreamingPromptStrategy(Project project) {
        super(project);
        this.promptExecutionService = NonStreamingPromptExecutionService.getInstance();
    }
    
    /**
     * Constructor for dependency injection, primarily used for testing.
     *
     * @param project The IntelliJ project
     * @param chatMemoryManager The chat memory manager
     * @param threadPoolManager The thread pool manager
     * @param promptExecutionService The non-streaming prompt execution service
     */
    protected NonStreamingPromptStrategy(
            @NotNull Project project,
            @NotNull ChatMemoryManager chatMemoryManager,
            @NotNull ThreadPoolManager threadPoolManager,
            @NotNull NonStreamingPromptExecutionService promptExecutionService) {
        super(project, chatMemoryManager, threadPoolManager);
        this.promptExecutionService = promptExecutionService;
    }

    @Override
    protected String getStrategyName() {
        return "non-streaming prompt";
    }

    @Override
    protected void executeStrategySpecific(
            @NotNull ChatMessageContext context,
            @NotNull PromptOutputPanel panel,
            @NotNull PromptTask<PromptResult> resultTask) {

        // Store references for cancellation
        currentPanel.set(panel);
        currentMessageId.set(context.getId());
        currentTabKey.set(context.getTabId() != null ? context.getTabId() : "default");

        // /find no longer needs a special branch here — the abstract parent short-circuits
        // before this method runs (so all strategies, including streaming, skip the LLM
        // call for find queries).

        // prepareMemory() runs the RAG retrieval (and, when query expansion is enabled, N LLM
        // calls). On the EDT it freezes the prompt-panel glow + Compose loading indicator
        // until those finish (task-217). Move it inside the pool task so the indicator —
        // already enabled by addUserPromptMessage() — can repaint immediately.
        threadPoolManager.getPromptExecutionPool().execute(() -> {
            try {
                prepareMemory(context);

                // Record start time
                long startTime = System.currentTimeMillis();

                // Execute the query
                var response = promptExecutionService.executeQuery(context).get();
                
                if (response == null) {
                    // Error was already handled by executeQuery()'s exceptionally() handler
                    // which swallows the exception and returns null - hide loading indicator
                    hideLoadingIndicator(panel, context.getId());
                    resultTask.complete(PromptResult.failure(context, new ExecutionException("Null response received")));
                    return;
                }
                
                log.debug("Adding AI message to prompt output panel for context {}", context.getId());
                context.setAiMessage(response.aiMessage());
                context.setExecutionTimeMs(System.currentTimeMillis() - startTime);

                // Set token usage and cost
                context.setTokenUsageAndCost(response.tokenUsage());

                // Add chat response to panel
                panel.addChatResponse(context);

                // Add the conversation to the chat service
                project.getMessageBus()
                        .syncPublisher(AppTopics.CONVERSATION_TOPIC)
                        .onNewConversation(context);

                // Add file references if any
                if (context.getFileReferences() != null && !context.getFileReferences().isEmpty()) {
                    log.debug("Adding file references to conversation: {} files", context.getFileReferences().size());
                    panel.getConversationPanel().viewController.addFileReferences(context, context.getFileReferences());
                }

                // Hide loading indicator on successful completion
                if (panel.getConversationPanel() != null && panel.getConversationPanel().viewController != null) {
                    panel.getConversationPanel().viewController.hideLoadingIndicator(context.getId());
                }

                resultTask.complete(PromptResult.success(context));
            } catch (Exception e) {
                if (e instanceof CancellationException || 
                    e.getCause() instanceof CancellationException || 
                    Thread.currentThread().isInterrupted()) {
                    log.info("Prompt execution cancelled for context {}", context.getId());
                    resultTask.cancel(true);
                } else {
                    handleExecutionError(e, context, resultTask, panel);
                }
            }
        });
        
        // Additional cancellation handling for non-streaming strategy
        resultTask.whenComplete((result, error) -> {
            if (resultTask.isCancelled()) {
                log.debug("Task cancelled, cancelling prompt execution for tab {}", currentTabKey.get());
                promptExecutionService.cancelExecutingQueryForTab(currentTabKey.get());
            }
        });
    }

    /**
     * Cancel the current prompt execution.
     * Deactivates activity handlers and hides the loading indicator to prevent
     * stale events from re-showing the "Thinking..." indicator.
     */
    @Override
    public void cancel() {
        log.info("Cancelling non-streaming strategy for tab {}", currentTabKey.get());
        promptExecutionService.cancelExecutingQueryForTab(currentTabKey.get());

        PromptOutputPanel panel = currentPanel.get();
        if (panel != null && panel.getConversationPanel() != null
                && panel.getConversationPanel().viewController != null) {
            var viewController = panel.getConversationPanel().viewController;
            // Deactivate handlers first to prevent stale events from re-showing indicator
            viewController.deactivateActivityHandlers();
            // Then hide the loading indicator
            String messageId = currentMessageId.get();
            if (messageId != null) {
                viewController.hideLoadingIndicator(messageId);
                // Durable in-chat marker: non-streaming runs have no partial text, but the
                // user must still see that the request was stopped rather than silently
                // dropped. Terminal states are final, so a late completion can't undo it.
                // Posted via invokeLater: cancel() can run on arbitrary threads and Compose
                // state mutations belong on the EDT.
                ApplicationManager.getApplication().invokeLater(() ->
                        viewController.setTerminalState(messageId, TerminalState.STOPPED, null));
            }
        }
    }

}
