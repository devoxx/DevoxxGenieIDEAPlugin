package com.devoxx.genie.ui.settings.common;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * What one Settings test search produced: results with a summary line, or a message saying why
 * there are none. Built off the EDT and handed to {@link SearchTestPanel} for display.
 *
 * @param kind    how the panel styles the status line
 * @param message the status line text, written for the user (never contains credentials)
 * @param items   the results, best first; empty unless {@code kind == RESULTS}
 */
public record SearchTestOutcome<T>(@NotNull Kind kind, @NotNull String message, @NotNull List<T> items) {

    public enum Kind {
        /** At least one result; {@code message} summarises them. */
        RESULTS,
        /** The search worked but found nothing. */
        EMPTY,
        /** The search could not run or failed. */
        ERROR
    }

    public SearchTestOutcome {
        items = List.copyOf(items);
    }

    public static <T> @NotNull SearchTestOutcome<T> results(@NotNull String summary, @NotNull List<T> items) {
        return items.isEmpty()
                ? new SearchTestOutcome<>(Kind.EMPTY, summary, List.of())
                : new SearchTestOutcome<>(Kind.RESULTS, summary, items);
    }

    public static <T> @NotNull SearchTestOutcome<T> empty(@NotNull String message) {
        return new SearchTestOutcome<>(Kind.EMPTY, message, List.of());
    }

    public static <T> @NotNull SearchTestOutcome<T> error(@NotNull String message) {
        return new SearchTestOutcome<>(Kind.ERROR, message, List.of());
    }
}
