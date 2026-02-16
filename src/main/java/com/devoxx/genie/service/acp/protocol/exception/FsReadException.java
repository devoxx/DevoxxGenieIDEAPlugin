package com.devoxx.genie.service.acp.protocol.exception;

/**
 * Exception thrown when a file read operation fails in the ACP protocol.
 */
public class FsReadException extends AcpException {

    public FsReadException(String message) {
        super(message);
    }

    public FsReadException(String message, Throwable cause) {
        super(message, cause);
    }
}