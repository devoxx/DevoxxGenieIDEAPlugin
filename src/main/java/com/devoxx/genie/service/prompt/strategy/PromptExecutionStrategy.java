package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Strategy interface for different prompt execution approaches.
 */
public interface PromptExecutionStrategy {
    
    /**
     * Execute a prompt using the specific strategy implementation.
     *
     * @param chatMessageContext the context containing all prompt information
     * @param promptOutputPanel the panel where output should be displayed
     * @param onComplete callback to run when execution is complete
     * @return a CompletableFuture that completes when execution is done
     */
    CompletableFuture<Void> execute(@NotNull ChatMessageContext chatMessageContext, 
                                  @NotNull PromptOutputPanel promptOutputPanel, 
                                  @NotNull Runnable onComplete);
    
    /**
     * Cancel the current execution if possible.
     */
    void cancel();
}
