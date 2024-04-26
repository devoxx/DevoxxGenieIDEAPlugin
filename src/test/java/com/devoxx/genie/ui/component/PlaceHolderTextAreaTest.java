package com.devoxx.genie.ui.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceHolderTextAreaTest {

    @Test
    void testPlaceholderTextArea() {
         PlaceholderTextArea placeholderTextArea = new PlaceholderTextArea();
         placeholderTextArea.setPlaceholder("Test");

         assertTrue(placeholderTextArea.getPlaceholder().equals("Test"));
    }
}
