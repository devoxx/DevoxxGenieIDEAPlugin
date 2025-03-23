package com.devoxx.genie.service.prompt.error;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Base exception class for all prompt-related errors.
 * Provides a consistent way to handle and categorize errors in the prompt package.
 */
@Slf4j
@Getter
public class PromptException extends RuntimeException {
    private final ErrorSeverity severity;
    private final boolean userVisible;
    
    public enum ErrorSeverity {
        INFO,      // Informational, non-critical errors
        WARNING,   // Warnings that might affect functionality
        ERROR,     // Serious errors that prevent normal operation
        CRITICAL   // Critical errors that require immediate attention
    }

    public PromptException(String message, ErrorSeverity severity, boolean userVisible) {
        super(message);
        this.severity = severity;
        this.userVisible = userVisible;
        log.error("{}:{} - {}", severity, message, userVisible);
    }

    public PromptException(String message, Throwable cause, ErrorSeverity severity, boolean userVisible) {
        super(message, cause);
        this.severity = severity;
        this.userVisible = userVisible;
        log.error("{}:{} - {}", severity, message, userVisible);
    }
}
