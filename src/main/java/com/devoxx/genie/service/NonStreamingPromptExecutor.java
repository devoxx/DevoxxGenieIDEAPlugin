package com.devoxx.genie.service;

import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.service.exception.ProviderUnavailableException;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;

public class NonStreamingPromptExecutor {

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

        currentTask = CompletableFuture.runAsync(() -> {
            try {
                promptExecutionService.executeQuery(chatMessageContext)
                    .thenAccept(aiMessageOptional -> {
                        if (!isCancelled && aiMessageOptional.isPresent()) {
                            chatMessageContext.setAiMessage(aiMessageOptional.get());
                            promptOutputPanel.addChatResponse(chatMessageContext);
                        } else if (isCancelled) {
                            promptOutputPanel.removeLastUserPrompt(chatMessageContext);
                        }
                    }).get(); // This blocks until the CompletableFuture is done
            } catch (InterruptedException | ExecutionException e) {
                ErrorHandler.handleError(chatMessageContext.getProject(), e.getCause());
            } finally {
                enableButtons.run();
            }
        });
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
