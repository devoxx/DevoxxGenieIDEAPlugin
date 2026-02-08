package com.devoxx.genie.ui.webview.template;

import com.devoxx.genie.model.welcome.WelcomeAnnouncement;
import com.devoxx.genie.model.welcome.WelcomeContent;
import com.devoxx.genie.model.welcome.WelcomeFeature;
import com.devoxx.genie.model.welcome.WelcomeSocialLink;
import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.ui.util.HelpUtil;
import com.devoxx.genie.ui.webview.WebServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Template for generating the welcome panel HTML content.
 * Supports both remote content (from WelcomeContentService) and local fallback (from ResourceBundle).
 */
public class WelcomeTemplate extends HtmlTemplate {

    private static final String DEFAULT_REVIEW_URL =
            "https://plugins.jetbrains.com/plugin/24169-devoxxgenie/reviews?noRedirect=true";

    private final ResourceBundle resourceBundle;
    private final WelcomeContent remoteContent;

    public WelcomeTemplate(WebServer webServer, ResourceBundle resourceBundle) {
        this(webServer, resourceBundle, null);
    }

    public WelcomeTemplate(WebServer webServer, ResourceBundle resourceBundle, @Nullable WelcomeContent remoteContent) {
        super(webServer);
        this.resourceBundle = resourceBundle;
        this.remoteContent = remoteContent;
    }

    @Override
    public @NotNull String generate() {
        String htmlTemplate = ResourceLoader.loadResource("webview/html/welcome.html");
        String customPromptCommands = HelpUtil.getCustomPromptCommandsForWebView();

        if (remoteContent != null) {
            return generateFromRemoteContent(htmlTemplate, customPromptCommands);
        }
        return generateFromResourceBundle(htmlTemplate, customPromptCommands);
    }

    @NotNull
    private String generateFromRemoteContent(@NotNull String htmlTemplate, @NotNull String customPromptCommands) {
        String title = escapeHtml(remoteContent.getTitle());
        String description = escapeHtml(remoteContent.getDescription());
        String instructions = escapeHtml(remoteContent.getInstructions());
        String tip = escapeHtml(remoteContent.getTip());
        String enjoy = escapeHtml(remoteContent.getEnjoy());
        String featuresHtml = buildFeaturesHtml(remoteContent.getFeatures());
        String announcementsHtml = buildAnnouncementsHtml(remoteContent.getAnnouncements());
        String socialLinksHtml = buildSocialLinksHtml(remoteContent.getSocialLinks());
        String reviewUrl = getReviewUrl(remoteContent.getReviewUrl());
        String trackingPixelHtml = buildTrackingPixelHtml(remoteContent.getTrackingPixelUrl());

        return htmlTemplate
                .replace("${title}", title)
                .replace("${description}", description)
                .replace("${instructions}", instructions)
                .replace("${tip}", tip)
                .replace("${enjoy}", enjoy)
                .replace("${features}", featuresHtml)
                .replace("${announcements}", announcementsHtml)
                .replace("${socialLinks}", socialLinksHtml)
                .replace("${reviewUrl}", reviewUrl)
                .replace("${trackingPixel}", trackingPixelHtml)
                .replace("${customPromptCommands}", customPromptCommands);
    }

    @NotNull
    private String generateFromResourceBundle(@NotNull String htmlTemplate, @NotNull String customPromptCommands) {
        String title = resourceBundle.getString("welcome.title");
        String description = resourceBundle.getString("welcome.description");
        String instructions = resourceBundle.getString("welcome.instructions");
        String tip = resourceBundle.getString("welcome.tip");
        String enjoy = resourceBundle.getString("welcome.enjoy");

        return htmlTemplate
                .replace("${title}", title)
                .replace("${description}", description)
                .replace("${instructions}", instructions)
                .replace("${tip}", tip)
                .replace("${enjoy}", enjoy)
                .replace("${features}", getDefaultFeaturesHtml())
                .replace("${announcements}", "")
                .replace("${socialLinks}", getDefaultSocialLinksHtml())
                .replace("${reviewUrl}", DEFAULT_REVIEW_URL)
                .replace("${trackingPixel}", "")
                .replace("${customPromptCommands}", customPromptCommands);
    }

    @NotNull
    private String buildFeaturesHtml(@Nullable List<WelcomeFeature> features) {
        if (features == null || features.isEmpty()) {
            return getDefaultFeaturesHtml();
        }
        StringBuilder sb = new StringBuilder();
        for (WelcomeFeature feature : features) {
            sb.append("<li><span class=\"feature-emoji\">")
                    .append(escapeHtml(feature.getEmoji()))
                    .append("</span><span class=\"feature-name\">")
                    .append(escapeHtml(feature.getName()))
                    .append(":</span> ")
                    .append(escapeHtml(feature.getDescription()))
                    .append("</li>\n");
        }
        return sb.toString();
    }

