package com.devoxx.genie.service.prompt.error;

/**
 * Exception for streaming-related errors.
 */
public class StreamingException extends PromptException {
    public StreamingException(String message) {
        super(message, ErrorSeverity.WARNING, false);
    }

    public StreamingException(String message, Throwable cause) {
        super(message, cause, ErrorSeverity.WARNING, false);
    }
}
