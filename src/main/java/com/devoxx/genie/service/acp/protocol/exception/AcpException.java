package com.devoxx.genie.service.acp.protocol.exception;

/**
 * Base exception for all ACP (Agent Communication Protocol) errors.
 */
public class AcpException extends Exception {

    public AcpException(String message) {
        super(message);
    }

    public AcpException(String message, Throwable cause) {
        super(message, cause);
    }
}
