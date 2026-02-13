package com.devoxx.genie.service.acp.protocol.exception;

/**
 * Exception thrown when an ACP JSON-RPC request fails due to an execution error.
 *
 * <p>This differs from {@link AcpTimeoutException} in that the request was sent
 * and a failure occurred during execution, rather than a timeout waiting for a response.
 */
public class AcpRequestException extends AcpException {

    public AcpRequestException(String message) {
        super(message);
    }

    public AcpRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
