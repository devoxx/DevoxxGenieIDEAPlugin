package com.devoxx.genie.ui.settings.appearance;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.ThemeDetector;
import com.devoxx.genie.ui.webview.WebServer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.util.messages.MessageBusConnection;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import static com.devoxx.genie.ui.topic.AppTopics.APPEARANCE_SETTINGS_TOPIC;

/**
 * Handles appearance settings changes and updates existing WebViews.
 */
@Service
@Slf4j
public final class AppearanceRefreshHandler implements AppearanceSettingsEvents {

    public AppearanceRefreshHandler() {
        // Register for appearance settings changes
         MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
         connection.subscribe(APPEARANCE_SETTINGS_TOPIC, this);
    }

    public static AppearanceRefreshHandler getInstance() {
        return ApplicationManager.getApplication().getService(AppearanceRefreshHandler.class);
    }

    /**
     * Called when appearance settings have changed.
     * This will inject updated CSS styles directly into the current page.
     */
    private void refreshAppearance() {
        log.info("Appearance settings changed, updating CSS");

        // Get the web server instance
        WebServer webServer = WebServer.getInstance();
        if (webServer == null || !webServer.isRunning()) {
            log.warn("Web server not running, can't update appearance");
            return;
        }

        // Generate new CSS based on current settings
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        boolean isDarkTheme = ThemeDetector.isDarkTheme();

        try {
            // Create CSS injection script for all open WebViews
            generateCssUpdateScript(state, isDarkTheme, webServer);
        } catch (Exception e) {
            log.error("Error generating appearance CSS update", e);
        }
    }

    /**
     * Generates a CSS update script to refresh all WebViews with new appearance settings.
     * This will be made available via the WebServer to be injected into WebViews.
     */
    private void generateCssUpdateScript(@NotNull DevoxxGenieStateService state, boolean isDarkTheme, WebServer webServer) {
        StringBuilder cssStyleUpdates = new StringBuilder();

        // Apply line height
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-line-height', '")
                .append(state.getLineHeight()).append("');\n");

        // Apply padding and margins
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-message-padding', '")
                .append(state.getMessagePadding()).append("px');\n");
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-message-margin', '")
                .append(state.getMessageMargin()).append("px');\n");

        // Apply border settings
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-border-width', '")
                .append(state.getBorderWidth()).append("px');\n");
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-corner-radius', '")
                .append(state.getCornerRadius()).append("px');\n");

        // Apply colors
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-user-message-border-color', '")
                .append(state.getUserMessageBorderColor()).append("');\n");
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-assistant-message-border-color', '")
                .append(state.getAssistantMessageBorderColor()).append("');\n");

        // Apply background colors - always use the user's selected colors
        // Remove dark theme override to respect user settings
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-user-message-background-color', '")
                .append(state.getUserMessageBackgroundColor()).append("');\n");
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-assistant-message-background-color', '")
                .append(state.getAssistantMessageBackgroundColor()).append("');\n");
                
        // Apply text colors
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-user-message-text-color', '")
                .append(state.getUserMessageTextColor()).append("');\n");
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-assistant-message-text-color', '")
                .append(state.getAssistantMessageTextColor()).append("');\n");
                
        // Apply text colors directly to elements
        cssStyleUpdates.append("document.querySelectorAll('.user-message').forEach(function(el) { ")
                .append("el.style.color = '").append(state.getUserMessageTextColor()).append("'; });\n");
        cssStyleUpdates.append("document.querySelectorAll('.assistant-message').forEach(function(el) { ")
                .append("el.style.color = '").append(state.getAssistantMessageTextColor()).append("'; });\n");

        // Apply font sizes if custom sizes are enabled
        if (state.getUseCustomFontSize()) {
            cssStyleUpdates.append("document.body.style.fontSize = '").append(state.getCustomFontSize()).append("px';\n");
        } else {
            cssStyleUpdates.append("document.body.style.fontSize = '';\n");
        }

        if (state.getUseCustomCodeFontSize()) {
            cssStyleUpdates.append("document.querySelectorAll('code').forEach(function(el) { el.style.fontSize = '")
                    .append(state.getCustomCodeFontSize()).append("px'; });\n");
        } else {
            cssStyleUpdates.append("document.querySelectorAll('code').forEach(function(el) { el.style.fontSize = ''; });\n");
        }

        // Apply rounded corners setting
        String borderRadius = state.getUseRoundedCorners() ? state.getCornerRadius() + "px" : "0";
        cssStyleUpdates.append("document.querySelectorAll('.user-message, .assistant-message').forEach(function(el) { ")
                .append("el.style.borderRadius = '").append(borderRadius).append("'; });\n");

        // Update message pair margins
        cssStyleUpdates.append("document.querySelectorAll('.message-pair').forEach(function(el) { ")
                .append("el.style.marginBottom = 'calc(").append(state.getMessageMargin()).append("px * 2)'; });\n");

        // Register this script with the WebServer
        String scriptId = "appearance-update-script";
        webServer.addDynamicScript(scriptId, cssStyleUpdates.toString());
        log.info("Created appearance update script - will be applied to next WebView refresh");
    }

    @Override
    public void appearanceSettingsChanged() {
        // Call the refreshAppearance method which handles getting all required instances
        refreshAppearance();
    }
}
