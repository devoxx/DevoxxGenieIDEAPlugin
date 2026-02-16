package com.devoxx.genie.service.acp.protocol.exception;

/**
 * Exception thrown when a terminal creation operation fails in the ACP protocol.
 */
public class TerminalCreateException extends AcpException {

    public TerminalCreateException(String message) {
        super(message);
    }

    public TerminalCreateException(String message, Throwable cause) {
        super(message, cause);
    }
}