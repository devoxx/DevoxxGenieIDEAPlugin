package com.devoxx.genie.service.prompt.error;

/**
 * Exception for web search related errors.
 */
public class WebSearchException extends PromptException {
    public WebSearchException(String message) {
        super(message, ErrorSeverity.WARNING, true);
    }

    public WebSearchException(String message, Throwable cause) {
        super(message, cause, ErrorSeverity.WARNING, true);
    }
}
