package com.devoxx.genie.ui.util;

import com.intellij.util.messages.Topic;

/**
 * Interface for notifying components about theme changes in the IDE.
 */
public interface ThemeChangeNotifier {
    
    /**
     * Message bus topic for theme change events.
     */
    Topic<ThemeChangeNotifier> THEME_CHANGED_TOPIC = Topic.create("DevoxxGenie Theme Changed", ThemeChangeNotifier.class);
    
    /**
     * Called when the IDE theme changes.
     * 
     * @param isDarkTheme true if the new theme is dark, false if it's light
     */
    void themeChanged(boolean isDarkTheme);
}
