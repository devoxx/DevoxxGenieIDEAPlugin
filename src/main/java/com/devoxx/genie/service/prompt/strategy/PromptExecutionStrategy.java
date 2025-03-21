package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy interface for different prompt execution approaches.
 */
public interface PromptExecutionStrategy {
    
    /**
     * Execute a prompt using the specific strategy implementation.
     *
     * @param chatMessageContext the context containing all prompt information
     * @param promptOutputPanel the panel where output should be displayed
     * @return a PromptTask that completes with a PromptResult when execution is done
     */
    PromptTask<PromptResult> execute(@NotNull ChatMessageContext chatMessageContext, 
                                     @NotNull PromptOutputPanel promptOutputPanel);
    
    /**
     * Cancel the current execution if possible.
     * Default implementation is a no-op, strategy implementations should override if needed.
     */
    default void cancel() {
        // Default no-op implementation
    }
}
