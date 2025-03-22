package com.devoxx.genie.service.prompt.command;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Interface for handling special prompt commands.
 */
public interface PromptCommand {
    
    /**
     * Check if this command matches the given prompt.
     *
     * @param prompt the user prompt to check
     * @return true if this command should handle the prompt
     */
    boolean matches(@NotNull String prompt);
    
    /**
     * Process the command, potentially modifying the context and/or displaying output.
     *
     * @param chatMessageContext the context to process
     * @param promptOutputPanel the output panel for displaying results
     * @return an Optional containing the processed prompt if execution should continue,
     *         or empty if execution should stop after this command
     */
    Optional<String> process(@NotNull ChatMessageContext chatMessageContext, 
                            @NotNull PromptOutputPanel promptOutputPanel);
}
