package com.devoxx.genie.ui.component;

import com.intellij.ui.ColorUtil;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StyleSheetUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.text.html.StyleSheet;

import static com.devoxx.genie.ui.util.DevoxxGenieFontsUtil.SOURCE_CODE_PRO_FONT;
import static com.devoxx.genie.ui.util.FontUtil.getFontSize;


public class StyleSheetsFactory {

    private StyleSheetsFactory() {}

    @Contract(" -> new")
    public static @NotNull StyleSheet createCodeStyleSheet() {
        return StyleSheetUtil.loadStyleSheet(
            "code, pre, .pre { " +
                "   font-family: '" + SOURCE_CODE_PRO_FONT + "'; " +
                "   font-size: " + getFontSize() + "pt;" +
                "}"
        );
    }

    @Contract(" -> new")
    public static @NotNull StyleSheet createParagraphStyleSheet() {
        float fontSize = getFontSize();

        return StyleSheetUtil.loadStyleSheet(
            "body { font-size: " + fontSize + "pt; }" +
                "p, li { font-size: " + fontSize + "pt; line-height: 125%; }" +
                "h6 { font-size: " + (fontSize + JBUIScale.scale(1)) + "pt; }" +
                "h5 { font-size: " + (fontSize + JBUIScale.scale(2)) + "pt; }" +
                "h4 { font-size: " + (fontSize + JBUIScale.scale(3)) + "pt; }" +
                "h3 { font-size: " + (fontSize + JBUIScale.scale(4)) + "pt; }" +
                "h2 { font-size: " + (fontSize + JBUIScale.scale(6)) + "pt; }" +
                "h1 { font-size: " + (fontSize + JBUIScale.scale(8)) + "pt; }" +
                "h1, h2, h3, h4, h5, h6, p, ul, ol { margin: 0 0 0 0; " + paragraphSpacing() + " }" +
                "ul { margin-left: " + JBUIScale.scale(10) + "pt; }" +
                "ol { margin-left: " + JBUIScale.scale(20) + "pt; }" +
                "li { padding: " + JBUIScale.scale(1) + "pt 0 " + JBUIScale.scale(2) + "pt 0; }" +
                "code, pre, .pre { " +
                "   font-family: '" + SOURCE_CODE_PRO_FONT + "'; " +
                "   font-size: " + fontSize + "pt;" +
                "   color: orange" +
                "}" +
                "hr {" +
                " margin: " + JBUIScale.scale(4) + "pt 0;" +
                " border-bottom: " + JBUIScale.scale(1) + "pt solid " + ColorUtil.toHtmlColor(UIUtil.getTooltipSeparatorColor()) + ";" +
                " width: 100%;" +
                "}" +
                "a { color: " + ColorUtil.toHtmlColor(JBUI.CurrentTheme.Link.Foreground.ENABLED) + "; text-decoration: none; }"
        );
    }

    private static @NotNull String paragraphSpacing() {
        return "padding: " + JBUIScale.scale(4) + "pt 0 " + JBUIScale.scale(4) + "pt 0";
    }
}
