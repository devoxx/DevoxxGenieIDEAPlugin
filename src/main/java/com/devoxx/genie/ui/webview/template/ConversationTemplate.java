package com.devoxx.genie.ui.webview.template;

import com.devoxx.genie.ui.webview.WebServer;
import org.jetbrains.annotations.NotNull;

/**
 * Template for generating the main conversation HTML structure.
 */
public class ConversationTemplate extends HtmlTemplate {

    // Languages that require additional PrismJS component loading
    private static final String[] LANGUAGES_WITH_COMPONENTS = {
            "java", "python", "javascript", "typescript", "csharp", "go", "rust", "kotlin", 
            "bash", "cpp", "css", "dart", "json", "markdown", "sql", "yaml"
    };

    /**
     * Constructor with WebServer dependency.
     *
     * @param webServer The web server instance for resource URLs
     */
    public ConversationTemplate(WebServer webServer) {
        super(webServer);
    }

    @Override
    public @NotNull String generate() {
        String htmlTemplate = ResourceLoader.loadResource("webview/html/conversation.html");
        return htmlTemplate
                .replace("${prismCssUrl}", webServer.getPrismCssUrl())
                .replace("${styles}", generateStyles())
                .replace("${scripts}", generateScriptTags());
    }
    
    /**
     * Generate the CSS styles for the conversation.
     * 
     * @return CSS styles as a string
     */
    private String generateStyles() {
        // Get the theme state and font sizes
        boolean isDarkMode = com.devoxx.genie.ui.util.ThemeDetector.isDarkTheme();
        int editorFontSize = com.devoxx.genie.ui.util.EditorFontUtil.getEditorFontSize();
        
        // Calculate derived font sizes
        int metadataFontSize = Math.max(editorFontSize - 2, 10);
        int buttonFontSize = Math.max(editorFontSize - 2, 10);
        int headerFontSize = Math.max((int)(editorFontSize * 1.2), 14);
        int subtextFontSize = Math.max(editorFontSize - 2, 10);
        int filePathFontSize = Math.max(editorFontSize - 2, 10);
        
        // Load the CSS templates
        String themeVariables = ResourceLoader.loadResource("webview/css/theme-variables.css");
        String darkThemeOverrides = ResourceLoader.loadResource("webview/css/dark-theme.css");
        String cssTemplate = ResourceLoader.loadResource("webview/css/conversation.css");
        
        // Apply font size variables to the CSS template
        String css = cssTemplate
                .replace("${fontSize}", String.valueOf(editorFontSize))
                .replace("${metadataFontSize}", String.valueOf(metadataFontSize))
                .replace("${buttonFontSize}", String.valueOf(buttonFontSize))
                .replace("${headerFontSize}", String.valueOf(headerFontSize))
                .replace("${subtextFontSize}", String.valueOf(subtextFontSize))
                .replace("${filePathFontSize}", String.valueOf(filePathFontSize));
        
        // Return the combined styles, including dark theme overrides if needed
        StringBuilder styleBuilder = new StringBuilder();
        styleBuilder.append("<style>\n");
        styleBuilder.append(themeVariables).append("\n");
        
        // Apply dark theme overrides if needed
        if (isDarkMode) {
            styleBuilder.append(darkThemeOverrides).append("\n");
        }
        
        styleBuilder.append(css).append("\n");
        styleBuilder.append("</style>");
        
        return styleBuilder.toString();
    }
    
    /**
     * Generate script tags for PrismJS and custom JavaScript functions.
     * 
     * @return Script tags as a string
     */
    private @NotNull String generateScriptTags() {
        StringBuilder scripts = new StringBuilder();
        
        // Add PrismJS core
        scripts.append("\n<script src=\"").append(webServer.getPrismJsUrl()).append("\"></script>\n");
        
        // Add components for detected languages
        for (String lang : LANGUAGES_WITH_COMPONENTS) {
            scripts.append("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-")
                    .append(lang)
                    .append(".min.js\"></script>\n");
        }
        
        // Add JavaScript for handling copy functionality and code highlighting
        scripts.append("<script>\n")
                .append(generateJavaScript())
                .append("</script>\n");
        
        return scripts.toString();
    }
    
    /**
     * Generate custom JavaScript functions for the conversation.
     * 
     * @return JavaScript functions as a string
     */
    private String generateJavaScript() {
        // Load JavaScript from external file
        return ResourceLoader.loadResource("webview/js/conversation.js");
    }
}