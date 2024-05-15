package com.devoxx.genie.ui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DoubleConverterTest {

    private final DoubleConverter converter = new DoubleConverter();

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

    @Test
    void testToString_validDouble_returnsFormattedString() {
        Double value = 3.14;
        String expected = "3.14";
        String result = converter.toString(value);
        assertEquals(expected, result);
    }

}
