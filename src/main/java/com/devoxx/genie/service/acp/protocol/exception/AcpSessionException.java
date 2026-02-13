package com.devoxx.genie.service.acp.protocol.exception;

/**
 * Exception thrown when ACP session operations fail.
 */
public class AcpSessionException extends AcpException {

    public AcpSessionException(String message) {
        super(message);
    }

    public AcpSessionException(String message, Throwable cause) {
        super(message, cause);
    }
}
