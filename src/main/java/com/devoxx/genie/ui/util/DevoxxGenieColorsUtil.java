package com.devoxx.genie.ui.util;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;

import java.awt.*;

public class DevoxxGenieColorsUtil {

    public static final Color GRAY_REGULAR = Gray._100;
    public static final Color GRAY_DARK = Gray._85; // Darker for dark theme if desired

    public static final Color TRANSPARENT_COLOR = new JBColor(new Color(0, 0, 0, 0), new Color(0, 0, 0, 0));

    public static final Color HOVER_BG_COLOR = new JBColor(new Color(192, 192, 192, 50), new Color(192, 192, 192, 50));

    public static final Color CODE_BORDER_BG_COLOR = new JBColor(new Color(192, 192, 192, 100), new Color(192, 192, 192, 50));

    public static final Color PROMPT_BG_COLOR = new JBColor(new Color(42, 45, 48), new Color(33, 36, 39));

    public static final Color CODE_BG_COLOR = new JBColor(new Color(211, 211, 211, 100), new Color(10, 10, 10, 100));

    public static final Color PROMPT_INPUT_BORDER = new JBColor(new Color(37, 150, 190), new Color(28, 141, 181));

    public static final Color GRAY_COLOR = new JBColor(GRAY_REGULAR, GRAY_DARK);
}
