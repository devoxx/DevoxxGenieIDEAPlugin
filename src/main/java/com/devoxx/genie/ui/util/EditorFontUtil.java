package com.devoxx.genie.ui.util;

import com.intellij.openapi.editor.colors.EditorColorsManager;

/**
 * Utility class for getting editor font information.
 */
public class EditorFontUtil {

    private static final int DEFAULT_FONT_SIZE = 12;

    private EditorFontUtil() {
        /* This utility class should not be instantiated */
    }

    /**
     * Gets the editor font size, falling back to a default if the EditorColorsManager is not available.
     *
     * @return the editor font size
     */
    public static int getEditorFontSize() {
        EditorColorsManager manager = EditorColorsManager.getInstance();
        if (manager != null) {
            return manager.getGlobalScheme().getEditorFontSize();
        }
        return DEFAULT_FONT_SIZE;
    }
}
