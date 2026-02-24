package com.devoxx.genie.ui.settings.appearance;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.util.messages.MessageBusConnection;
import lombok.extern.slf4j.Slf4j;

import static com.devoxx.genie.ui.topic.AppTopics.APPEARANCE_SETTINGS_TOPIC;

/**
 * Handles appearance settings changes.
 * With Compose for Desktop, theme changes are observed automatically via
 * isSystemInDarkTheme() and state recomposition. This handler is kept for
 * the service contract but no longer injects CSS into a WebView.
 */
@Service
@Slf4j
public final class AppearanceRefreshHandler implements AppearanceSettingsEvents {

    public AppearanceRefreshHandler() {
        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
        connection.subscribe(APPEARANCE_SETTINGS_TOPIC, this);
    }

    public static AppearanceRefreshHandler getInstance() {
        return ApplicationManager.getApplication().getService(AppearanceRefreshHandler.class);
    }

    @Override
    public void appearanceSettingsChanged() {
        // No-op: Compose UI observes theme changes automatically via recomposition.
        log.info("Appearance settings changed (handled by Compose recomposition)");
    }
}
