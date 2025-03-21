package com.devoxx.genie.service.prompt.error;

/**
 * Exception for model-related errors (e.g., model not active, API key missing).
 */
public class ModelException extends PromptException {
    public ModelException(String message) {
        super(message, ErrorSeverity.ERROR, true);
    }

    public ModelException(String message, Throwable cause) {
        super(message, cause, ErrorSeverity.ERROR, true);
    }
}
