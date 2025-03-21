package com.devoxx.genie.service.prompt.result;

import com.devoxx.genie.model.request.ChatMessageContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Standardized result object for prompt execution operations.
 * Encapsulates success/failure status, related context, and any error information.
 */
public class PromptResult {
    private final ChatMessageContext context;
    private final boolean successful;
    private final Throwable error;

    private PromptResult(@NotNull ChatMessageContext context,
                         boolean successful,
                         @Nullable Throwable error) {
        this.context = context;
        this.successful = successful;
        this.error = error;
    }

    /**
     * Create a successful result
     */
    public static PromptResult success(@NotNull ChatMessageContext context) {
        return new PromptResult(context, true, null);
    }

    /**
     * Create a failure result with associated error
     */
    public static PromptResult failure(@NotNull ChatMessageContext context, @NotNull Throwable error) {
        return new PromptResult(context, false, error);
    }

    /**
     * Get the chat message context associated with this result
     */
    public @NotNull ChatMessageContext getContext() {
        return context;
    }

    /**
     * Get the error if operation failed, or null if successful
     */
    public @Nullable Throwable getError() {
        return error;
    }

    @Override
    public String toString() {
        return "PromptResult{" +
                "successful=" + successful +
                ", error=" + (error != null ? error.getMessage() : "none") +
                ", contextId=" + context.getId() +
                '}';
    }
}
