package com.devoxx.genie.service.ap;

/**
 * Thrown by {@link ApCliService} when the {@code ap} binary cannot be invoked,
 * exits non-zero, times out, or returns output that cannot be parsed.
 */
public class ApCliException extends Exception {

    public ApCliException(String message) {
        super(message);
    }

    public ApCliException(String message, Throwable cause) {
        super(message, cause);
    }
}
