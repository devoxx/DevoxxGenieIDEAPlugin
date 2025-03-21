package com.devoxx.genie.service.prompt.error;

/**
 * Exception for chat memory-related errors.
 * This exception type is used for any errors related to the management,
 * retrieval, or modification of conversation history.
 */
public class MemoryException extends PromptException {

    /**
     * Creates a new memory exception with the specified message.
     * By default, memory exceptions are ERROR severity but not user-visible.
     *
     * @param message Error message
     */
    public MemoryException(String message) {
        super(message, ErrorSeverity.ERROR, false);
    }

    /**
     * Creates a new memory exception with the specified message and cause.
     * By default, memory exceptions are ERROR severity but not user-visible.
     *
     * @param message Error message
     * @param cause The cause of this exception
     */
    public MemoryException(String message, Throwable cause) {
        super(message, cause, ErrorSeverity.ERROR, false);
    }

    /**
     * Creates a new memory exception with the specified message, cause and user visibility.
     *
     * @param message Error message
     * @param cause The cause of this exception
     * @param severity The severity level of this exception
     * @param userVisible Whether this exception should be shown to the user
     */
    public MemoryException(String message, Throwable cause, ErrorSeverity severity, boolean userVisible) {
        super(message, cause, severity, userVisible);
    }

    /**
     * Creates a new memory exception with the specified message and user visibility.
     *
     * @param message Error message
     * @param severity The severity level of this exception
     * @param userVisible Whether this exception should be shown to the user
     */
    public MemoryException(String message, ErrorSeverity severity, boolean userVisible) {
        super(message, severity, userVisible);
    }
}