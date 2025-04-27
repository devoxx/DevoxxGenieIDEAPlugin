package com.devoxx.genie.ui.webview;

import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.util.WelcomeUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.Getter;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ResourceBundle;

/**
 * Controller for managing the Welcome WebView component.
 * This class handles the display of welcome content in a WebView.
 */
public class WelcomeWebViewController implements CustomPromptChangeListener {
    private static final Logger LOG = Logger.getInstance(WelcomeWebViewController.class);

    /**
     * -- GETTER --
     * Get the JCef browser component.
     *
     * @return The JBCefBrowser instance
     */
    @Getter
    private final JBCefBrowser browser;
    private final WebServer webServer;
    private boolean isLoaded = false;
    private final ResourceBundle resourceBundle;
    private String resourceId;

    /**
     * Creates a new WelcomeWebViewController with a fresh browser.
     *
     * @param resourceBundle The resource bundle for i18n
     */
    public WelcomeWebViewController(@NotNull ResourceBundle resourceBundle) {
        this.webServer = WebServer.getInstance();
        this.resourceBundle = resourceBundle;
        
        // Ensure web server is running
        if (!webServer.isRunning()) {
            webServer.start();
        }
        
        // Create HTML content for the welcome panel
        String htmlContent = generateWelcomeHtml();
        
        // Register content with the web server to get a URL
        resourceId = webServer.addDynamicResource(htmlContent);
        String resourceUrl = webServer.getResourceUrl(resourceId);
        
        LOG.info("Loading Welcome WebView content from: " + resourceUrl);
        
        // Create browser and load content
        browser = WebViewFactory.createBrowser(resourceUrl);
        
        // Add load handler to detect when page is fully loaded
        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                isLoaded = true;
                LOG.info("Welcome WebView loaded with status: " + httpStatusCode);
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
     * Generate HTML content for the welcome panel.
     *
     * @return HTML content as a string
     */
    private @NotNull String generateWelcomeHtml() {
        StringBuilder htmlBuilder = new StringBuilder();
        
        // Start HTML document with references to PrismJS resources
        htmlBuilder.append("<!DOCTYPE html>\n")
                .append("<html>\n")
                .append("<head>\n")
                .append("    <meta charset=\"utf-8\">\n")
                .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("    <title>DevoxxGenie Welcome</title>\n")
                .append("    <link rel=\"stylesheet\" href=\"").append(webServer.getPrismCssUrl()).append("\">\n")
                .append("    <style>\n")
                .append("        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif; line-height: 1.6; margin: 0; padding: 20px; background-color: #000000; color: #e0e0e0; }\n")
                .append("        h2 { margin-top: 20px; margin-bottom: 10px; color: #64b5f6; }\n")
                .append("        a { color: #64b5f6; text-decoration: none; }\n")
                .append("        a:hover { text-decoration: underline; }\n")
                .append("        ul { padding-left: 20px; }\n")
                .append("        li { margin-bottom: 8px; }\n")
                .append("        .feature-emoji { margin-right: 5px; }\n")
                .append("        .feature-name { font-weight: bold; }\n")
                .append("        .subtext { font-size: 0.9em; color: #aaaaaa; margin-top: 5px; }\n")
                .append("        .container { max-width: 800px; margin: 0 auto; }\n")
                .append("    </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("<div class=\"container\">\n");
        
        // Add welcome content - we'll get this from WelcomeUtil but adapt it for WebView
        String title = resourceBundle.getString("welcome.title");
        htmlBuilder.append("    <h2>").append(title).append("</h2>\n")
                .append("    <p>Follow us on Bluesky : <a href=\"https://bsky.app/profile/devoxxgenie.bsky.social\" target=\"_blank\">@DevoxGenie.bsky.social</a></p>\n")
                .append("    <p>").append(resourceBundle.getString("welcome.description")).append("</p>\n")
                .append("    <p>").append(resourceBundle.getString("welcome.instructions")).append("</p>\n")
                
                .append("    <h2>Features 🚀</h2>\n")
                .append("    <p class=\"subtext\">Configure features in the settings page.</p>\n")
                .append("    <ul>\n")
                .append("        <li><span class=\"feature-emoji\">🔥</span><span class=\"feature-name\">MCP Support:</span> You can now add MCP servers!</li>\n")
                .append("        <li><span class=\"feature-emoji\">🗄️</span><span class=\"feature-name\">DEVOXXGENIE.md:</span> Generate project info for extra system instructions</li>\n")
                .append("        <li><span class=\"feature-emoji\">🎹</span><span class=\"feature-name\">Define submit shortcode:</span> You can now define the keyboard shortcode to submit a prompt in settings.</li>\n")
                .append("        <li><span class=\"feature-emoji\">📸</span><span class=\"feature-name\">DnD images:</span> You can now DnD images with multimodal LLM's.</li>\n")
                .append("        <li><span class=\"feature-emoji\">🧐</span><span class=\"feature-name\">RAG Support:</span> Retrieval-Augmented Generation (RAG) support for automatically incorporating project context into your prompts.</li>\n")
                .append("        <li><span class=\"feature-emoji\">💪🏻</span><span class=\"feature-name\">Git Diff:</span> Show Git Diff dialog to commit LLM suggestions</li>\n")
                .append("        <li><span class=\"feature-emoji\">❌</span><span class=\"feature-name\">.gitignore:</span> Exclude files and directories based on .gitignore file</li>\n")
                .append("        <li><span class=\"feature-emoji\">👀</span><span class=\"feature-name\">Chat History:</span> All chats are saved and can be restored or removed</li>\n")
                .append("        <li><span class=\"feature-emoji\">🧠</span><span class=\"feature-name\">Project Scanner:</span> Add source code (full project or by package) to prompt context (or clipboard) when using Anthropic, OpenAI or Gemini.</li>\n")
                .append("        <li><span class=\"feature-emoji\">💰</span><span class=\"feature-name\">Token Cost Calculator:</span> Calculate the cost when using Cloud LLM providers. Input/Output token prices can be viewed in the Settings page.</li>\n")
                .append("        <li><span class=\"feature-emoji\">🔍</span><span class=\"feature-name\">Web Search:</span> Search the web for a given query using Google or Tavily</li>\n")
                .append("        <li><span class=\"feature-emoji\">🏎️</span><span class=\"feature-name\">Streaming responses:</span> See each token as it's received from the LLM in real-time</li>\n")
                .append("    </ul>\n")
                
                .append("    <h2>Utility Commands:</h2>\n")
                .append("    <p class=\"subtext\">You can update the prompts for each utility command or add custom ones in the settings page.</p>\n")
                .append("    <ul>\n")
                .append("        ").append(getFormattedCustomPromptCommands()).append("\n")
                .append("    </ul>\n")
                
                .append("    <p>").append(resourceBundle.getString("welcome.tip")).append("</p>\n")
                .append("    <p>").append(resourceBundle.getString("welcome.enjoy")).append("</p>\n")
                .append("    <p class=\"subtext\">BTW If you like this plugin please give us a <a href=\"https://plugins.jetbrains.com/plugin/24169-devoxxgenie/reviews?noRedirect=true\" target=\"_blank\">review</a> ❤️</p>\n")
                .append("</div>\n")
                .append("</body>\n")
                .append("</html>");
        
        return htmlBuilder.toString();
    }
    
    /**
     * Get the formatted custom prompt commands from HelpUtil
     * but ensure proper HTML formatting.
     *
     * @return HTML-formatted commands as a string
     */
    private String getFormattedCustomPromptCommands() {
        // Use the helper method from HelpUtil to get properly formatted HTML commands
        return com.devoxx.genie.ui.util.HelpUtil.getCustomPromptCommandsForWebView();
    }

    /**
     * Update the welcome content when custom prompts change.
     */
    @Override
    public void onCustomPromptsChanged() {
        // Create fresh HTML content
        String htmlContent = generateWelcomeHtml();
        
        // Update the resource in the web server
        resourceId = webServer.addDynamicResource(htmlContent);
        String resourceUrl = webServer.getResourceUrl(resourceId);
        
        // Reload the browser with the new content
        browser.loadURL(resourceUrl);
    }
}
