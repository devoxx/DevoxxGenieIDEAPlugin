package com.devoxx.genie.service.websearch;

import org.jetbrains.annotations.NotNull;

/** Thrown when a search is attempted with no provider selected or with required credentials missing. */
public class WebSearchNotConfiguredException extends IllegalStateException {

    private final transient WebSearchReadiness readiness;

    public WebSearchNotConfiguredException(@NotNull WebSearchReadiness readiness) {
        super(readiness.message());
        this.readiness = readiness;
    }

    public @NotNull WebSearchReadiness readiness() {
        return readiness;
    }
}