    @NotNull
    private String buildAnnouncementsHtml(@Nullable List<WelcomeAnnouncement> announcements) {
        if (announcements == null || announcements.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (WelcomeAnnouncement announcement : announcements) {
            String type = announcement.getType() != null ? announcement.getType() : "info";
            sb.append("<div class=\"announcement announcement-")
                    .append(escapeHtml(type))
                    .append("\">")
                    .append(escapeHtml(announcement.getMessage()))
                    .append("</div>\n");
        }
        return sb.toString();
    }

    @NotNull
    private String buildSocialLinksHtml(@Nullable List<WelcomeSocialLink> socialLinks) {
        if (socialLinks == null || socialLinks.isEmpty()) {
            return getDefaultSocialLinksHtml();
        }
        StringBuilder sb = new StringBuilder();
        for (WelcomeSocialLink link : socialLinks) {
            sb.append("<p>Follow us on ")
                    .append(escapeHtml(link.getPlatform()))
                    .append(" : <a href=\"")
                    .append(escapeHtml(link.getUrl()))
                    .append("\">")
                    .append(escapeHtml(link.getLabel()))
                    .append("</a></p>\n");
        }
        return sb.toString();
    }

    @NotNull
    private String buildTrackingPixelHtml(@Nullable String trackingPixelUrl) {
        if (trackingPixelUrl == null || trackingPixelUrl.isEmpty()) {
            return "";
        }
        // Only allow HTTPS URLs for security
        if (!trackingPixelUrl.startsWith("https://")) {
            return "";
        }
        String separator = trackingPixelUrl.contains("?") ? "&" : "?";
        String version = getPluginVersion();
        String os = URLEncoder.encode(System.getProperty("os.name", "unknown"), StandardCharsets.UTF_8);
        String fullUrl = trackingPixelUrl + separator + "v=" + version + "&os=" + os;
        return "<img src=\"" + escapeHtml(fullUrl) + "\" width=\"1\" height=\"1\" style=\"display:none\" alt=\"\">";
    }

    @NotNull
    private String getPluginVersion() {
        try {
            return PropertiesService.getInstance().getVersion();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @NotNull
    private String getReviewUrl(@Nullable String reviewUrl) {
        if (reviewUrl == null || reviewUrl.isEmpty()) {
            return DEFAULT_REVIEW_URL;
        }
        return escapeHtml(reviewUrl);
    }

    @NotNull
    private String getDefaultFeaturesHtml() {
        return """
                <li><span class="feature-emoji">🛍️</span><span class="feature-name">MCP Marketplace:</span> Discover and install MCP servers directly from Settings.</li>
                <li><span class="feature-emoji">📂</span><span class="feature-name">@ Opens Files Popup:</span> Type <code>@</code> in your prompt to open the file selection dialog.</li>
                <li><span class="feature-emoji">🎨</span><span class="feature-name">Appearance:</span> Change the conversation appearances</li>
                <li><span class="feature-emoji">🔥</span><span class="feature-name">MCP Support:</span> You can now add MCP servers!</li>
                <li><span class="feature-emoji">🗄️</span><span class="feature-name">DEVOXXGENIE.md:</span> Generate project info for extra system instructions</li>
                <li><span class="feature-emoji">🎹</span><span class="feature-name">Define submit shortcode:</span> You can now define the keyboard shortcode to submit a prompt in settings.</li>
                <li><span class="feature-emoji">📸</span><span class="feature-name">DnD images:</span> You can now DnD images with multimodal LLM's.</li>
                <li><span class="feature-emoji">🧐</span><span class="feature-name">RAG Support:</span> Retrieval-Augmented Generation (RAG) support for automatically incorporating project context into your prompts.</li>
                <li><span class="feature-emoji">❌</span><span class="feature-name">.gitignore:</span> Exclude files and directories based on .gitignore file</li>
                <li><span class="feature-emoji">👀</span><span class="feature-name">Chat History:</span> All chats are saved and can be restored or removed</li>
                <li><span class="feature-emoji">🧠</span><span class="feature-name">Project Scanner:</span> Add source code (full project or by package) to prompt context (or clipboard) when using Anthropic, OpenAI or Gemini.</li>
                <li><span class="feature-emoji">🤖</span><span class="feature-name">Sub-Agents:</span> Parallel codebase exploration using multiple sub-agents. Enable in Agent Settings and ask the LLM to explore your project!</li>
                """;
    }

    @NotNull
    private String getDefaultSocialLinksHtml() {
        return "<p>Follow us on Bluesky : <a href=\"https://bsky.app/profile/devoxxgenie.bsky.social\">@DevoxxGenie.bsky.social</a></p>";
    }
}
