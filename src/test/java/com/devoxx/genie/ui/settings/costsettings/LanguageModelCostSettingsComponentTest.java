package com.devoxx.genie.ui.settings.costsettings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LanguageModelCostSettingsComponentTest {

    @Test
    public void testGetContextWindowWithInteger() {
        Object contextWindowObj = 8000;
        int result = LanguageModelCostSettingsComponent.getContextWindow(contextWindowObj);
        assertEquals(8000, result);
    }

    @Test
    public void testGetContextWindowWithString() {
        Object contextWindowObj = "8,000";
        int result = LanguageModelCostSettingsComponent.getContextWindow(contextWindowObj);
        assertEquals(8000, result);
    }

    @Test
    public void testGetContextWindowWithStringAndSpace() {
        Object contextWindowObj = "8 000";
        int result = LanguageModelCostSettingsComponent.getContextWindow(contextWindowObj);
        assertEquals(8000, result);
    }

    @Test
    public void testGetContextWindowWithIntegerValue() {
        Integer contextWindowObj = 8_000;
        int result = LanguageModelCostSettingsComponent.getContextWindow(contextWindowObj);
        assertEquals(8000, result);
    }

    @Test
    public void testGetContextWindowWithInvalidString() {
        Object contextWindowObj = "invalid";
        assertThrows(NumberFormatException.class, () -> {
            LanguageModelCostSettingsComponent.getContextWindow(contextWindowObj);
        });
    }

    @Test
    public void testGetContextWindowWithUnexpectedType() {
        Object contextWindowObj = new Object();
        assertThrows(IllegalArgumentException.class, () -> {
            LanguageModelCostSettingsComponent.getContextWindow(contextWindowObj);
        });
    }
}
