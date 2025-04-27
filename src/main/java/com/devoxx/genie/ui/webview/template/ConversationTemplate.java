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
        return """
                    <style>
                        html, body { width: 100%; height: 100%; margin: 0; padding: 0; }
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif; line-height: 1.6; padding: 0; background-color: #000000; color: #e0e0e0; overflow-y: auto; }
                        #conversation-container { padding: 20px; min-height: 100%; }
                        pre { margin: 1em 0; position: relative; border-radius: 4px; background-color: #1e1e1e; overflow-x: auto; }
                        code { font-family: 'JetBrains Mono', Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace; }
                        .toolbar-container { position: absolute; top: 0; right: 0; padding: 5px; }
                        .copy-button { background: rgba(255, 255, 255, 0.1); border: none; border-radius: 4px; color: #fff; cursor: pointer; font-size: 0.8em; padding: 4px 8px; }
                        .copy-button:hover { background: rgba(255, 255, 255, 0.2); }
                        .message-pair { margin-bottom: 20px; width: 100%; }
                        .user-message { background-color: #1a2733; border-left: 4px solid #0077cc; padding: 10px; margin: 10px 0; border-radius: 4px; }
                        .assistant-message { background-color: #1a2a1a; border-left: 4px solid #4CAF50; padding: 10px; margin: 10px 0; border-radius: 4px; position: relative; }
                        .metadata-info { font-size: 0.8em; color: #aaaaaa; margin-bottom: 10px; font-style: italic; }
                        .copy-response-button { position: absolute; top: 10px; right: 10px; background: rgba(255, 255, 255, 0.1); border: none; border-radius: 4px; color: #e0e0e0; cursor: pointer; font-size: 0.8em; padding: 4px 8px; }
                        .copy-response-button:hover { background: rgba(255, 255, 255, 0.2); }
                        a { color: #64b5f6; text-decoration: none; }
                        a:hover { text-decoration: underline; }
                        h2 { margin-top: 20px; margin-bottom: 10px; color: #64b5f6; }
                        ul { padding-left: 20px; }
                        li { margin-bottom: 8px; }
                        .feature-emoji { margin-right: 5px; }
                        .feature-name { font-weight: bold; }
                        .subtext { font-size: 0.9em; color: #aaaaaa; margin-top: 5px; }
                        .container { width: 100%; max-width: 800px; margin: 0 auto; }
                        /* File references styles */
                        .file-references-container { margin: 10px 0; background-color: #1e1e1e; border-radius: 4px; border-left: 4px solid #64b5f6; }
                        .file-references-header { padding: 10px; cursor: pointer; display: flex; align-items: center; }
                        .file-references-header:hover { background-color: rgba(255, 255, 255, 0.1); }
                        .file-references-icon { margin-right: 8px; }
                        .file-references-title { flex-grow: 1; font-weight: bold; }
                        .file-references-toggle { margin-left: 8px; }
                        .file-references-content { padding: 10px; border-top: 1px solid rgba(255, 255, 255, 0.1); }
                        .file-list { list-style-type: none; padding: 0; margin: 0; }
                        .file-item { padding: 5px 0; }
                        .file-name { font-weight: bold; margin-right: 8px; }
                        .file-path { color: #aaaaaa; font-style: italic; font-size: 0.9em; }
                    </style>
                """;
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
                        const messageText = assistantMessage.innerText;
                        // Extract text content after Assistant: label
                        const responseText = messageText.split('Assistant:')[1].trim();
                        navigator.clipboard.writeText(responseText).then(function() {
                            button.textContent = 'Copied!';
                            setTimeout(function() {
                                button.textContent = 'Copy';
                            }, 2000);
                        }).catch(function(err) {
                            console.error('Failed to copy: ', err);
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
