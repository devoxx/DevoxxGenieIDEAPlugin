package com.devoxx.genie.service.acp.protocol.exception;

/**
 * Exception thrown when ACP connection or initialization fails.
 */
public class AcpConnectionException extends AcpException {

    public AcpConnectionException(String message) {
        super(message);
    }

    public AcpConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
