package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.ui.settings.appearance.AppearanceRefreshHandler;
import com.devoxx.genie.ui.settings.appearance.AppearanceSettingsEvents;
import com.devoxx.genie.ui.util.ThemeChangeNotifier;
import com.devoxx.genie.ui.util.ThemeDetector;
import com.devoxx.genie.ui.webview.WebServer;
import com.devoxx.genie.ui.webview.template.ConversationTemplate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.messages.MessageBusConnection;
import lombok.extern.slf4j.Slf4j;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;

import java.util.ResourceBundle;
import java.util.function.Consumer;

import static com.devoxx.genie.ui.topic.AppTopics.APPEARANCE_SETTINGS_TOPIC;

/**
 * Manages theme-related functionality for the WebView.
 * Handles theme changes and updates the WebView accordingly.
 */
@Slf4j
public class WebViewThemeManager implements ThemeChangeNotifier, AppearanceSettingsEvents {

    private final JBCefBrowser browser;
    private final WebServer webServer;
    private final WebViewJavaScriptExecutor jsExecutor;
    private final Consumer<ResourceBundle> welcomeContentLoader;
    private final MessageBusConnection messageBusConnection;
    private final ThemeDetector.ThemeChangeListener themeChangeListener;
    private CefLoadHandlerAdapter currentThemeLoadHandler;

    public WebViewThemeManager(JBCefBrowser browser, WebServer webServer,
                              WebViewJavaScriptExecutor jsExecutor,
                              Consumer<ResourceBundle> welcomeContentLoader) {
        this.browser = browser;
        this.webServer = webServer;
        this.jsExecutor = jsExecutor;
        this.welcomeContentLoader = welcomeContentLoader;

        // Register for theme change notifications (store reference for removal)
        themeChangeListener = this::themeChanged;
        ThemeDetector.addThemeChangeListener(themeChangeListener);

        // Register for appearance settings changes (store connection for disposal)
        messageBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
        messageBusConnection.subscribe(APPEARANCE_SETTINGS_TOPIC, this);

        // Initialize the appearance refresh handler
        AppearanceRefreshHandler.getInstance();
    }
    
    /**
     * Called when appearance settings have changed.
     * Applies the changes by injecting updated CSS into the current page.
     */
    private void applyAppearanceChanges() {
        log.info("Appearance settings changed, updating WebView");
        
        // Check if browser is available
        if (browser == null) {
            log.warn("Browser is null, cannot apply appearance changes");
            return;
        }
        
        if (jsExecutor.isLoaded()) {
            try {
                // Get script from the appearance handler via the web server
                String scriptUrl = webServer.getScriptUrl("appearance-update-script");
                if (scriptUrl != null && !scriptUrl.isEmpty()) {
                    // Run the script that updates all CSS variables
                    jsExecutor.executeJavaScript(
                        String.format("var script = document.createElement('script'); " +
                                     "script.src = '%s'; " +
                                     "document.head.appendChild(script);", scriptUrl)
                    );
                }
            } catch (Exception e) {
                log.error("Error applying appearance changes: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Called when the IDE theme changes.
     * Refresh the web view with new styling based on the current theme.
     *
     * @param isDarkTheme true if the new theme is dark, false if it's light
     */
    @Override
    public void themeChanged(boolean isDarkTheme) {
        log.info("Theme changed to " + (isDarkTheme ? "dark" : "light") + " mode, refreshing web view");
        
        // Check if browser is available
        if (browser == null) {
            log.warn("Browser is null, cannot refresh theme");
            return;
        }
        
        // Reload the content with the new theme
        ConversationTemplate template = new ConversationTemplate(webServer);
        String htmlContent = template.generate();
        
        // Update the browser content - this will reload with the new theme styles
        if (jsExecutor.isLoaded()) {
            try {
                // Create a new resource with the updated HTML content
                String resourceId = webServer.addDynamicResource(htmlContent);
                String resourceUrl = webServer.getResourceUrl(resourceId);

                // Remove previous theme load handler if it exists to prevent accumulation
                if (currentThemeLoadHandler != null) {
                    browser.getJBCefClient().removeLoadHandler(currentThemeLoadHandler, browser.getCefBrowser());
                }

                // Add a temporary load handler to reload the welcome content after the theme change
                final boolean[] welcomeReloaded = {false};
                currentThemeLoadHandler = new CefLoadHandlerAdapter() {
                    @Override
                    public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                        if (!welcomeReloaded[0]) {
                            welcomeReloaded[0] = true;
                            log.info("Browser reloaded after theme change");
                            ApplicationManager.getApplication().invokeLater(() -> {
                                ResourceBundle resourceBundle = ResourceBundle.getBundle("messages");
                                welcomeContentLoader.accept(resourceBundle);
                            });
                        }
                    }
                };
                browser.getJBCefClient().addLoadHandler(currentThemeLoadHandler, browser.getCefBrowser());

                // Reload the browser with the new content
                browser.loadURL(resourceUrl);
            } catch (Exception e) {
                log.error("Error refreshing browser theme: {}", e.getMessage());
            }
        }
    }

    @Override
    public void appearanceSettingsChanged() {
        applyAppearanceChanges();
    }

    /**
     * Dispose of resources when this manager is no longer needed.
     */
    public void dispose() {
        ThemeDetector.removeThemeChangeListener(themeChangeListener);
        if (messageBusConnection != null) {
            messageBusConnection.disconnect();
        }
        if (currentThemeLoadHandler != null && browser != null) {
            try {
                browser.getJBCefClient().removeLoadHandler(currentThemeLoadHandler, browser.getCefBrowser());
            } catch (Exception e) {
                log.debug("Error removing theme load handler during disposal: {}", e.getMessage());
            }
            currentThemeLoadHandler = null;
        }
    }
}