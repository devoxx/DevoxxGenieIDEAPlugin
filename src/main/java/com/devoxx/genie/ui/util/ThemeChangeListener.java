package com.devoxx.genie.ui.util;

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for IDE theme changes to update plugin UI components.
 * This ensures the web views refresh when the IDE theme changes
 * between light and dark modes.
 */
public class ThemeChangeListener implements LafManagerListener {

    /**
     * Register the theme change listener with the IDE.
     */
    public static void register() {
        ApplicationManager.getApplication().getMessageBus()
                .connect()
                .subscribe(LafManagerListener.TOPIC, new ThemeChangeListener());
    }

    /**
     * Called when the Look and Feel (theme) changes in the IDE.
     * Refresh all open web views to match the new theme.
     *
     * @param source The LafManager that triggered the event
     */
    @Override
    public void lookAndFeelChanged(@NotNull LafManager source) {
        // Force theme to be re-detected
        boolean isDarkTheme = ThemeDetector.isDarkTheme();
        
        // Refresh any open projects' web views
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) {
                continue;
            }
            
            // Publish a theme change event that other components can listen for
            project.getMessageBus().syncPublisher(ThemeChangeNotifier.THEME_CHANGED_TOPIC)
                    .themeChanged(isDarkTheme);
        }
    }
}
