package com.devoxx.genie.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDateTimeConverterTest {

    private final LocalDateTimeConverter converter = new LocalDateTimeConverter();

    // --- fromString tests ---

    @Test
    void fromString_validIsoDateTime_returnsLocalDateTime() {
        String input = "2024-03-15T10:30:45";
        LocalDateTime result = converter.fromString(input);

        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(3);
        assertThat(result.getDayOfMonth()).isEqualTo(15);
        assertThat(result.getHour()).isEqualTo(10);
        assertThat(result.getMinute()).isEqualTo(30);
        assertThat(result.getSecond()).isEqualTo(45);
    }

    @Test
    void fromString_dateTimeWithSubSeconds_returnsLocalDateTime() {
        String input = "2024-01-01T00:00:00.123456789";
        LocalDateTime result = converter.fromString(input);

        assertThat(result).isNotNull();
        assertThat(result.getNano()).isEqualTo(123456789);
    }

    @Test
    void fromString_midnightDateTime_returnsCorrectTime() {
        String input = "2024-12-31T00:00:00";
        LocalDateTime result = converter.fromString(input);

        assertThat(result).isNotNull();
        assertThat(result.getHour()).isZero();
        assertThat(result.getMinute()).isZero();
        assertThat(result.getSecond()).isZero();
    }

    @Test
    void fromString_endOfDay_returnsCorrectTime() {
        String input = "2024-06-15T23:59:59";
        LocalDateTime result = converter.fromString(input);

        assertThat(result).isNotNull();
        assertThat(result.getHour()).isEqualTo(23);
        assertThat(result.getMinute()).isEqualTo(59);
        assertThat(result.getSecond()).isEqualTo(59);
    }

    @Test
    void fromString_invalidFormat_throwsException() {
        assertThatThrownBy(() -> converter.fromString("not-a-date"))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void fromString_dateOnly_throwsException() {
        assertThatThrownBy(() -> converter.fromString("2024-03-15"))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void fromString_emptyString_throwsException() {
        assertThatThrownBy(() -> converter.fromString(""))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void fromString_dateWithSpaceInsteadOfT_throwsException() {
        assertThatThrownBy(() -> converter.fromString("2024-03-15 10:30:45"))
                .isInstanceOf(DateTimeParseException.class);
    }

    // --- toString tests ---

    @Test
    void toString_validLocalDateTime_returnsIsoString() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 15, 10, 30, 45);
        String result = converter.toString(dateTime);

        assertThat(result).isEqualTo("2024-03-15T10:30:45");
    }

    @Test
    void toString_midnight_returnsCorrectString() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        String result = converter.toString(dateTime);

        assertThat(result).isEqualTo("2024-01-01T00:00:00");
    }

    @Test
    void toString_withNanos_includesNanosInString() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 12, 0, 0, 123456789);
        String result = converter.toString(dateTime);

        assertThat(result).isEqualTo("2024-06-15T12:00:00.123456789");
    }

    @Test
    void toString_endOfDay_returnsCorrectString() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        String result = converter.toString(dateTime);

        assertThat(result).isEqualTo("2024-12-31T23:59:59");
    }

    // --- Round-trip tests ---

    @Test
    void roundTrip_fromStringThenToString_preservesValue() {
        String original = "2024-07-20T14:30:00";
        LocalDateTime parsed = converter.fromString(original);
        String formatted = converter.toString(parsed);

        assertThat(formatted).isEqualTo(original);
    }

    @Test
    void roundTrip_toStringThenFromString_preservesValue() {
        LocalDateTime original = LocalDateTime.of(2024, 11, 5, 8, 15, 30);
        String formatted = converter.toString(original);
        LocalDateTime parsed = converter.fromString(formatted);

        assertThat(parsed).isEqualTo(original);
    }
}
