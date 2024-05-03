package com.devoxx.genie.ui.util;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;

import java.awt.*;

public class DevoxxGenieColors {

    public static final Color GRAY_REGULAR = Gray._100;
    public static final Color GRAY_DARK = Gray._85; // Darker for dark theme if desired

    public static final Color TRANSPARENT_COLOR = new JBColor(new Color(0, 0, 0, 0), new Color(0, 0, 0, 0));
    public static final Color HOVER_BG_COLOR = new JBColor(new Color(192, 192, 192, 50), new Color(192, 192, 192, 50));

    public static final Color DEFAULT_BG_COLOR = new JBColor(new Color(61, 63, 66), new Color(45, 48, 52));

    public static final Color PROMPT_BG_COLOR = new JBColor(new Color(42, 45, 48), new Color(33, 36, 39));

    public static final Color PROMPT_INPUT_BORDER = new JBColor(new Color(37, 150, 190), new Color(28, 141, 181));

    public static final Color WARNING_BG_COLOR = new JBColor(Color.RED, new Color(255, 69, 0)); // Making dark theme slightly different

    public static final Color INPUT_AREA_BORDER = PROMPT_INPUT_BORDER; // Reuse if same as PROMPT_INPUT_BORDER

    private static final Color LIGHT_GRAY_REGULAR = new Color(56, 59, 64);
    private static final Color LIGHT_GRAY_DARK = new Color(45, 48, 52); // Slightly darker for dark theme

    public static final Color GRAY_COLOR = new JBColor(GRAY_REGULAR, GRAY_DARK);
    public static final Color LIGHT_GRAY_COLOR = new JBColor(LIGHT_GRAY_REGULAR, LIGHT_GRAY_DARK);
}
