package com.devoxx.genie.ui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DoubleConverterTest {

    private final DoubleConverterUtil converter = new DoubleConverterUtil();

    @Test
    void testFromString_validDoubleWithDot_returnsDouble() {
        String value = "3.14";
        Double expected = 3.14;
        Double result = converter.fromString(value);
        assertEquals(expected, result);
    }

    @Test
    void testFromString_validDoubleWithComma_returnsDouble() {
        String value = "3,14";
        Double expected = 3.14;
        Double result = converter.fromString(value);
        assertEquals(expected, result);
    }

    @Test
    void testFromString_invalidDouble_throwsNumberFormatException() {
        String value = "invalid";
        assertThrows(NumberFormatException.class, () -> converter.fromString(value));
    }
}
