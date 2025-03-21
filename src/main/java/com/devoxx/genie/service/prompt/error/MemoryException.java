package com.devoxx.genie.service.prompt.error;

/**
 * Exception for memory-related errors.
 */
public class MemoryException extends PromptException {
    public MemoryException(String message) {
        super(message, ErrorSeverity.ERROR, false);
    }

    public MemoryException(String message, Throwable cause) {
        super(message, cause, ErrorSeverity.ERROR, false);
    }
}
