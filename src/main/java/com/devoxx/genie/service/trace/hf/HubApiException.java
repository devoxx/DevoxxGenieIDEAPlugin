package com.devoxx.genie.service.trace.hf;

import org.jetbrains.annotations.NotNull;

/** A failed Hugging Face Hub API call, with a message fit to show the user. Never contains the token. */
public class HubApiException extends Exception {

    private final int statusCode;

    public HubApiException(int statusCode, @NotNull String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HubApiException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    /** HTTP status, or -1 for network errors. */
    public int statusCode() {
        return statusCode;
    }
}
