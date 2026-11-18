package com.devoxx.genie.service.websearch;

import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Marks a web search provider whose upstream API is being shut down (TASK-277.4), so settings can warn and
 * search failures can explain why. Messages switch from "will be discontinued" to "was discontinued" on
 * {@link #endDate()}.
 *
 * @param endDate      the day the provider's API stops working
 * @param detailsUrl   the provider's own announcement
 * @param alternatives which registered providers to use instead, as a phrase (e.g. "Keenable or Tavily")
 */
public record WebSearchDeprecation(@NotNull LocalDate endDate,
                                   @NotNull String detailsUrl,
                                   @NotNull String alternatives) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    public boolean isPast(@NotNull Clock clock) {
        return !LocalDate.now(clock).isBefore(endDate);
    }

    public @NotNull String formattedEndDate() {
        return DATE.format(endDate);
    }

    /** The settings warning shown while the provider is selected. */
    public @NotNull String warning(@NotNull String providerName, @NotNull Clock clock) {
        return isPast(clock)
                ? providerName + " was discontinued on " + formattedEndDate() + " and no longer works. "
                  + "Switch to " + alternatives + "."
                : providerName + " is deprecated: its API will be discontinued on " + formattedEndDate() + ". "
                  + "Switch to " + alternatives + " before then.";
    }

    /** The message for any search failure of the deprecated provider; safe to show (no response details). */
    public @NotNull String failureMessage(@NotNull String providerName, @NotNull String failureType,
                                          @NotNull Clock clock) {
        if (isPast(clock)) {
            return providerName + " was discontinued on " + formattedEndDate() + ". Choose another provider ("
                   + alternatives + ") in " + WebSearchReadiness.SETTINGS_PATH + ".";
        }
        return providerName + " failed (" + failureType + "). Check the API key and Search Engine ID in "
               + WebSearchReadiness.SETTINGS_PATH + ". Note: its API will be discontinued on " + formattedEndDate()
               + "; consider switching to " + alternatives + ".";
    }
}
