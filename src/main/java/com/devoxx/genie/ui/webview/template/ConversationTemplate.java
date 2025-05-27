package com.devoxx.genie.ui.webview.template;

import com.devoxx.genie.ui.webview.WebServer;
import org.jetbrains.annotations.NotNull;

/**
 * Template for generating the main conversation HTML structure.
 */
public class ConversationTemplate extends HtmlTemplate {

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
    private @NotNull String generateStyles() {
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
        String appearanceTemplate = ResourceLoader.loadResource("webview/css/appearance-custom.css");
        String mcpFormatting = ResourceLoader.loadResource("webview/css/mcp-formatting.css");
        String externalLinksStyles = ResourceLoader.loadResource("webview/css/external-links.css");
        
        // Apply font size variables to the CSS template
        String css = cssTemplate
                .replace("${fontSize}", String.valueOf(editorFontSize))
                .replace("${metadataFontSize}", String.valueOf(metadataFontSize))
                .replace("${buttonFontSize}", String.valueOf(buttonFontSize))
                .replace("${headerFontSize}", String.valueOf(headerFontSize))
                .replace("${subtextFontSize}", String.valueOf(subtextFontSize))
                .replace("${filePathFontSize}", String.valueOf(filePathFontSize));

        // Apply appearance settings to the custom appearance CSS template
        com.devoxx.genie.ui.settings.DevoxxGenieStateService stateService = 
                com.devoxx.genie.ui.settings.DevoxxGenieStateService.getInstance();
        
        String customAppearanceCss = appearanceTemplate
                .replace("${lineHeight}", String.valueOf(stateService.getLineHeight()))
                .replace("${messagePadding}", String.valueOf(stateService.getMessagePadding()))
                .replace("${messageMargin}", String.valueOf(stateService.getMessageMargin()))
                .replace("${borderWidth}", String.valueOf(stateService.getBorderWidth()))
                .replace("${cornerRadius}", String.valueOf(stateService.getCornerRadius()))
                .replace("${userMessageBorderColor}", stateService.getUserMessageBorderColor())
                .replace("${assistantMessageBorderColor}", stateService.getAssistantMessageBorderColor())
                .replace("${userMessageBackgroundColor}", stateService.getUserMessageBackgroundColor())
                .replace("${assistantMessageBackgroundColor}", stateService.getAssistantMessageBackgroundColor())
                .replace("${userMessageTextColor}", stateService.getUserMessageTextColor())
                .replace("${assistantMessageTextColor}", stateService.getAssistantMessageTextColor())
                .replace("${customFontSize}", String.valueOf(stateService.getCustomFontSize()))
                .replace("${customCodeFontSize}", String.valueOf(stateService.getCustomCodeFontSize()))
                .replace("${useRoundedCorners}", String.valueOf(stateService.getUseRoundedCorners()));
        
        // Return the combined styles, including dark theme overrides if needed
        StringBuilder styleBuilder = new StringBuilder();
        styleBuilder.append("<style>\n");
        styleBuilder.append(themeVariables).append("\n");

        // Apply dark theme overrides if needed
        if (isDarkMode) {
            styleBuilder.append(darkThemeOverrides).append("\n");
        }

        styleBuilder.append(css).append("\n");
        
        // Apply custom appearance styles
        if (Boolean.TRUE.equals(stateService.getUseCustomColors())) {
            styleBuilder.append(customAppearanceCss).append("\n");
        }
        
        // Apply MCP formatting styles
        styleBuilder.append(mcpFormatting).append("\n");
        
        // Apply external links styles
        styleBuilder.append(externalLinksStyles).append("\n");
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
        
        // First add the script loader utility
        scripts.append("\n<script id=\"script-loader\">\n")
               .append(ResourceLoader.loadResource("webview/js/script-loader.js"))
               .append("\n</script>\n");
        
        // Add PrismJS core - Note: already includes basic language support
        scripts.append("<script src=\"").append(webServer.getPrismJsUrl()).append("\"></script>\n");
        
        // Add Turndown.js for HTML to Markdown conversion
        scripts.append("<script>\n")
               .append(ResourceLoader.loadResource("webview/js/lib/turndown.js"))
               .append("\n</script>\n");
        
        // Get conversation JavaScript 
        String conversationJs = ResourceLoader.loadResource("webview/js/conversation.js");
        String fileReferencesJs = ResourceLoader.loadResource("webview/js/file-references.js");
        String mcpHandlerJs = ResourceLoader.loadResource("webview/js/mcp-handler.js");
        String externalLinkHandlerJs = ResourceLoader.loadResource("webview/js/external-link-handler.js");
        
        // Add script to load the JavaScript dynamically
        scripts.append("<script>\n")
               .append("  // Load the conversation JavaScript\n")
               .append("  loadScriptContent('conversation-script', `")
               .append(escapeJS(conversationJs))
               .append("`);\n\n")
               .append("  // Load the file references JavaScript\n")
               .append("  loadScriptContent('file-references-script', `")
               .append(escapeJS(fileReferencesJs))
               .append("`);\n\n")
               .append("  // Load the MCP handler JavaScript\n")
               .append("  loadScriptContent('mcp-handler-script', `")
               .append(escapeJS(mcpHandlerJs))
               .append("`);\n\n")
               .append("  // Load the external link handler JavaScript\n")
               .append("  loadScriptContent('external-link-handler-script', `")
               .append(escapeJS(externalLinkHandlerJs))
               .append("`);\n\n")
               .append("  // Initialize theme for file references\n")
               .append("  document.addEventListener('DOMContentLoaded', function() {\n")
               .append("    if (typeof addFileReferencesStyles === 'function') {\n")
               .append("      addFileReferencesStyles(").append(com.devoxx.genie.ui.util.ThemeDetector.isDarkTheme()).append(");\n")
               .append("    }\n")
               .append("  });\n")
               .append("</script>\n");
        
        return scripts.toString();
    }
    
    /**
     * Escapes JavaScript string literals.
     * Prevents issues when inserting JavaScript inside template literals.
     *
     * @param text The text to escape
     * @return Escaped text suitable for use in JavaScript
     */
    public String escapeJS(@NotNull String text) {
        return text.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("${", "\\${");
    }
}
