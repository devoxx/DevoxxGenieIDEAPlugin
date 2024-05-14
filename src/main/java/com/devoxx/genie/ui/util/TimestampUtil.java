package com.devoxx.genie.ui.util;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimestampUtil {

    /**
     * Get the current timestamp.
     * @return the current timestamp
     */
    public static @NotNull String getCurrentTimestamp() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM ''yy HH:mm");
        return dateTime.format(formatter);
    }
}
