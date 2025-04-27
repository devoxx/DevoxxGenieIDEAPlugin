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
        StringBuilder htmlBuilder = new StringBuilder();
        
        // Add welcome content
        String title = resourceBundle.getString("welcome.title");
        htmlBuilder.append("<div class=\"container\">\n");
        htmlBuilder.append("    <h2>").append(title).append("</h2>\n")
                .append("    <p>Follow us on Bluesky : <a href=\"https://bsky.app/profile/devoxxgenie.bsky.social\" target=\"_blank\">@DevoxGenie.bsky.social</a></p>\n")
                .append("    <p>").append(resourceBundle.getString("welcome.description")).append("</p>\n")
                .append("    <p>").append(resourceBundle.getString("welcome.instructions")).append("</p>\n")
                
                .append(generateFeaturesSection())
                .append(generateUtilityCommandsSection())
                
                .append("    <p>").append(resourceBundle.getString("welcome.tip")).append("</p>\n")
                .append("    <p>").append(resourceBundle.getString("welcome.enjoy")).append("</p>\n")
                .append("    <p class=\"subtext\">BTW If you like this plugin please give us a <a href=\"https://plugins.jetbrains.com/plugin/24169-devoxxgenie/reviews?noRedirect=true\" target=\"_blank\">review</a> ❤️</p>\n")
                .append("</div>\n");
        
        return htmlBuilder.toString();
    }
    
    /**
     * Generate the features section of the welcome content.
     * 
     * @return Features section HTML as a string
     */
    private @NotNull String generateFeaturesSection() {
        return "    <h2>Features 🚀</h2>\n" +
                "    <p class=\"subtext\">Configure features in the settings page.</p>\n" +
                "    <ul>\n" +
                "        <li><span class=\"feature-emoji\">🔥</span><span class=\"feature-name\">MCP Support:</span> You can now add MCP servers!</li>\n" +
                "        <li><span class=\"feature-emoji\">🗄️</span><span class=\"feature-name\">DEVOXXGENIE.md:</span> Generate project info for extra system instructions</li>\n" +
                "        <li><span class=\"feature-emoji\">🎹</span><span class=\"feature-name\">Define submit shortcode:</span> You can now define the keyboard shortcode to submit a prompt in settings.</li>\n" +
                "        <li><span class=\"feature-emoji\">📸</span><span class=\"feature-name\">DnD images:</span> You can now DnD images with multimodal LLM's.</li>\n" +
                "        <li><span class=\"feature-emoji\">🧐</span><span class=\"feature-name\">RAG Support:</span> Retrieval-Augmented Generation (RAG) support for automatically incorporating project context into your prompts.</li>\n" +
                "        <li><span class=\"feature-emoji\">💪🏻</span><span class=\"feature-name\">Git Diff:</span> Show Git Diff dialog to commit LLM suggestions</li>\n" +
                "        <li><span class=\"feature-emoji\">❌</span><span class=\"feature-name\">.gitignore:</span> Exclude files and directories based on .gitignore file</li>\n" +
                "        <li><span class=\"feature-emoji\">👀</span><span class=\"feature-name\">Chat History:</span> All chats are saved and can be restored or removed</li>\n" +
                "        <li><span class=\"feature-emoji\">🧠</span><span class=\"feature-name\">Project Scanner:</span> Add source code (full project or by package) to prompt context (or clipboard) when using Anthropic, OpenAI or Gemini.</li>\n" +
                "        <li><span class=\"feature-emoji\">💰</span><span class=\"feature-name\">Token Cost Calculator:</span> Calculate the cost when using Cloud LLM providers. Input/Output token prices can be viewed in the Settings page.</li>\n" +
                "        <li><span class=\"feature-emoji\">🔍</span><span class=\"feature-name\">Web Search:</span> Search the web for a given query using Google or Tavily</li>\n" +
                "        <li><span class=\"feature-emoji\">🏎️</span><span class=\"feature-name\">Streaming responses:</span> See each token as it's received from the LLM in real-time</li>\n" +
                "    </ul>\n";
    }
    
    /**
     * Generate the utility commands section of the welcome content.
     * 
     * @return Utility commands section HTML as a string
     */
    private @NotNull String generateUtilityCommandsSection() {
        return "    <h2>Utility Commands:</h2>\n" +
                "    <p class=\"subtext\">You can update the prompts for each utility command or add custom ones in the settings page.</p>\n" +
                "    <ul>\n" +
                "        " + HelpUtil.getCustomPromptCommandsForWebView() + "\n" +
                "    </ul>\n";
    }
}
