package com.devoxx.genie.ui.util;

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.ui.StartupUiUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listener for IDE theme changes to update plugin UI components.
 * This ensures the web views refresh when the IDE theme changes
 * between light and dark modes.
 */
public class ThemeChangeListener implements LafManagerListener {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    /**
     * Register the theme change listener with the IDE.
     */
    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
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
        ApplicationManager.getApplication().invokeLater(() -> {
            boolean isDarkTheme = StartupUiUtil.isUnderDarcula();

            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                if (project.isDisposed()) {
                    continue;
                }

                project.getMessageBus().syncPublisher(ThemeChangeNotifier.THEME_CHANGED_TOPIC)
                        .themeChanged(isDarkTheme);
            }
        });
    }
}
