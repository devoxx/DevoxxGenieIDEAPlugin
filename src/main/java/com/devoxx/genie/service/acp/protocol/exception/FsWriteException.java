package com.devoxx.genie.service.acp.protocol.exception;

/**
 * Exception thrown when a file write operation fails in the ACP protocol.
 */
public class FsWriteException extends AcpException {

    public FsWriteException(String message) {
        super(message);
    }

    public FsWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}