package com.devoxx.genie.service.welcome;

import com.devoxx.genie.model.welcome.WelcomeContent;
import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WelcomeContentServiceTest {

    private final Gson gson = new GsonBuilder().create();

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
        when(stateService.getWelcomeContentCachedJson()).thenReturn("");
        when(stateService.getWelcomeContentLastFetchTimestamp()).thenReturn(0L);
        when(stateService.getWelcomeContentPluginVersion()).thenReturn("");
    }

    @AfterEach
    void tearDown() {
        appManagerStatic.close();
        stateServiceStatic.close();
        propertiesServiceStatic.close();
    }

    @Test
    void getWelcomeContent_returnsNullWhenNoCacheExists() {
        WelcomeContentService service = new WelcomeContentService();

        WelcomeContent result = service.getWelcomeContent();

        assertNull(result, "Should return null when no cache exists");
    }

    @Test
    void getWelcomeContent_restoresFromPersistentCache() {
        String cachedJson = createSampleWelcomeJson(1);
        when(stateService.getWelcomeContentCachedJson()).thenReturn(cachedJson);
        when(stateService.getWelcomeContentLastFetchTimestamp()).thenReturn(System.currentTimeMillis());
        when(stateService.getWelcomeContentPluginVersion()).thenReturn("0.9.2");

        WelcomeContentService service = new WelcomeContentService();

        WelcomeContent result = service.getWelcomeContent();

        assertNotNull(result);
        assertEquals("Welcome to Devoxx Genie", result.getTitle());
        assertEquals(1, result.getSchemaVersion());
    }

    @Test
    void getWelcomeContent_ignoresUnsupportedSchemaVersion() {
        String cachedJson = createSampleWelcomeJson(999);
        when(stateService.getWelcomeContentCachedJson()).thenReturn(cachedJson);

        WelcomeContentService service = new WelcomeContentService();

        WelcomeContent result = service.getWelcomeContent();

        assertNull(result, "Should return null for unsupported schema version");
    }

    @Test
    void getWelcomeContent_returnsCachedContentOnSubsequentCalls() {
        String cachedJson = createSampleWelcomeJson(1);
        when(stateService.getWelcomeContentCachedJson()).thenReturn(cachedJson);
        when(stateService.getWelcomeContentLastFetchTimestamp()).thenReturn(System.currentTimeMillis());
        when(stateService.getWelcomeContentPluginVersion()).thenReturn("0.9.2");

        WelcomeContentService service = new WelcomeContentService();

        WelcomeContent first = service.getWelcomeContent();
        WelcomeContent second = service.getWelcomeContent();

        assertNotNull(first);
        assertSame(first, second, "Should return same cached instance");
    }

    @Test
    void parsesWelcomeJsonCorrectly() {
        String json = createFullSampleWelcomeJson();
        WelcomeContent content = gson.fromJson(json, WelcomeContent.class);

        assertNotNull(content);
        assertEquals(1, content.getSchemaVersion());
        assertEquals("Welcome to Devoxx Genie", content.getTitle());
        assertNotNull(content.getFeatures());
        assertEquals(2, content.getFeatures().size());
        assertEquals("MCP Marketplace", content.getFeatures().get(0).getName());
        assertNotNull(content.getSocialLinks());
        assertEquals(1, content.getSocialLinks().size());
        assertEquals("Bluesky", content.getSocialLinks().get(0).getPlatform());
        assertEquals("Enjoy!", content.getEnjoy());
        assertNotNull(content.getAnnouncements());
        assertTrue(content.getAnnouncements().isEmpty());
    }

    private String createSampleWelcomeJson(int schemaVersion) {
        return """
                {
                  "schemaVersion": %d,
                  "lastUpdated": "2026-02-07",
                  "title": "Welcome to Devoxx Genie",
                  "description": "Test description",
                  "instructions": "Test instructions",
                  "features": [],
                  "announcements": [],
                  "tip": "Test tip",
                  "enjoy": "Enjoy!",
                  "socialLinks": [],
                  "reviewUrl": "",
                  "trackingPixelUrl": ""
                }
                """.formatted(schemaVersion);
    }

    private String createFullSampleWelcomeJson() {
        return """
                {
                  "schemaVersion": 1,
                  "lastUpdated": "2026-02-07",
                  "title": "Welcome to Devoxx Genie",
                  "description": "The Devoxx Genie plugin allows you to interact with LLMs.",
                  "instructions": "Start by selecting a language model provider.",
                  "features": [
                    { "emoji": "\uD83D\uDECD\uFE0F", "name": "MCP Marketplace", "description": "Discover and install MCP servers." },
                    { "emoji": "\uD83D\uDCC2", "name": "@ Opens Files Popup", "description": "Type @ to open file selection." }
                  ],
                  "announcements": [],
                  "tip": "You can modify the endpoints in settings.",
                  "enjoy": "Enjoy!",
                  "socialLinks": [
                    { "platform": "Bluesky", "url": "https://bsky.app/profile/devoxxgenie.bsky.social", "label": "@DevoxxGenie.bsky.social" }
                  ],
                  "reviewUrl": "https://plugins.jetbrains.com/plugin/24169-devoxxgenie/reviews?noRedirect=true",
                  "trackingPixelUrl": ""
                }
                """;
    }
}
