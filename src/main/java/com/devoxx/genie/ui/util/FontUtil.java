package com.devoxx.genie.ui.util;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.ui.scale.JBUIScale;

public class FontUtil {

    private FontUtil() {}

    public static float getFontSize() {
        EditorColorsManager editorColorsManager = EditorColorsManager.getInstance();
        if (editorColorsManager == null) {
            // Default font size when running in test environment
            return JBUIScale.scale(12.0f);
        }
        
        EditorColorsScheme scheme = editorColorsManager.getGlobalScheme();
        float fontSize = scheme.getEditorFontSize(); // returns int (size in points)
        float scaleFactor = JBUIScale.scale(1.0f); // returns scale-adjusted value
        return fontSize * scaleFactor;
    }
}
