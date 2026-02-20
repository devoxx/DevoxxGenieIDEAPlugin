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

    public static final String NEWLINE = "');\n";
    public static final String PIXELS = "px');\n";
    public static final String COLON_NEWLINE = "'; });\n";

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
                .append(state.getLineHeight()).append(NEWLINE);

        // Apply padding and margins
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-message-padding', '")
                .append(state.getMessagePadding()).append(PIXELS);
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-message-margin', '")
                .append(state.getMessageMargin()).append(PIXELS);

        // Apply border settings
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-border-width', '")
                .append(state.getBorderWidth()).append(PIXELS);
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-corner-radius', '")
                .append(state.getCornerRadius()).append(PIXELS);

        // Apply border colors - these are always the Devoxx brand colors regardless of theme
        // Orange for user, blue for assistant
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-user-message-border-color', '")
                .append(state.getUserMessageBorderColor()).append(NEWLINE);
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-assistant-message-border-color', '")
                .append(state.getAssistantMessageBorderColor()).append(NEWLINE);

        // Apply background and text colors based on useCustomColors setting
        String userMessageBorderColor;
        String userMessageBackgroundColor;
        String userMessageTextColor;

        String assistantMessageBorderColor;
        String assistantMessageBackgroundColor;
        String assistantMessageTextColor;

        if (Boolean.TRUE.equals(state.getUseCustomColors())) {
            log.debug("Use custom colors");

            // Use user-defined colors
            userMessageBorderColor = state.getUserMessageBorderColor();
            userMessageBackgroundColor = state.getUserMessageBackgroundColor();
            userMessageTextColor = state.getUserMessageTextColor();

            assistantMessageBorderColor = state.getAssistantMessageBorderColor();
            assistantMessageBackgroundColor = state.getAssistantMessageBackgroundColor();
            assistantMessageTextColor = state.getAssistantMessageTextColor();

        } else {
            // Use default theme-based colors
            if (isDarkTheme) {
                log.debug("Use Dark theme");
                // Dark theme defaults
                userMessageBorderColor = "#FF5400";
                userMessageBackgroundColor = "#2a2520";
                userMessageTextColor = "#e0e0e0";

                assistantMessageBorderColor = "#0095C9";
                assistantMessageBackgroundColor = "#1e282e";
                assistantMessageTextColor = "#e0e0e0";
            } else {
                log.debug("Use Light theme");
                // Light theme defaults
                userMessageBorderColor = "#FF5400";
                userMessageBackgroundColor = "#fff9f0";
                userMessageTextColor = "#000000";

                assistantMessageBorderColor = "#0095C9";
                assistantMessageTextColor = "#000000";
                assistantMessageBackgroundColor = "#f0f7ff";
            }
        }

        // Set CSS variables
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-user-message-background-color', '")
                .append(userMessageBackgroundColor).append(NEWLINE);
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-assistant-message-background-color', '")
                .append(assistantMessageBackgroundColor).append(NEWLINE);
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-user-message-text-color', '")
                .append(userMessageTextColor).append(NEWLINE);
        cssStyleUpdates.append("document.documentElement.style.setProperty('--custom-assistant-message-text-color', '")
                .append(assistantMessageTextColor).append(NEWLINE);
                
        // Apply text colors directly to elements - this is needed for proper rendering
        cssStyleUpdates.append("document.querySelectorAll('.user-message').forEach(function(el) { ")
                .append("el.style.color = '").append(userMessageTextColor).append("'; ")
                .append("el.style.backgroundColor = '").append(userMessageBackgroundColor).append("'; ")
                .append("el.style.borderColor = '").append(userMessageBorderColor).append(COLON_NEWLINE);
        cssStyleUpdates.append("document.querySelectorAll('.assistant-message').forEach(function(el) { ")
                .append("el.style.color = '").append(assistantMessageTextColor).append("'; ")
                .append("el.style.backgroundColor = '").append(assistantMessageBackgroundColor).append("'; ")
                .append("el.style.borderColor = '").append(assistantMessageBorderColor).append(COLON_NEWLINE);

        // Apply font sizes if custom sizes are enabled
        if (Boolean.TRUE.equals(state.getUseCustomFontSize())) {
            cssStyleUpdates.append("document.body.style.fontSize = '").append(state.getCustomFontSize()).append("px';\n");
        } else {
            cssStyleUpdates.append("document.body.style.fontSize = '';\n");
        }

        if (Boolean.TRUE.equals(state.getUseCustomCodeFontSize())) {
            cssStyleUpdates.append("document.querySelectorAll('code').forEach(function(el) { el.style.fontSize = '")
                    .append(state.getCustomCodeFontSize()).append("px'; });\n");
        } else {
            cssStyleUpdates.append("document.querySelectorAll('code').forEach(function(el) { el.style.fontSize = ''; });\n");
        }

        // Apply rounded corners setting
        String borderRadius = Boolean.TRUE.equals(state.getUseRoundedCorners()) ? state.getCornerRadius() + "px" : "0";
        cssStyleUpdates.append("document.querySelectorAll('.user-message, .assistant-message').forEach(function(el) { ")
                .append("el.style.borderRadius = '").append(borderRadius).append(COLON_NEWLINE);

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
