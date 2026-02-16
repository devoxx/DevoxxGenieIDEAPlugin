package com.devoxx.genie.ui.webview.template;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.EditorFontUtil;
import com.devoxx.genie.ui.util.ThemeDetector;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConversationTemplateTest {

    @Mock
    private WebServer webServer;

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private Application application;

    private MockedStatic<ThemeDetector> themeDetectorStatic;
    private MockedStatic<EditorFontUtil> editorFontUtilStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;
    private MockedStatic<ApplicationManager> appManagerStatic;
    private MockedStatic<ResourceLoader> resourceLoaderStatic;

    @BeforeEach
    void setUp() {
        themeDetectorStatic = mockStatic(ThemeDetector.class);
        themeDetectorStatic.when(ThemeDetector::isDarkTheme).thenReturn(false);

        editorFontUtilStatic = mockStatic(EditorFontUtil.class);
        editorFontUtilStatic.when(EditorFontUtil::getEditorFontSize).thenReturn(14);

        stateServiceStatic = mockStatic(DevoxxGenieStateService.class);
        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        appManagerStatic = mockStatic(ApplicationManager.class);
        appManagerStatic.when(ApplicationManager::getApplication).thenReturn(application);

        // Setup state service defaults
        when(stateService.getLineHeight()).thenReturn(1.6);
        when(stateService.getMessagePadding()).thenReturn(10);
        when(stateService.getMessageMargin()).thenReturn(10);
        when(stateService.getBorderWidth()).thenReturn(4);
        when(stateService.getCornerRadius()).thenReturn(4);
        when(stateService.getUserMessageBorderColor()).thenReturn("#FF5400");
        when(stateService.getAssistantMessageBorderColor()).thenReturn("#0095C9");
        when(stateService.getUserMessageBackgroundColor()).thenReturn("#fff9f0");
        when(stateService.getAssistantMessageBackgroundColor()).thenReturn("#f0f7ff");
        when(stateService.getUserMessageTextColor()).thenReturn("#000000");
        when(stateService.getAssistantMessageTextColor()).thenReturn("#000000");
        when(stateService.getCustomFontSize()).thenReturn(14);
        when(stateService.getCustomCodeFontSize()).thenReturn(14);
        when(stateService.getUseRoundedCorners()).thenReturn(true);
        when(stateService.getUseCustomColors()).thenReturn(false);

        // Setup WebServer
        when(webServer.getPrismCssUrl()).thenReturn("http://localhost/prism.css");
        when(webServer.getPrismJsUrl()).thenReturn("http://localhost/prism.js");

        // Setup resource loader
        resourceLoaderStatic = mockStatic(ResourceLoader.class);
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/html/conversation.html"))
                .thenReturn("<html><head><link rel=\"stylesheet\" href=\"${prismCssUrl}\">${styles}</head><body><div id=\"conversation-container\"></div>${scripts}</body></html>");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/css/theme-variables.css"))
                .thenReturn(":root { --bg: #fff; }");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/css/dark-theme.css"))
                .thenReturn(":root { --bg-dark: #1e1e1e; }");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/css/conversation.css"))
                .thenReturn("body { font-size: ${fontSize}px; } .meta { font-size: ${metadataFontSize}px; } .btn { font-size: ${buttonFontSize}px; } h1 { font-size: ${headerFontSize}px; } .sub { font-size: ${subtextFontSize}px; } .path { font-size: ${filePathFontSize}px; }");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/css/appearance-custom.css"))
                .thenReturn(".custom { line-height: ${lineHeight}; padding: ${messagePadding}px; margin: ${messageMargin}px; border-width: ${borderWidth}px; border-radius: ${cornerRadius}px; }");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/css/mcp-formatting.css"))
                .thenReturn(".mcp { color: blue; }");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/css/external-links.css"))
                .thenReturn(".external-link { text-decoration: underline; }");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/js/script-loader.js"))
                .thenReturn("function loadScriptContent(id, content) {}");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/js/conversation.js"))
                .thenReturn("// conversation.js");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/js/file-references.js"))
                .thenReturn("// file-references.js");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/js/mcp-handler.js"))
                .thenReturn("// mcp-handler.js");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/js/external-link-handler.js"))
                .thenReturn("// external-link-handler.js");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/js/lib/turndown.js"))
                .thenReturn("// turndown.js");
    }

    @AfterEach
    void tearDown() {
        themeDetectorStatic.close();
        editorFontUtilStatic.close();
        stateServiceStatic.close();
        appManagerStatic.close();
        resourceLoaderStatic.close();
    }

    @Test
    void generate_producesValidHtml() {
        ConversationTemplate template = new ConversationTemplate(webServer);

        String result = template.generate();

        assertThat(result).contains("<html>");
        assertThat(result).contains("</html>");
        assertThat(result).contains("conversation-container");
    }

    @Test
    void generate_replacesPrismCssUrl() {
        ConversationTemplate template = new ConversationTemplate(webServer);

        String result = template.generate();

        assertThat(result).contains("http://localhost/prism.css");
        assertThat(result).doesNotContain("${prismCssUrl}");
    }

    @Test
    void generate_containsStylesAndScripts() {
        ConversationTemplate template = new ConversationTemplate(webServer);

        String result = template.generate();

        assertThat(result).contains("<style>");
        assertThat(result).contains("</style>");
        assertThat(result).contains("<script");
        assertThat(result).doesNotContain("${styles}");
        assertThat(result).doesNotContain("${scripts}");
    }

    @Test
    void generate_includesDarkThemeWhenDarkMode() {
        themeDetectorStatic.when(ThemeDetector::isDarkTheme).thenReturn(true);

        ConversationTemplate template = new ConversationTemplate(webServer);

        String result = template.generate();

        assertThat(result).contains("--bg-dark: #1e1e1e;");
    }

    @Test
    void generate_excludesDarkThemeWhenLightMode() {
        themeDetectorStatic.when(ThemeDetector::isDarkTheme).thenReturn(false);

        ConversationTemplate template = new ConversationTemplate(webServer);

        String result = template.generate();

        assertThat(result).doesNotContain("--bg-dark: #1e1e1e;");
    }

    @Test
    void generate_replacesAllFontSizePlaceholders() {
        ConversationTemplate template = new ConversationTemplate(webServer);

        String result = template.generate();

        assertThat(result).doesNotContain("${fontSize}");
        assertThat(result).doesNotContain("${metadataFontSize}");
        assertThat(result).doesNotContain("${buttonFontSize}");
        assertThat(result).doesNotContain("${headerFontSize}");
        assertThat(result).doesNotContain("${subtextFontSize}");
        assertThat(result).doesNotContain("${filePathFontSize}");
    }

    @Test
    void generate_includesCustomColorsWhenEnabled() {
        when(stateService.getUseCustomColors()).thenReturn(true);

        ConversationTemplate template = new ConversationTemplate(webServer);

        String result = template.generate();

        assertThat(result).contains("padding: 10px");
        assertThat(result).contains("border-width: 4px");
    }

    @Test
    void generate_excludesCustomColorsWhenDisabled() {
        when(stateService.getUseCustomColors()).thenReturn(false);

        ConversationTemplate template = new ConversationTemplate(webServer);

        String result = template.generate();

        // Custom appearance CSS should NOT be in the output when disabled
        assertThat(result).doesNotContain("line-height: 1.6");
    }

    @Test
    void generate_containsMCPFormatting() {
        ConversationTemplate template = new ConversationTemplate(webServer);

        String result = template.generate();

        assertThat(result).contains(".mcp { color: blue; }");
    }

    @Test
    void generate_containsExternalLinksStyles() {
        ConversationTemplate template = new ConversationTemplate(webServer);

        String result = template.generate();

        assertThat(result).contains(".external-link { text-decoration: underline; }");
    }

    @Test
    void generate_containsScriptTags() {
        ConversationTemplate template = new ConversationTemplate(webServer);

        String result = template.generate();

        assertThat(result).contains("script-loader");
        assertThat(result).contains("http://localhost/prism.js");
        assertThat(result).contains("loadScriptContent");
    }

    @Test
    void escapeJS_escapesBackslash() {
        ConversationTemplate template = new ConversationTemplate(webServer);

        String result = template.escapeJS("path\\to\\file");

        assertThat(result).isEqualTo("path\\\\to\\\\file");
    }

    @Test
    void escapeJS_escapesBacktick() {
        ConversationTemplate template = new ConversationTemplate(webServer);

        String result = template.escapeJS("hello `world`");

        assertThat(result).isEqualTo("hello \\`world\\`");
    }

    @Test
    void escapeJS_escapesDollarBrace() {
        ConversationTemplate template = new ConversationTemplate(webServer);

        String result = template.escapeJS("value is ${name}");

        assertThat(result).isEqualTo("value is \\${name}");
    }
}
