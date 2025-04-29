package com.devoxx.genie.ui.webview.template;

import com.devoxx.genie.ui.util.HelpUtil;
import com.devoxx.genie.ui.webview.WebServer;
import org.jetbrains.annotations.NotNull;

import java.util.ResourceBundle;

/**
 * Template for generating the welcome panel HTML content.
 */
public class WelcomeTemplate extends HtmlTemplate {

    private final ResourceBundle resourceBundle;

    /**
     * Constructor with WebServer and ResourceBundle dependencies.
     *
     * @param webServer The web server instance for resource URLs
     * @param resourceBundle The resource bundle for i18n
     */
    public WelcomeTemplate(WebServer webServer, ResourceBundle resourceBundle) {
        super(webServer);
        this.resourceBundle = resourceBundle;
    }

    @Override
    public @NotNull String generate() {
        // Load the HTML template from the resources
        String htmlTemplate = ResourceLoader.loadResource("webview/html/welcome.html");
        
        // Get the localized strings from the resource bundle
        String title = resourceBundle.getString("welcome.title");
        String description = resourceBundle.getString("welcome.description");
        String instructions = resourceBundle.getString("welcome.instructions");
        String tip = resourceBundle.getString("welcome.tip");
        String enjoy = resourceBundle.getString("welcome.enjoy");
        
        // Get the custom prompts for utility commands
        String customPromptCommands = HelpUtil.getCustomPromptCommandsForWebView();
        
        // Replace the placeholder variables in the template
        return htmlTemplate
            .replace("${title}", title)
            .replace("${description}", description)
            .replace("${instructions}", instructions)
            .replace("${tip}", tip)
            .replace("${enjoy}", enjoy)
            .replace("${customPromptCommands}", customPromptCommands);
    }
}