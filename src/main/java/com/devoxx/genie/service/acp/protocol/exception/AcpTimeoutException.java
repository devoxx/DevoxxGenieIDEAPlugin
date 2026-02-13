package com.devoxx.genie.service.acp.protocol.exception;

/**
 * Exception thrown when an ACP request times out.
 */
public class AcpTimeoutException extends AcpException {

    public AcpTimeoutException(String message) {
        super(message);
    }

    public AcpTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
