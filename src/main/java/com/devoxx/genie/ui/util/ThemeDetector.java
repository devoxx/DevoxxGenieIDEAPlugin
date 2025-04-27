package com.devoxx.genie.ui.util;

import com.intellij.ui.JBColor;
import java.awt.Color;

/**
 * Utility class for detecting the current IDE theme (light/dark).
 */
public class ThemeDetector {

    /**
     * Check if the current IDE theme is dark.
     * 
     * @return true if the IDE is using a dark theme, false otherwise
     */
    public static boolean isDarkTheme() {
        // Use JBColor.isBright() which is the recommended way to check theme status
        // This is a stable API that's unlikely to change
        return !JBColor.isBright();  // If JBColor is NOT bright, it's dark theme
    }
    
    /**
     * Determine if a color is considered dark.
     * 
     * @param color The color to check
     * @return true if the color is dark, false otherwise
     */
    public static boolean isDarkColor(Color color) {
        // Calculate the perceived brightness 
        double brightness = (0.299 * color.getRed() + 
                           0.587 * color.getGreen() + 
                           0.114 * color.getBlue()) / 255.0;
        
        // If brightness is less than 0.5, it's considered dark
        return brightness < 0.5;
    }
}
