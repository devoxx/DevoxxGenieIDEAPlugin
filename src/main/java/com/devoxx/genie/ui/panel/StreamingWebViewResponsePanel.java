package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.panel.chatresponse.ResponseHeaderPanel;
import com.devoxx.genie.ui.webview.WebServer;
import com.devoxx.genie.ui.webview.WebViewFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A WebView-based panel for streaming chat responses.
 * Uses JCEF browser with PrismJS for syntax highlighting.
 */
public class StreamingWebViewResponsePanel extends BackgroundPanel {
    private static final Logger LOG = Logger.getInstance(StreamingWebViewResponsePanel.class);

    private final JBCefBrowser browser;
    private final WebServer webServer;
    private final ChatMessageContext chatMessageContext;
    private final StringBuilder markdownContent = new StringBuilder();
    private final AtomicBoolean isLoaded = new AtomicBoolean(false);
    private final Parser parser = Parser.builder().build();

    /**
     * Create a new streaming response panel with WebView.
     *
     * @param chatMessageContext the chat message context
     */
    public StreamingWebViewResponsePanel(@NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getId());
        this.chatMessageContext = chatMessageContext;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new ResponseHeaderPanel(chatMessageContext));

        // Initialize web server
        webServer = WebServer.getInstance();
        if (!webServer.isRunning()) {
            webServer.start();
        }

        // Create initial HTML content with PrismJS
        String htmlContent = createInitialHtml();
        String resourcePath = webServer.addDynamicResource(htmlContent);
        String resourceUrl = webServer.getResourceUrl(resourcePath);

        // Create and initialize browser
        browser = WebViewFactory.createBrowser(resourceUrl);

        // Add load handler
        JBCefClient client = browser.getJBCefClient();
        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                isLoaded.set(true);
                LOG.info("StreamingWebView loaded with status: " + httpStatusCode);
            }
        }, browser.getCefBrowser());

        // Add browser component to panel
        add(browser.getComponent());

        // Set maximum size for proper display
        Dimension maximumSize = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        browser.getComponent().setMaximumSize(maximumSize);
        browser.getComponent().setMinimumSize(new Dimension(getPreferredSize().width, getPreferredSize().height));

        // Add file list if available
        if (!FileListManager.getInstance().isEmpty(chatMessageContext.getProject())) {
            java.util.List<VirtualFile> files = FileListManager.getInstance().getFiles(chatMessageContext.getProject());
            ExpandablePanel fileListPanel = new ExpandablePanel(chatMessageContext, files);
            add(fileListPanel);
        }
    }

    /**
     * Insert token into document stream.
     *
     * @param token the LLM string token
     */
    public void insertToken(String token) {
        ApplicationManager.getApplication().invokeLater(() -> {
            markdownContent.append(token);
            
            // Update the browser content using JavaScript
            if (isLoaded.get()) {
                updateBrowserContent(markdownContent.toString());
            }
        });
    }

    /**
     * Update the browser content with JavaScript.
     *
     * @param markdown the current markdown content
     */
    private void updateBrowserContent(String markdown) {
        // Escape JavaScript string content
        String escapedMarkdown = markdown
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        
        // Use JavaScript to update the content and apply syntax highlighting
        String script = "updateAssistantContent('" + escapedMarkdown + "');";
        browser.getCefBrowser().executeJavaScript(script, browser.getCefBrowser().getURL(), 0);
    }

    /**
     * Create initial HTML with PrismJS.
     *
     * @return the HTML content
     */
    private String createInitialHtml() {
        // Get user query for display
        String userQuery = chatMessageContext.getUserPrompt();
        String escapedUserQuery = userQuery.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>");

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>DevoxxGenie Streaming Response</title>
                    <link rel="stylesheet" href="%s">
                    <style>
                        body { 
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, 
                                         Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif; 
                            line-height: 1.6; 
                            margin: 0; 
                            padding: 10px; 
                            color: #333;
                            background-color: #f8f8f8;
                        }
                        pre { 
                            margin: 1em 0; 
                            border-radius: 4px;
                            position: relative;
                        }
                        code { 
                            font-family: 'JetBrains Mono', Consolas, Monaco, 'Andale Mono', 
                                        'Ubuntu Mono', monospace; 
                        }
                        .toolbar-container {
                            position: absolute;
                            top: 0;
                            right: 0;
                            padding: 5px;
                            opacity: 0.7;
                            transition: opacity 0.3s;
                        }
                        .toolbar-container:hover {
                            opacity: 1;
                        }
                        .copy-button {
                            background: rgba(255, 255, 255, 0.2);
                            border: none;
                            border-radius: 3px;
                            color: #fff;
                            cursor: pointer;
                            font-size: 12px;
                            padding: 3px 8px;
                        }
                        .copy-button:hover {
                            background: rgba(255, 255, 255, 0.3);
                        }
                        #content {
                            min-height: 100px;
                        }
                        .user-message {
                            background-color: #f2f8ff;
                            border-left: 4px solid #0077cc;
                            padding: 10px;
                            margin: 10px 0;
                            border-radius: 4px;
                        }
                        .assistant-message {
                            background-color: #f5f5f5;
                            border-left: 4px solid #4CAF50;
                            padding: 10px;
                            margin: 10px 0;
                            border-radius: 4px;
                        }
                    </style>
                </head>
                <body>
                    <div class="conversation">
                        <!-- User message -->
                        <div class="user-message">
                            <strong>User:</strong>
                            <p>%s</p>
                        </div>
                        
                        <!-- Assistant message (to be filled in dynamically) -->
                        <div class="assistant-message">
                            <strong>Assistant:</strong>
                            <div id="assistant-content"></div>
                        </div>
                    </div>
                    
                    <!-- Load PrismJS from CDN -->
                    <script src="%s"></script>
                    
                    <!-- Load language components -->
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-java.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-javascript.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-python.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-bash.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-typescript.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-csharp.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-cpp.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-kotlin.min.js"></script>
                    
                    <script>
                        // Simple Markdown to HTML converter
                        function markdownToHtml(markdown) {
                            let html = markdown;
                            
                            // Code blocks
                            html = html.replace(/```(\\w*)(\\n[\\s\\S]*?\\n)```/g, function(match, language, code) {
                                return '<pre><code class="language-' + (language || 'plaintext') + '">' + 
                                    code.trim().replace(/</g, '&lt;').replace(/>/g, '&gt;') + 
                                    '</code></pre>';
                            });
                            
                            // Headers
                            html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>');
                            html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>');
                            html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>');
                            
                            // Paragraphs
                            html = html.replace(/^(?!<h|<pre|<ul|<ol|<li)[^\\n]+$/gm, function(match) {
                                return '<p>' + match + '</p>';
                            });
                            
                            // Lists
                            html = html.replace(/^\\* (.+)$/gm, '<ul><li>$1</li></ul>');
                            
                            return html;
                        }
                        
                        // Update assistant content and highlight code
                        function updateAssistantContent(markdown) {
                            const contentDiv = document.getElementById('assistant-content');
                            contentDiv.innerHTML = markdownToHtml(markdown);
                            
                            // Apply PrismJS highlighting
                            if (typeof Prism !== 'undefined') {
                                Prism.highlightAll();
                                
                                // Add copy buttons to code blocks
                                document.querySelectorAll('pre').forEach(function(pre) {
                                    // Skip if already has a toolbar
                                    if (pre.querySelector('.toolbar-container')) return;
                                    
                                    const container = document.createElement('div');
                                    container.className = 'toolbar-container';
                                    
                                    const copyButton = document.createElement('button');
                                    copyButton.className = 'copy-button';
                                    copyButton.textContent = 'Copy';
                                    
                                    copyButton.addEventListener('click', function() {
                                        const code = pre.querySelector('code');
                                        const text = code.textContent;
                                        
                                        navigator.clipboard.writeText(text).then(function() {
                                            copyButton.textContent = 'Copied!';
                                            setTimeout(function() {
                                                copyButton.textContent = 'Copy';
                                            }, 2000);
                                        }).catch(function(err) {
                                            console.error('Failed to copy: ', err);
                                        });
                                    });
                                    
                                    container.appendChild(copyButton);
                                    pre.appendChild(container);
                                });
                            }
                        }
                        
                        // Initialize
                        document.addEventListener('DOMContentLoaded', function() {
                            updateAssistantContent('');
                        });
                    </script>
                </body>
                </html>
                """.formatted(
                webServer.getPrismCssUrl(),
                escapedUserQuery,
                webServer.getPrismJsUrl()
        );
    }
//
//    @Override
//    public void dispose() {
//        if (browser != null) {
//            browser.dispose();
//        }
//        super.dispose();
//    }
}
