package com.devoxx.genie.ui.util;

import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Utility class for standardized font handling across the application
 */
public class DevoxxGenieFontsUtil {
    private DevoxxGenieFontsUtil() {
    }

    public static final String SOURCE_CODE_PRO_FONT = "Source Code Pro";
    public static final String DROPDOWN_FONT_FAMILY = Font.DIALOG;
    public static final int DROPDOWN_FONT_SIZE = 14; // Increased base size for better readability

    /**
     * Get a standardized scaled font for dropdown menus that respects IDE scaling
     * @return The font to use for dropdown menus
     */
    public static @NotNull Font getDropdownFont() {
        // We want to ensure the same size is used across all UI components
        int scaledSize = JBUI.scale(DROPDOWN_FONT_SIZE);
        return new Font(DROPDOWN_FONT_FAMILY, Font.PLAIN, scaledSize);
    }
    
    /**
     * Get a standardized scaled font for secondary dropdown text (like token counts)
     * @return The font to use for secondary information in dropdowns
     */
    public static @NotNull Font getDropdownInfoFont() {
        // Slightly smaller than the main dropdown font
        int scaledSize = JBUI.scale(DROPDOWN_FONT_SIZE - 1);
        return new Font(DROPDOWN_FONT_FAMILY, Font.PLAIN, scaledSize);
    }
}
