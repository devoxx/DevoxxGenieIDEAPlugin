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
        StringBuilder htmlBuilder = new StringBuilder();
        
        // Start HTML document with references to PrismJS resources
        htmlBuilder.append("<!DOCTYPE html>\n")
                .append("<html>\n")
                .append("<head>\n")
                .append("    <meta charset=\"utf-8\">\n")
                .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("    <title>DevoxxGenie Conversation</title>\n")
                .append("    <link rel=\"stylesheet\" href=\"").append(webServer.getPrismCssUrl()).append("\">\n")
                .append(generateStyles())
                .append("</head>\n")
                .append("<body>\n")
                .append("<div id=\"conversation-container\">\n")
                .append("</div>\n");
        
        // Load PrismJS core and JavaScript components
        htmlBuilder.append(generateScriptTags());
        
        htmlBuilder.append("</body>\n")
                .append("</html>");
        
        return htmlBuilder.toString();
    }
    
    /**
     * Generate the CSS styles for the conversation.
     * 
     * @return CSS styles as a string
     */
    private String generateStyles() {
        return "    <style>\n"
                + "        html, body { width: 100%; height: 100%; margin: 0; padding: 0; }\n"
                + "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif; line-height: 1.6; padding: 0; background-color: #000000; color: #e0e0e0; overflow-y: auto; }\n"
                + "        #conversation-container { padding: 20px; min-height: 100%; }\n"
                + "        pre { margin: 1em 0; position: relative; border-radius: 4px; background-color: #1e1e1e; overflow-x: auto; }\n"
                + "        code { font-family: 'JetBrains Mono', Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace; }\n"
                + "        .toolbar-container { position: absolute; top: 0; right: 0; padding: 5px; }\n"
                + "        .copy-button { background: rgba(255, 255, 255, 0.1); border: none; border-radius: 4px; color: #fff; cursor: pointer; font-size: 0.8em; padding: 4px 8px; }\n"
                + "        .copy-button:hover { background: rgba(255, 255, 255, 0.2); }\n"
                + "        .message-pair { margin-bottom: 20px; width: 100%; }\n"
                + "        .user-message { background-color: #1a2733; border-left: 4px solid #0077cc; padding: 10px; margin: 10px 0; border-radius: 4px; }\n"
                + "        .assistant-message { background-color: #1a2a1a; border-left: 4px solid #4CAF50; padding: 10px; margin: 10px 0; border-radius: 4px; position: relative; }\n"
                + "        .metadata-info { font-size: 0.8em; color: #aaaaaa; margin-bottom: 10px; font-style: italic; }\n"
                + "        .copy-response-button { position: absolute; top: 10px; right: 10px; background: rgba(255, 255, 255, 0.1); border: none; border-radius: 4px; color: #e0e0e0; cursor: pointer; font-size: 0.8em; padding: 4px 8px; }\n"
                + "        .copy-response-button:hover { background: rgba(255, 255, 255, 0.2); }\n"
                + "        a { color: #64b5f6; text-decoration: none; }\n"
                + "        a:hover { text-decoration: underline; }\n"
                + "        h2 { margin-top: 20px; margin-bottom: 10px; color: #64b5f6; }\n"
                + "        ul { padding-left: 20px; }\n"
                + "        li { margin-bottom: 8px; }\n"
                + "        .feature-emoji { margin-right: 5px; }\n"
                + "        .feature-name { font-weight: bold; }\n"
                + "        .subtext { font-size: 0.9em; color: #aaaaaa; margin-top: 5px; }\n"
                + "        .container { width: 100%; max-width: 800px; margin: 0 auto; }\n"
                + "    </style>\n";
    }
    
    /**
     * Generate script tags for PrismJS and custom JavaScript functions.
     * 
     * @return Script tags as a string
     */
    private String generateScriptTags() {
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
        return "    function copyMessageResponse(button) {\n"
                + "        const assistantMessage = button.closest('.assistant-message');\n"
                + "        const messageText = assistantMessage.innerText;\n"
                + "        // Extract text content after Assistant: label\n"
                + "        const responseText = messageText.split('Assistant:')[1].trim();\n"
                + "        navigator.clipboard.writeText(responseText).then(function() {\n"
                + "            button.textContent = 'Copied!';\n"
                + "            setTimeout(function() {\n"
                + "                button.textContent = 'Copy';\n"
                + "            }, 2000);\n"
                + "        }).catch(function(err) {\n"
                + "            console.error('Failed to copy: ', err);\n"
                + "        });\n"
                + "    }\n"
                + "\n"
                + "    function highlightCodeBlocks() {\n"
                + "        if (typeof Prism !== 'undefined') {\n"
                + "            Prism.highlightAll();\n"
                + "            // Add copy buttons to code blocks\n"
                + "            document.querySelectorAll('pre:not(.processed)').forEach(function(block) {\n"
                + "                // Mark the block as processed to avoid adding buttons multiple times\n"
                + "                block.classList.add('processed');\n"
                + "                \n"
                + "                var button = document.createElement('button');\n"
                + "                button.className = 'copy-button';\n"
                + "                button.textContent = 'Copy';\n"
                + "                \n"
                + "                var container = document.createElement('div');\n"
                + "                container.className = 'toolbar-container';\n"
                + "                container.appendChild(button);\n"
                + "                \n"
                + "                block.appendChild(container);\n"
                + "                \n"
                + "                button.addEventListener('click', function() {\n"
                + "                    var code = block.querySelector('code');\n"
                + "                    var text = code.textContent;\n"
                + "                    \n"
                + "                    navigator.clipboard.writeText(text).then(function() {\n"
                + "                        button.textContent = 'Copied!';\n"
                + "                        setTimeout(function() {\n"
                + "                            button.textContent = 'Copy';\n"
                + "                        }, 2000);\n"
                + "                    }).catch(function(err) {\n"
                + "                        console.error('Failed to copy: ', err);\n"
                + "                        button.textContent = 'Error!';\n"
                + "                    });\n"
                + "                });\n"
                + "            });\n"
                + "        }\n"
                + "    }\n"
                + "    \n"
                + "    // Initialize when the page loads\n"
                + "    document.addEventListener('DOMContentLoaded', function() {\n"
                + "        highlightCodeBlocks();\n"
                + "        // Ensure the whole page is visible\n"
                + "        document.body.style.display = 'block';\n"
                + "    });";
    }
}
