package com.devoxx.genie.ui.util;

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.ide.ui.laf.UIThemeLookAndFeelInfo;
import com.intellij.openapi.application.ApplicationManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for detecting the current IDE theme (light/dark).
 * Includes real-time notification when the theme changes.
 */
public class ThemeDetector {
    private static final List<ThemeChangeListener> themeChangeListeners = new ArrayList<>();
    private static boolean isDarkThemeValue;

    static {
        // Initialize the current theme value
        isDarkThemeValue = detectCurrentThemeDark();
        
        // Register a listener for theme changes with the LAF manager
        var application = ApplicationManager.getApplication();
        if (application != null) {
            application.getMessageBus().connect()
                .subscribe(LafManagerListener.TOPIC, (LafManagerListener) source -> {
                    boolean newIsDarkTheme = detectCurrentThemeDark();
                    // Only notify if the theme type (dark/light) has actually changed
                    if (newIsDarkTheme != isDarkThemeValue) {
                        isDarkThemeValue = newIsDarkTheme;
                        notifyThemeChangeListeners(isDarkThemeValue);
                    }
                });
        }
    }

    public static void addThemeChangeListener(ThemeChangeListener listener) {
        themeChangeListeners.add(listener);
    }

    public static void removeThemeChangeListener(ThemeChangeListener listener) {
        themeChangeListeners.remove(listener);
    }

    /**
     * Check if the current IDE theme is dark.
     * 
     * @return true if the IDE is using a dark theme, false otherwise
     */
    public static boolean isDarkTheme() {
        return isDarkThemeValue;
    }

    private static boolean detectCurrentThemeDark() {
        try {
            UIThemeLookAndFeelInfo currentTheme = LafManager.getInstance().getCurrentUIThemeLookAndFeel();
            return currentTheme != null && currentTheme.isDark();
        } catch (Exception | NoClassDefFoundError e) {
            // No IntelliJ Application available (plain unit tests, headless tooling):
            // fall back to light theme instead of failing class initialization.
            return false;
        }
    }
    
    /**
     * Notify all registered listeners that the theme has changed.
     * 
     * @param isDark true if the new theme is dark, false otherwise
     */
    private static void notifyThemeChangeListeners(boolean isDark) {
        for (ThemeChangeListener listener : themeChangeListeners) {
            listener.onThemeChanged(isDark);
        }
    }
    
    /**
     * Interface for listeners that want to be notified when the theme changes.
     */
    public interface ThemeChangeListener {
        /**
         * Called when the theme changes.
         * 
         * @param isDarkTheme true if the new theme is dark, false if it's light
         */
        void onThemeChanged(boolean isDarkTheme);
    }
}
