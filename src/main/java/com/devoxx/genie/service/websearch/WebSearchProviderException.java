package com.devoxx.genie.service.websearch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A web search provider failure with a message written for the user (e.g. "Keenable's free search limit was
 * reached"). The message never contains credentials or raw response bodies, so callers may show it as-is;
 * other exceptions from providers should not be shown verbatim.
 */
public class WebSearchProviderException extends RuntimeException {

    public enum Kind { RATE_LIMITED, UNAUTHORIZED, FAILED }

    private final Kind kind;

    public WebSearchProviderException(@NotNull Kind kind, @NotNull String userMessage, @Nullable Throwable cause) {
        super(userMessage, cause);
        this.kind = kind;
    }

    public @NotNull Kind kind() {
        return kind;
    }
}
