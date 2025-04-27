package com.devoxx.genie.ui.webview;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.util.LanguageGuesser;
import com.intellij.lang.Language;
import com.intellij.lang.documentation.DocumentationSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.Getter;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.commonmark.node.Block;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for managing the WebView component.
 * This class handles the interaction between the Java code and the WebView.
 */
public class WebViewController {
    private static final Logger LOG = Logger.getInstance(WebViewController.class);

    /**
     * -- GETTER --
     *  Get the JCef browser component.
     *
     * @return The JBCefBrowser instance
     */
    @Getter
    private final JBCefBrowser browser;
    private final WebServer webServer;
    private boolean isLoaded = false;
    
    // Languages that require additional PrismJS component loading
    private static final String[] LANGUAGES_WITH_COMPONENTS = {
        "java", "python", "javascript", "typescript", "csharp", "go", "rust", "kotlin", 
        "bash", "cpp", "css", "dart", "json", "markdown", "sql", "yaml"
    };
    
    /**
     * Creates a new WebViewController with a fresh browser.
     *
     * @param chatMessageContext The chat message context
     */
    public WebViewController(@NotNull ChatMessageContext chatMessageContext) {
        this.webServer = WebServer.getInstance();
        
        // Ensure web server is running
        if (!webServer.isRunning()) {
            webServer.start();
        }
        
        // Create HTML content for the response
        String htmlContent = generateHtmlContent(chatMessageContext);
        
        // Register content with the web server to get a URL
        String resourceId = webServer.addDynamicResource(htmlContent);
        String resourceUrl = webServer.getResourceUrl(resourceId);
        
        LOG.info("Loading WebView content from: " + resourceUrl);
        
        // Create browser and load content
        browser = WebViewFactory.createBrowser(resourceUrl);
        
        // Add load handler to detect when page is fully loaded
        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                isLoaded = true;
                LOG.info("WebView loaded with status: " + httpStatusCode);
            }
        }, browser.getCefBrowser());
    }

    /**
     * Check if the browser is fully loaded.
     *
     * @return true if loaded, false otherwise
     */
    public boolean isLoaded() {
        return isLoaded;
    }
    
    /**
     * Generate HTML content for the given chat message context.
     *
     * @param chatMessageContext The chat message context
     * @return HTML content as a string
     */
    private @NotNull String generateHtmlContent(@NotNull ChatMessageContext chatMessageContext) {
        StringBuilder htmlBuilder = new StringBuilder();
        
        // Start HTML document with references to PrismJS resources
        htmlBuilder.append("<!DOCTYPE html>\n")
                .append("<html>\n")
                .append("<head>\n")
                .append("    <meta charset=\"utf-8\">\n")
                .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("    <title>DevoxxGenie Response</title>\n")
                .append("    <link rel=\"stylesheet\" href=\"").append(webServer.getPrismCssUrl()).append("\">\n")
                .append("    <style>\n")
                .append("        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif; line-height: 1.6; margin: 0; padding: 10px; background-color: #000000; color: #e0e0e0; }\n")
                .append("        pre { margin: 1em 0; position: relative; border-radius: 4px; background-color: #1e1e1e; }\n")
                .append("        code { font-family: 'JetBrains Mono', Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace; }\n")
                .append("        .toolbar-container { position: absolute; top: 0; right: 0; padding: 5px; }\n")
                .append("        .copy-button { background: rgba(255, 255, 255, 0.1); border: none; border-radius: 4px; color: #fff; cursor: pointer; font-size: 0.8em; padding: 4px 8px; }\n")
                .append("        .copy-button:hover { background: rgba(255, 255, 255, 0.2); }\n")
                .append("        .user-message { background-color: #1a2733; border-left: 4px solid #0077cc; padding: 10px; margin: 10px 0; border-radius: 4px; }\n")
                .append("        .assistant-message { background-color: #1a2a1a; border-left: 4px solid #4CAF50; padding: 10px; margin: 10px 0; border-radius: 4px; position: relative; }\n")
                .append("        .metadata-info { font-size: 0.8em; color: #aaaaaa; margin-bottom: 10px; font-style: italic; }\n")
                .append("        .copy-response-button { position: absolute; top: 10px; right: 10px; background: rgba(255, 255, 255, 0.1); border: none; border-radius: 4px; color: #e0e0e0; cursor: pointer; font-size: 0.8em; padding: 4px 8px; }\n")
                .append("        .copy-response-button:hover { background: rgba(255, 255, 255, 0.2); }\n")
                .append("        a { color: #64b5f6; }\n")
                .append("    </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("<div class=\"conversation\">\n");
        
        // Add user message
        htmlBuilder.append("    <div class=\"user-message\">\n")
                .append("        <strong>User:</strong>\n")
                .append("        <p>").append(escapeHtml(chatMessageContext.getUserPrompt())).append("</p>\n")
                .append("    </div>\n");
        
        // Add assistant response
        htmlBuilder.append("    <div class=\"assistant-message\">\n")
                .append("        ").append(formatMetadata(chatMessageContext)).append("\n")
                .append("        <button class=\"copy-response-button\" onclick=\"copyFullResponse()\">Copy</button>\n")
                .append("        <strong>Assistant:</strong>\n");
        
        // Parse the markdown content
        String markdownResponse = chatMessageContext.getAiMessage().text();
        Node document = Parser.builder().build().parse(markdownResponse);
        
        // Process each node in the document
        Node node = document.getFirstChild();
        while (node != null) {
            if (node instanceof FencedCodeBlock fencedCodeBlock) {
                htmlBuilder.append(renderCodeBlock(fencedCodeBlock, chatMessageContext.getProject()));
            } else if (node instanceof IndentedCodeBlock indentedCodeBlock) {
                htmlBuilder.append(renderCodeBlock(indentedCodeBlock, chatMessageContext.getProject()));
            } else {
                // Use standard CommonMark HTML renderer for non-code blocks
                HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
                htmlBuilder.append(htmlRenderer.render(node));
            }
            
            node = node.getNext();
        }
        
        htmlBuilder.append("    </div>\n")
                .append("</div>\n");
        
        // Load PrismJS core and components
        htmlBuilder.append("\n<script src=\"").append(webServer.getPrismJsUrl()).append("\"></script>\n");
        
        // Add components for detected languages
        for (String lang : LANGUAGES_WITH_COMPONENTS) {
            htmlBuilder.append("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-")
                    .append(lang)
                    .append(".min.js\"></script>\n");
        }
        
        // Add function to copy full assistant response
        htmlBuilder.append("<script>\n")
                .append("    function copyFullResponse() {\n")
                .append("        const assistantMessage = document.querySelector('.assistant-message').innerText;\n")
                .append("        const responseText = assistantMessage.split('Assistant:')[1].trim();\n")
                .append("        navigator.clipboard.writeText(responseText).then(function() {\n")
                .append("            const button = document.querySelector('.copy-response-button');\n")
                .append("            button.textContent = 'Copied!';\n")
                .append("            setTimeout(function() {\n")
                .append("                button.textContent = 'Copy';\n")
                .append("            }, 2000);\n")
                .append("        }).catch(function(err) {\n")
                .append("            console.error('Failed to copy: ', err);\n")
                .append("        });\n")
                .append("    }\n");
        
        // Initialize PrismJS and add copy functionality
        htmlBuilder.append("\n")
                .append("    // Initialize PrismJS\n")
                .append("    document.addEventListener('DOMContentLoaded', function() {\n")
                .append("        if (typeof Prism !== 'undefined') {\n")
                .append("            Prism.highlightAll();\n")
                .append("            // Add copy buttons to code blocks\n")
                .append("            document.querySelectorAll('pre').forEach(function(block) {\n")
                .append("                var button = document.createElement('button');\n")
                .append("                button.className = 'copy-button';\n")
                .append("                button.textContent = 'Copy';\n")
                .append("                \n")
                .append("                var container = document.createElement('div');\n")
                .append("                container.className = 'toolbar-container';\n")
                .append("                container.appendChild(button);\n")
                .append("                \n")
                .append("                block.appendChild(container);\n")
                .append("                \n")
                .append("                button.addEventListener('click', function() {\n")
                .append("                    var code = block.querySelector('code');\n")
                .append("                    var text = code.textContent;\n")
                .append("                    \n")
                .append("                    navigator.clipboard.writeText(text).then(function() {\n")
                .append("                        button.textContent = 'Copied!';\n")
                .append("                        setTimeout(function() {\n")
                .append("                            button.textContent = 'Copy';\n")
                .append("                        }, 2000);\n")
                .append("                    }).catch(function(err) {\n")
                .append("                        console.error('Failed to copy: ', err);\n")
                .append("                        button.textContent = 'Error!';\n")
                .append("                    });\n")
                .append("                });\n")
                .append("            });\n")
                .append("        }\n")
                .append("    });\n")
                .append("</script>\n")
                .append("</body>\n")
                .append("</html>");
        
        return htmlBuilder.toString();
    }
    
    /**
     * Render a code block with PrismJS syntax highlighting.
     *
     * @param codeBlock The code block to render
     * @param project   The project
     * @return HTML representation of the code block
     */
    private @NotNull String renderCodeBlock(@NotNull Block codeBlock, @Nullable Project project) {
        StringBuilder sb = new StringBuilder();
        
        String code;
        String language = "";
        
        if (codeBlock instanceof FencedCodeBlock fencedCodeBlock) {
            code = fencedCodeBlock.getLiteral();
            language = fencedCodeBlock.getInfo();
        } else if (codeBlock instanceof IndentedCodeBlock indentedCodeBlock) {
            code = indentedCodeBlock.getLiteral();
        } else {
            return ""; // Unsupported code block type
        }
        
        // Map language from markdown to PrismJS language classes
        String prismLanguage = mapLanguageToPrism(language);
        
        // Create HTML for code block with PrismJS classes
        sb.append("<pre><code class=\"language-").append(prismLanguage).append("\">")
                .append(escapeHtml(code))
                .append("</code></pre>\n");
        
        return sb.toString();
    }
    
    /**
     * Map language identifier to PrismJS language class.
     *
     * @param languageInfo Language info from markdown code block
     * @return PrismJS language class
     */
    private @NotNull String mapLanguageToPrism(@Nullable String languageInfo) {
        if (languageInfo == null || languageInfo.isEmpty()) {
            return "plaintext";
        }
        
        String lang = languageInfo.trim().toLowerCase();
        
        // Map common language identifiers to PrismJS language classes
        return switch (lang) {
            case "js", "javascript" -> "javascript";
            case "ts", "typescript" -> "typescript";
            case "py", "python" -> "python";
            case "java" -> "java";
            case "c#", "csharp", "cs" -> "csharp";
            case "c++" -> "cpp";
            case "go" -> "go";
            case "rust" -> "rust";
            case "rb", "ruby" -> "ruby";
            case "kt", "kotlin" -> "kotlin";
            case "json" -> "json";
            case "yaml", "yml" -> "yaml";
            case "html" -> "markup";
            case "css" -> "css";
            case "sh", "bash" -> "bash";
            case "md", "markdown" -> "markdown";
            case "sql" -> "sql";
            case "docker", "dockerfile" -> "docker";
            case "dart" -> "dart";
            case "graphql" -> "graphql";
            case "hcl" -> "hcl";
            case "nginx" -> "nginx";
            case "powershell", "ps" -> "powershell";
            // Add more language mappings as needed
            default -> "plaintext";
        };
    }
    
    /**
     * Escape HTML special characters in a string.
     *
     * @param text The text to escape
     * @return Escaped text
     */
    private @NotNull String escapeHtml(@NotNull String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
    
    /**
     * Format metadata information for display in the WebView.
     * 
     * @param chatMessageContext The chat message context
     * @return HTML string with formatted metadata
     */
    private @NotNull String formatMetadata(@NotNull ChatMessageContext chatMessageContext) {
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM ''yy HH:mm");
        String timestamp = dateTime.format(formatter);
        
        String modelName = "Unknown";
        if (chatMessageContext.getLanguageModel() != null) {
            modelName = chatMessageContext.getLanguageModel().getModelName();
        }
        
        return "<div class=\"metadata-info\">" + timestamp + " · " + modelName + "</div>";
    }
}