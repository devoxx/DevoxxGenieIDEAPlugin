package com.devoxx.genie.service.prompt.error;

import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.cancellation.PromptCancellationService;
import com.devoxx.genie.ui.compose.model.TerminalState;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Centralized error handler for all prompt-related errors.
 * Provides consistent logging, notification, and recovery strategies.
 */
@Slf4j
public class PromptErrorHandler {

    /** Maximum length of the error summary shown inline in the chat; full detail stays in idea.log. */
    private static final int MAX_INLINE_ERROR_LENGTH = 300;

    /**
     * Handle a prompt exception with context information.
     * 
     * @param project The current project
     * @param exception The exception to handle
     * @param chatMessageContext Optional context for the error
     */
    public static void handleException(@NotNull Project project, @NotNull Throwable exception, @Nullable ChatMessageContext chatMessageContext) {
        // Convert to PromptException if it's not already one
        final PromptException promptException = convertToPromptException(exception);
        
        // Log the error with appropriate severity
        logException(promptException);
        
        // Show notification if user-visible
        if (promptException.isUserVisible()) {
            showNotification(project, promptException);
        }

        // Durable in-chat record: mark the message with an ERROR terminal state so the
        // conversation shows an inline error card (with Retry) that outlives the toast.
        updateChatTerminalState(project, promptException, chatMessageContext);

        // Delegate to global error handler for tracking/reporting
        ErrorHandler.handleError(project, promptException);
        
        // Perform any necessary recovery based on exception type
        performRecovery(promptException, chatMessageContext);
    }
    
    /**
     * Simplified version without chat context
     */
    public static void handleException(@NotNull Project project, @NotNull Throwable exception) {
        handleException(project, exception, null);
    }
    
    /**
     * Derives the human-readable, truncated error summary for inline chat display.
     * Reuses the exact same classification as the toast ({@link #convertToPromptException})
     * — no second classification layer. Long provider stack traces are truncated; the
     * full detail remains in idea.log.
     */
    public static @NotNull String userFacingMessage(@NotNull Throwable exception) {
        String message = convertToPromptException(exception).getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        if (message.length() > MAX_INLINE_ERROR_LENGTH) {
            message = message.substring(0, MAX_INLINE_ERROR_LENGTH) + "…";
        }
        return message;
    }

    /**
     * Writes the ERROR terminal state onto the chat message of the failed execution.
     * The panel is looked up via the cancellation service registry (still registered at
     * error time); when no panel is found (headless paths, already unregistered) this is
     * a silent no-op. The view model guarantees terminal states are final, so calling
     * this after a STOPPED marker has no effect.
     */
    private static void updateChatTerminalState(@NotNull Project project,
                                                @NotNull PromptException exception,
                                                @Nullable ChatMessageContext chatMessageContext) {
        if (chatMessageContext == null) {
            return;
        }
        try {
            PromptOutputPanel panel = PromptCancellationService.getInstance()
                    .getRegisteredPanel(project, chatMessageContext.getId());
            if (panel != null && panel.getConversationPanel() != null
                    && panel.getConversationPanel().viewController != null) {
                panel.getConversationPanel().viewController.setTerminalState(
                        chatMessageContext.getId(), TerminalState.ERROR, userFacingMessage(exception));
            }
        } catch (Exception e) {
            log.debug("Could not set ERROR terminal state in chat", e);
        }
    }

    /**
     * Convert any exception to a PromptException
     */
    private static PromptException convertToPromptException(Throwable exception) {
        if (exception instanceof PromptException promptException) {
            return promptException;
        }
        
        // Map known exception types to appropriate PromptExceptions
        if (exception instanceof com.devoxx.genie.service.exception.ModelNotActiveException) {
            return new ModelException(exception.getMessage(), exception);
        } else if (exception instanceof com.devoxx.genie.service.exception.ProviderUnavailableException) {
            return new ModelException("Provider unavailable: " + exception.getMessage(), exception);
        } else if (exception instanceof java.util.concurrent.TimeoutException) {
            return new ExecutionException("Request timed out", exception, 
                    PromptException.ErrorSeverity.WARNING, true);
        } else {
            // Default to ExecutionException for unknown exceptions
            return new ExecutionException("Execution error: " + exception.getMessage(), exception);
        }
    }
    
    /**
     * Log the exception with appropriate severity
     */
    private static void logException(@NotNull PromptException exception) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        
        switch (exception.getSeverity()) {
            case INFO:
                log.info(message, cause);
                break;
            case WARNING:
                log.warn(message, cause);
                break;
            case ERROR:
            case CRITICAL:
                log.error(message, cause);
                break;
        }
    }
    
    /**
     * Show notification to the user if needed
     */
    private static void showNotification(Project project, PromptException exception) {
        ApplicationManager.getApplication().invokeLater(() -> {
            NotificationUtil.sendNotification(project, exception.getMessage());
        });
    }
    
    /**
     * Perform recovery actions based on exception type
     */
    private static void performRecovery(PromptException exception, @Nullable ChatMessageContext chatMessageContext) {
        // Skip recovery if context is null
        if (chatMessageContext == null) {
            return;
        }
        
        // Perform type-specific recovery
        if (exception instanceof MemoryException) {
            // For memory exceptions, clear the problematic exchange
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    com.devoxx.genie.service.prompt.memory.ChatMemoryManager.getInstance()
                        .removeLastExchange(chatMessageContext);
                } catch (Exception e) {
                    log.warn("Error during memory recovery", e);
                }
            });
        }
    }
}
