package com.devoxx.genie.service.prompt.error;

/**
 * Exception for execution-related errors (e.g., timeout, execution failure).
 */
public class ExecutionException extends PromptException {
    public ExecutionException(String message) {
        super(message, ErrorSeverity.ERROR, false);
    }

    public ExecutionException(String message, Throwable cause) {
        super(message, cause, ErrorSeverity.ERROR, false);
    }

    public ExecutionException(String message, Throwable cause, ErrorSeverity severity, boolean userVisible) {
        super(message, cause, severity, userVisible);
    }
}
