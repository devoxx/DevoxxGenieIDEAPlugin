package com.devoxx.genie.ui.webview.template;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.welcome.*;
import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.webview.WebServer;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WelcomeTemplateTest {

    @Mock
    private WebServer webServer;

    @Mock
    private ResourceBundle resourceBundle;

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private PropertiesService propertiesService;

    @Mock
    private Application application;

    private MockedStatic<ApplicationManager> appManagerStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;
    private MockedStatic<PropertiesService> propertiesServiceStatic;

    @BeforeEach
    void setUp() {
        appManagerStatic = mockStatic(ApplicationManager.class);
        appManagerStatic.when(ApplicationManager::getApplication).thenReturn(application);

        stateServiceStatic = mockStatic(DevoxxGenieStateService.class);
        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        propertiesServiceStatic = mockStatic(PropertiesService.class);
        propertiesServiceStatic.when(PropertiesService::getInstance).thenReturn(propertiesService);
        when(propertiesService.getVersion()).thenReturn("0.9.2");

        // Setup custom prompts for HelpUtil
        List<CustomPrompt> customPrompts = new ArrayList<>();
        customPrompts.add(new CustomPrompt("test", "Run unit tests"));
        when(stateService.getCustomPrompts()).thenReturn(customPrompts);

        // Setup resource bundle
        when(resourceBundle.getString("welcome.title")).thenReturn("Welcome to DevoxxGenie");
        when(resourceBundle.getString("welcome.description")).thenReturn("Test description");
        when(resourceBundle.getString("welcome.instructions")).thenReturn("Test instructions");
        when(resourceBundle.getString("welcome.tip")).thenReturn("Test tip");
        when(resourceBundle.getString("welcome.enjoy")).thenReturn("Enjoy!");
    }

    @AfterEach
    void tearDown() {
        appManagerStatic.close();
        stateServiceStatic.close();
        propertiesServiceStatic.close();
    }

    @Test
    void generate_withNullRemoteContent_usesLocalFallback() {
        WelcomeTemplate template = new WelcomeTemplate(webServer, resourceBundle, null);

        String html = template.generate();

        assertNotNull(html);
        assertTrue(html.contains("Welcome to DevoxxGenie"));
        assertTrue(html.contains("Test description"));
        assertTrue(html.contains("MCP Marketplace"));
        assertTrue(html.contains("devoxxgenie.bsky.social"));
        assertTrue(html.contains("plugins.jetbrains.com"));
        assertFalse(html.contains("${"), "No unresolved placeholders should remain");
    }

    @Test
    void generate_withRemoteContent_usesRemoteData() {
        WelcomeContent content = createRemoteContent();

        WelcomeTemplate template = new WelcomeTemplate(webServer, resourceBundle, content);

        String html = template.generate();

        assertNotNull(html);
        assertTrue(html.contains("Remote Welcome Title"));
        assertTrue(html.contains("Remote description"));
        assertTrue(html.contains("Cool Feature"));
        assertTrue(html.contains("https://example.com/reviews"));
        assertFalse(html.contains("${"), "No unresolved placeholders should remain");
    }

    @Test
    void generate_withAnnouncements_rendersAnnouncementDivs() {
        WelcomeContent content = createRemoteContent();
        WelcomeAnnouncement announcement = new WelcomeAnnouncement();
        announcement.setType("info");
        announcement.setMessage("New version available!");
        content.setAnnouncements(List.of(announcement));

        WelcomeTemplate template = new WelcomeTemplate(webServer, resourceBundle, content);

        String html = template.generate();

        assertTrue(html.contains("announcement-info"));
        assertTrue(html.contains("New version available!"));
    }

    @Test
    void generate_withTrackingPixel_rendersImgTag() {
        WelcomeContent content = createRemoteContent();
        content.setTrackingPixelUrl("https://analytics.example.com/pixel.gif");

        WelcomeTemplate template = new WelcomeTemplate(webServer, resourceBundle, content);

        String html = template.generate();

        assertTrue(html.contains("https://analytics.example.com/pixel.gif?v=0.9.2&amp;os="));
        assertTrue(html.contains("width=\"1\""));
        assertTrue(html.contains("height=\"1\""));
    }

    @Test
    void generate_withHttpTrackingPixel_doesNotRender() {
        WelcomeContent content = createRemoteContent();
        content.setTrackingPixelUrl("http://insecure.example.com/pixel.gif");

        WelcomeTemplate template = new WelcomeTemplate(webServer, resourceBundle, content);

        String html = template.generate();

        assertFalse(html.contains("insecure.example.com"), "HTTP tracking pixels should not be rendered");
    }

    @Test
    void generate_withEmptyTrackingPixel_rendersNothing() {
        WelcomeContent content = createRemoteContent();
        content.setTrackingPixelUrl("");

        WelcomeTemplate template = new WelcomeTemplate(webServer, resourceBundle, content);

        String html = template.generate();

        assertFalse(html.contains("<img"), "Empty tracking pixel should not produce an img tag");
    }

    @Test
    void generate_escapesHtmlInRemoteContent() {
        WelcomeContent content = createRemoteContent();
        content.setTitle("Welcome <script>alert('xss')</script>");

        WelcomeTemplate template = new WelcomeTemplate(webServer, resourceBundle, content);

        String html = template.generate();

        assertFalse(html.contains("<script>"), "Script tags should be escaped");
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void generate_withSocialLinks_rendersLinks() {
        WelcomeContent content = createRemoteContent();
        WelcomeSocialLink link = new WelcomeSocialLink();
        link.setPlatform("Twitter");
        link.setUrl("https://twitter.com/devoxxgenie");
        link.setLabel("@DevoxxGenie");
        content.setSocialLinks(List.of(link));

        WelcomeTemplate template = new WelcomeTemplate(webServer, resourceBundle, content);

        String html = template.generate();

        assertTrue(html.contains("Twitter"));
        assertTrue(html.contains("https://twitter.com/devoxxgenie"));
        assertTrue(html.contains("@DevoxxGenie"));
    }

    @Test
    void generate_withNoConstructorArgs_usesLocalFallback() {
        WelcomeTemplate template = new WelcomeTemplate(webServer, resourceBundle);

        String html = template.generate();

        assertNotNull(html);
        assertTrue(html.contains("Welcome to DevoxxGenie"));
    }

    @Test
    void generate_withNullAnnouncementMessage_skipsAnnouncement() {
        WelcomeContent content = createRemoteContent();
        WelcomeAnnouncement announcement = new WelcomeAnnouncement();
        announcement.setType("info");
        announcement.setMessage(null); // null message — should be skipped
        content.setAnnouncements(List.of(announcement));

        WelcomeTemplate template = new WelcomeTemplate(webServer, resourceBundle, content);

        String html = template.generate();

        assertNotNull(html);
        assertFalse(html.contains("announcement-info"), "Announcement with null message should be skipped");
        assertFalse(html.contains("${"), "No unresolved placeholders should remain");
    }

    @Test
    void generate_withEmptyAnnouncementMessage_skipsAnnouncement() {
        WelcomeContent content = createRemoteContent();
        WelcomeAnnouncement announcement = new WelcomeAnnouncement();
        announcement.setType("warning");
        announcement.setMessage(""); // empty message — should be skipped
        content.setAnnouncements(List.of(announcement));

        WelcomeTemplate template = new WelcomeTemplate(webServer, resourceBundle, content);

        String html = template.generate();

        assertNotNull(html);
        assertFalse(html.contains("announcement-warning"), "Announcement with empty message should be skipped");
    }

    @Test
    void generate_withNullTitle_fallsBackToResourceBundle() {
        WelcomeContent content = createRemoteContent();
        content.setTitle(null); // null field — should fall back to resource bundle

        WelcomeTemplate template = new WelcomeTemplate(webServer, resourceBundle, content);

        String html = template.generate();

        assertNotNull(html);
        assertTrue(html.contains("Welcome to DevoxxGenie"), "Null remote title should fall back to resource bundle");
        assertFalse(html.contains("${"), "No unresolved placeholders should remain");
    }

    @Test
    void generate_withAllNullRemoteFields_fallsBackToResourceBundle() {
        WelcomeContent content = new WelcomeContent();
        content.setSchemaVersion(1);
        // All text fields are null

        WelcomeTemplate template = new WelcomeTemplate(webServer, resourceBundle, content);

        String html = template.generate();

        assertNotNull(html);
        assertTrue(html.contains("Welcome to DevoxxGenie"));
        assertTrue(html.contains("Test description"));
        assertTrue(html.contains("Test instructions"));
        assertFalse(html.contains("${"), "No unresolved placeholders should remain");
    }

    @Test
    void generate_withMixedNullAndValidAnnouncements_rendersOnlyValid() {
        WelcomeContent content = createRemoteContent();
        WelcomeAnnouncement nullMsg = new WelcomeAnnouncement();
        nullMsg.setType("info");
        nullMsg.setMessage(null);
        WelcomeAnnouncement validMsg = new WelcomeAnnouncement();
        validMsg.setType("success");
        validMsg.setMessage("This one is valid!");
        content.setAnnouncements(List.of(nullMsg, validMsg));

        WelcomeTemplate template = new WelcomeTemplate(webServer, resourceBundle, content);

        String html = template.generate();

        assertNotNull(html);
        assertFalse(html.contains("announcement-info"), "Null message announcement should be skipped");
        assertTrue(html.contains("announcement-success"), "Valid announcement should be rendered");
        assertTrue(html.contains("This one is valid!"));
    }

    private WelcomeContent createRemoteContent() {
        WelcomeContent content = new WelcomeContent();
        content.setSchemaVersion(1);
        content.setLastUpdated("2026-02-07");
        content.setTitle("Remote Welcome Title");
        content.setDescription("Remote description");
        content.setInstructions("Remote instructions");
        content.setTip("Remote tip");
        content.setEnjoy("Have fun!");

        WelcomeFeature feature = new WelcomeFeature();
        feature.setEmoji("🔥");
        feature.setName("Cool Feature");
        feature.setDescription("A cool feature description");
        content.setFeatures(List.of(feature));

        content.setAnnouncements(List.of());

        WelcomeSocialLink socialLink = new WelcomeSocialLink();
        socialLink.setPlatform("Bluesky");
        socialLink.setUrl("https://bsky.app/profile/devoxxgenie.bsky.social");
        socialLink.setLabel("@DevoxxGenie.bsky.social");
        content.setSocialLinks(List.of(socialLink));

        content.setReviewUrl("https://example.com/reviews");
        content.setTrackingPixelUrl("");

        return content;
    }
}
