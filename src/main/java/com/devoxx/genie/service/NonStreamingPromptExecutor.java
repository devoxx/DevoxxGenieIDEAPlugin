package com.devoxx.genie.service;

import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;

public class NonStreamingPromptExecutor {

    private static final Logger LOG = Logger.getInstance(NonStreamingPromptExecutor.class);

    private final PromptExecutionService promptExecutionService;
    private volatile Future<?> currentTask;
    private volatile boolean isCancelled;

    public NonStreamingPromptExecutor() {
        this.promptExecutionService = PromptExecutionService.getInstance();
    }

    /**
     * Execute the prompt.
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel the prompt output panel
     * @param enableButtons the enable buttons
     */
    public void execute(ChatMessageContext chatMessageContext,
                        @NotNull PromptOutputPanel promptOutputPanel,
                        Runnable enableButtons) {
        promptOutputPanel.addUserPrompt(chatMessageContext);
        isCancelled = false;

        currentTask = promptExecutionService.executeQuery(chatMessageContext)
            .thenAccept(response -> {
                if (!isCancelled && response != null) {
                    LOG.debug(">>>> Adding AI message to prompt output panel");
                    chatMessageContext.setAiMessage(response.content());

                    // Set token usage and cost
                    chatMessageContext.setTokenUsageAndCost(response.tokenUsage());

                    promptOutputPanel.addChatResponse(chatMessageContext);
                } else if (isCancelled) {
                    LOG.debug(">>>> Prompt execution cancelled");
                    promptOutputPanel.removeLastUserPrompt(chatMessageContext);
                }
            })
            .exceptionally(throwable -> {
                ErrorHandler.handleError(chatMessageContext.getProject(), throwable);
                return null;
            })
            .whenComplete((result, throwable) -> enableButtons.run());
    }

    /**
     * Stop prompt execution.
     */
    public void stopExecution() {
        if (currentTask != null && !currentTask.isDone()) {
            isCancelled = true;
            currentTask.cancel(true);
        }
    }
}
