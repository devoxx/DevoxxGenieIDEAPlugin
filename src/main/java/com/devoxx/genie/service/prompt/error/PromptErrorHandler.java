package com.devoxx.genie.service.prompt.error;

import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.request.ChatMessageContext;
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
