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
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>DevoxxGenie Conversation</title>\n" +
                "    <link rel=\"stylesheet\" href=\"" + webServer.getPrismCssUrl() + "\">\n" +
                generateStyles() +
                "</head>\n" +
                "<body>\n" +
                "<div id=\"conversation-container\">\n" +
                "</div>\n" +

                // Load PrismJS core and JavaScript components
                generateScriptTags() +
                "</body>\n" +
                "</html>";
    }
    
    /**
     * Generate the CSS styles for the conversation.
     * 
     * @return CSS styles as a string
     */
    private String generateStyles() {
        // Use the ThemeDetector to determine if dark mode is active
        boolean isDarkMode = com.devoxx.genie.ui.util.ThemeDetector.isDarkTheme();
        
        // Set appropriate background and text colors based on theme
        String bgColorHex;
        if (isDarkMode) {
            bgColorHex = "#2b2b2b"; // Dark theme background
        } else {
            bgColorHex = "#f5f5f5"; // Light theme background
        }
        String textColor = isDarkMode ? "#e0e0e0" : "#2b2b2b";
        
        // Get the editor font size from the IDE settings
        int editorFontSize = com.devoxx.genie.ui.util.EditorFontUtil.getEditorFontSize();
        
        return String.format("""
        <style>
            html, body { width: 100%%; height: 100%%; margin: 0; padding: 0; }
            body { 
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif; 
                font-size: %dpx;
                line-height: 1.6; 
                padding: 0; 
                background-color: %s; 
                color: %s; 
                overflow-y: auto; 
            }
            #conversation-container { padding: 20px 10px; min-height: 100%%; }
            pre { margin: 1em 0; position: relative; border-radius: 4px; background-color: %s; overflow-x: auto; }
            code { 
                font-family: 'JetBrains Mono', Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace; 
                font-size: %dpx;
                white-space: pre-wrap; 
                word-break: break-word; 
            }
            .toolbar-container { position: absolute; top: 0; right: 0; padding: 5px; }
            .copy-button { background: %s; border: none; border-radius: 4px; color: %s; cursor: pointer; font-size: 0.8em; padding: 4px 8px; }
            .copy-button:hover { background: %s; }
            .message-pair { margin-bottom: 20px; width: 100%%; }
            .user-message { background-color: %s; border-left: 4px solid #FF5400; padding: 10px 8px; margin: 10px 0; border-radius: 4px; word-wrap: break-word; overflow-wrap: break-word; }
            .assistant-message { background-color: %s; border-left: 4px solid #0095C9; padding: 10px 8px; margin: 10px 0; border-radius: 4px; position: relative; word-wrap: break-word; overflow-wrap: break-word; }
            .metadata-info { font-size: %dpx; color: %s; margin-bottom: 10px; font-style: italic; }
            .copy-response-button { position: absolute; top: 10px; right: 10px; background: %s; border: none; border-radius: 4px; color: %s; cursor: pointer; font-size: %dpx; padding: 4px 8px; }
            .copy-response-button:hover { background: %s; }
            a { color: %s; text-decoration: none; }
            a:hover { text-decoration: underline; }
            h2 { margin-top: 10px; margin-bottom: 10px; color: %s; font-size: %dpx; }
            ul { padding-left: 20px; }
            li { margin-bottom: 8px; }
            .feature-emoji { margin-right: 5px; }
            .feature-name { font-weight: bold; }
            .subtext { font-size: %dpx; color: %s; margin-top: 5px; }
            .container { width: 100%%; max-width: 800px; margin: 0 auto; }
            .file-references-container { margin: 10px 0; background-color: %s; border-radius: 4px; border-left: 4px solid %s; }
            .file-references-header { padding: 10px 8px; cursor: pointer; display: flex; align-items: center; }
            .file-references-header:hover { background-color: %s; }
            .file-references-icon { margin-right: 8px; }
            .file-references-title { flex-grow: 1; font-weight: bold; }
            .file-references-toggle { margin-left: 8px; }
            .file-references-content { padding: 10px 8px; border-top: 1px solid %s; }
            .file-list { list-style-type: none; padding: 0; margin: 0; }
            .file-item { padding: 5px 0; }
            .file-name { font-weight: bold; margin-right: 8px; }
            .file-path { color: %s; font-style: italic; font-size: %dpx; word-wrap: break-word; overflow-wrap: break-word; }
        </style>
        """,
                editorFontSize,
                bgColorHex,
                textColor,
                isDarkMode ? "#1e1e1e" : "#f5f5f5",
                editorFontSize,
                isDarkMode ? "rgba(255, 255, 255, 0.1)" : "rgba(0, 0, 0, 0.1)",
                isDarkMode ? "#ffffff" : "#333333",
                isDarkMode ? "rgba(255, 255, 255, 0.2)" : "rgba(0, 0, 0, 0.2)",
                isDarkMode ? "#2a2520" : "#fff9f0", // Devoxx orange-tinted background for user messages
                isDarkMode ? "#1e282e" : "#f0f7ff", // Blue-tinted background for assistant messages
                Math.max(editorFontSize - 2, 10), // Slightly smaller font for metadata
                isDarkMode ? "#aaaaaa" : "#666666",
                isDarkMode ? "rgba(255, 255, 255, 0.1)" : "rgba(0, 0, 0, 0.1)",
                isDarkMode ? "#e0e0e0" : "#4a4a4a",
                Math.max(editorFontSize - 2, 10), // Slightly smaller font for buttons
                isDarkMode ? "rgba(255, 255, 255, 0.2)" : "rgba(0, 0, 0, 0.2)",
                "#FF5400", // Use Devoxx orange color
                "#FF5400", // Use Devoxx orange color
                Math.max((int)(editorFontSize * 1.2), 14), // Slightly larger font for headings
                Math.max(editorFontSize - 2, 10), // Slightly smaller font for subtexts
                isDarkMode ? "#aaaaaa" : "#666666",
                isDarkMode ? "#1e1e1e" : "#f5f5f5",
                "#FF5400", // Use Devoxx orange color
                isDarkMode ? "rgba(255, 255, 255, 0.1)" : "rgba(0, 0, 0, 0.05)",
                isDarkMode ? "rgba(255, 255, 255, 0.1)" : "rgba(0, 0, 0, 0.1)",
                isDarkMode ? "#aaaaaa" : "#666666",
                Math.max(editorFontSize - 2, 10) // Slightly smaller font for file paths
        );
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
        return """
                    function copyMessageResponse(button) {
                        const assistantMessage = button.closest('.assistant-message');
                        // Get all content except the Copy button and metadata
                        const contentToCopy = Array.from(assistantMessage.childNodes)
                            .filter(node => !node.classList || (!node.classList.contains('copy-response-button') && !node.classList.contains('metadata-info')))
                            .map(node => node.textContent || node.innerText)
                            .join('\\n')
                            .trim();

                        navigator.clipboard.writeText(contentToCopy).then(function() {
                            // Add animation class
                            button.classList.add('copy-button-flash');
                            button.textContent = 'Copied!';

                            setTimeout(function() {
                                button.textContent = 'Copy';
                                button.classList.remove('copy-button-flash');
                            }, 2000);
                        }).catch(function(err) {
                            console.error('Failed to copy: ', err);
                            button.textContent = 'Error!';
                            setTimeout(function() {
                                button.textContent = 'Copy';
                            }, 2000);
                        });
                    }

                    function toggleFileReferences(header) {
                        const content = header.nextElementSibling;
                        const toggle = header.querySelector('.file-references-toggle');
                        if (content.style.display === 'none') {
                            content.style.display = 'block';
                            toggle.textContent = '▼';
                        } else {
                            content.style.display = 'none';
                            toggle.textContent = '▶';
                        }
                    }
                
                    function highlightCodeBlocks() {
                        if (typeof Prism !== 'undefined') {
                            Prism.highlightAll();
                            // Add copy buttons to code blocks
                            document.querySelectorAll('pre:not(.processed)').forEach(function(block) {
                                // Mark the block as processed to avoid adding buttons multiple times
                                block.classList.add('processed');
                                var button = document.createElement('button');
                                button.className = 'copy-button';
                                button.textContent = 'Copy';
                                var container = document.createElement('div');
                                container.className = 'toolbar-container';
                                container.appendChild(button);
                                block.appendChild(container);
                                button.addEventListener('click', function() {
                                    var code = block.querySelector('code');
                                    var text = code.textContent;
                                    navigator.clipboard.writeText(text).then(function() {
                                        button.textContent = 'Copied!';
                                        setTimeout(function() {
                                            button.textContent = 'Copy';
                                        }, 2000);
                                    }).catch(function(err) {
                                        console.error('Failed to copy: ', err);
                                        button.textContent = 'Error!';
                                    });
                                });
                            });
                        }
                    }
                    // Initialize when the page loads
                    document.addEventListener('DOMContentLoaded', function() {
                        highlightCodeBlocks();
                        // Ensure the whole page is visible
                        document.body.style.display = 'block';
                    });
                """;
    }
}
