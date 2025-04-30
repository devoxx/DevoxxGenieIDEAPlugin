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
    
    public WebViewThemeManager(JBCefBrowser browser, WebServer webServer, 
                              WebViewJavaScriptExecutor jsExecutor,
                              Consumer<ResourceBundle> welcomeContentLoader) {
        this.browser = browser;
        this.webServer = webServer;
        this.jsExecutor = jsExecutor;
        this.welcomeContentLoader = welcomeContentLoader;
        
        // Register for theme change notifications
        ThemeDetector.addThemeChangeListener(this::themeChanged);
        
        // Register for appearance settings changes
        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
        connection.subscribe(APPEARANCE_SETTINGS_TOPIC, this);
        
        // Initialize the appearance refresh handler 
        AppearanceRefreshHandler.getInstance();
    }
    
    /**
     * Called when appearance settings have changed.
     * Applies the changes by injecting updated CSS into the current page.
     */
    private void applyAppearanceChanges() {
        log.info("Appearance settings changed, updating WebView");
        
        if (jsExecutor.isLoaded()) {
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
        
        // Reload the content with the new theme
        ConversationTemplate template = new ConversationTemplate(webServer);
        String htmlContent = template.generate();
        
        // Update the browser content - this will reload with the new theme styles
        if (jsExecutor.isLoaded()) {
            // Create a new resource with the updated HTML content
            String resourceId = webServer.addDynamicResource(htmlContent);
            String resourceUrl = webServer.getResourceUrl(resourceId);
            
            // Set a flag to indicate that we should reload welcome content after the browser loads
            final boolean[] welcomeReloaded = {false};
            
            // Add a temporary load handler to reload the welcome content after the theme change
            browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                    if (!welcomeReloaded[0]) {
                        welcomeReloaded[0] = true;
                        log.info("Browser reloaded after theme change, restoring welcome content");
                        ApplicationManager.getApplication().invokeLater(() -> {
                            ResourceBundle resourceBundle = ResourceBundle.getBundle("messages");
                            welcomeContentLoader.accept(resourceBundle);
                        });
                    }
                }
            }, browser.getCefBrowser());
            
            // Reload the browser with the new content
            browser.loadURL(resourceUrl);
        }
    }

    @Override
    public void appearanceSettingsChanged() {
        applyAppearanceChanges();
    }
}