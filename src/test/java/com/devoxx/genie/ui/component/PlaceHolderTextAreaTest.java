package com.devoxx.genie.ui.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceHolderTextAreaTest {

    @Test
    void testPlaceholderTextArea() {
         PlaceholderTextArea placeholderTextArea = new PlaceholderTextArea(1, 1);
         placeholderTextArea.setPlaceholder("Test");

         assertTrue(placeholderTextArea.getPlaceholder().equals("Test"));
    }
}
