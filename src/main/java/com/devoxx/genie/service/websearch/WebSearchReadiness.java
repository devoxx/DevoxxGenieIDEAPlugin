package com.devoxx.genie.service.websearch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Whether a {@link WebSearchConfig} can run a search, and if not, a user-facing reason.
 *
 * @param status        the outcome
 * @param provider      the selected provider, or null
 * @param missingFields labels of required credentials that are blank
 */
public record WebSearchReadiness(@NotNull Status status,
                                 @Nullable WebSearchProvider provider,
                                 @NotNull List<String> missingFields) {

    public static final String SETTINGS_PATH = "Settings → DevoxxGenie → Web search";

    public enum Status { READY, NO_PROVIDER, MISSING_CREDENTIALS }

    public WebSearchReadiness {
        missingFields = List.copyOf(missingFields);
    }

    public boolean isReady() {
        return status == Status.READY;
    }

    /** A message naming the problem and where to fix it; empty when ready. */
    public @NotNull String message() {
        return switch (status) {
            case READY -> "";
            case NO_PROVIDER -> "No web search provider is selected. Choose one in " + SETTINGS_PATH + ".";
            case MISSING_CREDENTIALS -> {
                String name = provider == null ? "The web search provider"
                        : WebSearchService.definition(provider).displayName();
                yield name + " is missing its " + String.join(" and ", missingFields)
                        + ". Enter it in " + SETTINGS_PATH + ".";
            }
        };
    }
}
