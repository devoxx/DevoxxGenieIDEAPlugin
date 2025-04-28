package com.devoxx.genie.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for thread-related operations.
 */
@Slf4j
public class ThreadUtils {

    private ThreadUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Sleeps for the specified duration in milliseconds.
     * Handles InterruptedException by restoring the interrupt flag.
     *
     * @param millis the duration to sleep in milliseconds
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Thread interrupted during sleep", e);
        }
    }

    /**
     * Sleeps for the specified duration in milliseconds.
     * Handles InterruptedException with a custom error message.
     *
     * @param millis the duration to sleep in milliseconds
     * @param errorMessage the error message to log if interrupted
     */
    public static void sleep(long millis, String errorMessage) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(errorMessage, e);
        }
    }
}