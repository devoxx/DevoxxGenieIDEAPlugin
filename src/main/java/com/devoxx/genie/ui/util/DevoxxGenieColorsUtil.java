package com.devoxx.genie.ui.util;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;

import java.awt.*;

public class DevoxxGenieColorsUtil {

    private static final int LIGHT_ALPHA = 100;
    private static final int DARK_ALPHA = 70;

    public static final Color GRAY_REGULAR = Gray._100;
    public static final Color GRAY_DARK = Gray._85; // Darker for dark theme if desired

    public static final Color PROMPT_TEXT_COLOR = new JBColor(
            new Color(0, 0, 0),                // Light theme
            new Color(255, 255, 255)            // Dark theme
    );

    public static final Color HOVER_BG_COLOR = new JBColor(
            new Color(180, 180, 180, 50),        // Light theme
            new Color(60, 60, 60, DARK_ALPHA)       // Dark theme
    );

    public static final Color CODE_BORDER_BG_COLOR = new JBColor(
            new Color(160, 160, 160, LIGHT_ALPHA),  // Light theme
            new Color(80, 80, 80, DARK_ALPHA)       // Dark theme
    );

    public static final Color PROMPT_BG_COLOR = new JBColor(
            Gray._250,                        // Light theme
            Gray._43     // Dark theme
    );

    public static final Color CODE_BG_COLOR = new JBColor(
            new Color(240, 240, 240, LIGHT_ALPHA), // Light theme
            new Color(45, 45, 45, LIGHT_ALPHA)     // Dark theme
    );

    public static final Color PROMPT_INPUT_BORDER = new JBColor(
            new Color(0, 122, 204),        // Light theme
            new Color(75, 110, 175)        // Dark theme
    );

    public static final Color GRAY_COLOR = new JBColor(GRAY_REGULAR, GRAY_DARK);
}
