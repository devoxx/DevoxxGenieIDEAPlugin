package com.devoxx.genie.service.security;

public class SecurityScanException extends Exception {

    public SecurityScanException(String message) {
        super(message);
    }

    public SecurityScanException(String message, Throwable cause) {
        super(message, cause);
    }
}
