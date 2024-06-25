package com.devoxx.genie.service;

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
                handleException(e, chatMessageContext);
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

    /**
     * Handle exception.
     * @param e the exception
     * @param chatMessageContext the chat message context
     */
    private void handleException(@NotNull Exception e, ChatMessageContext chatMessageContext) {
        Throwable cause = e.getCause();
        if (cause instanceof CancellationException) {
            return; // User cancelled, no warning required
        }
        if (cause instanceof TimeoutException) {
            NotificationUtil.sendNotification(chatMessageContext.getProject(),
                "Timeout occurred. Please increase the timeout setting.");
        } else if (cause instanceof ProviderUnavailableException) {
            NotificationUtil.sendNotification(chatMessageContext.getProject(),
                "LLM provider not available. Please select another provider or make sure it's running.");
        } else {
            String message = e.getMessage() + ". Maybe create an issue on GitHub?";
            NotificationUtil.sendNotification(chatMessageContext.getProject(), "Error occurred: " + message);
        }
    }
}
