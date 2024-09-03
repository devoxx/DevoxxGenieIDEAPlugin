package com.devoxx.genie.util;

import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeConverter extends Converter<LocalDateTime> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public @Nullable LocalDateTime fromString(@NotNull String value) {
        return LocalDateTime.parse(value, formatter);
    }

    @Override
    public @Nullable String toString(@NotNull LocalDateTime value) {
        return value.format(formatter);
    }
}
