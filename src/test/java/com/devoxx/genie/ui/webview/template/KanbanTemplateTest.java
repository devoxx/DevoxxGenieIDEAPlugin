package com.devoxx.genie.ui.webview.template;

import com.devoxx.genie.ui.util.ThemeDetector;
import com.devoxx.genie.ui.webview.WebServer;
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
class KanbanTemplateTest {

    @Mock
    private WebServer webServer;

    private MockedStatic<ThemeDetector> themeDetectorStatic;
    private MockedStatic<ResourceLoader> resourceLoaderStatic;

    @BeforeEach
    void setUp() {
        themeDetectorStatic = mockStatic(ThemeDetector.class);
        themeDetectorStatic.when(ThemeDetector::isDarkTheme).thenReturn(false);

        resourceLoaderStatic = mockStatic(ResourceLoader.class);
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/html/kanban.html"))
                .thenReturn("<html><head>${styles}</head><body>${scripts}</body></html>");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/css/theme-variables.css"))
                .thenReturn(":root { --bg: #fff; }");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/css/kanban.css"))
                .thenReturn(".kanban { display: flex; }");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/css/dark-theme.css"))
                .thenReturn(":root { --bg: #000; }");
        resourceLoaderStatic.when(() -> ResourceLoader.loadResource("webview/js/kanban.js"))
                .thenReturn("function initKanban() {}");
    }

    @AfterEach
    void tearDown() {
        themeDetectorStatic.close();
        resourceLoaderStatic.close();
    }

    @Test
    void generate_producesValidHtml() {
        KanbanTemplate template = new KanbanTemplate(webServer);

        String result = template.generate();

        assertThat(result).contains("<html>");
        assertThat(result).contains("</html>");
    }

    @Test
    void generate_containsStyles() {
        KanbanTemplate template = new KanbanTemplate(webServer);

        String result = template.generate();

        assertThat(result).contains("<style>");
        assertThat(result).contains("</style>");
        assertThat(result).contains("--bg: #fff;");
        assertThat(result).contains(".kanban { display: flex; }");
    }

    @Test
    void generate_containsScripts() {
        KanbanTemplate template = new KanbanTemplate(webServer);

        String result = template.generate();

        assertThat(result).contains("<script>");
        assertThat(result).contains("</script>");
        assertThat(result).contains("function initKanban() {}");
    }

    @Test
    void generate_includesDarkThemeWhenDarkMode() {
        themeDetectorStatic.when(ThemeDetector::isDarkTheme).thenReturn(true);

        KanbanTemplate template = new KanbanTemplate(webServer);

        String result = template.generate();

        assertThat(result).contains("--bg: #000;");
    }

    @Test
    void generate_excludesDarkThemeWhenLightMode() {
        themeDetectorStatic.when(ThemeDetector::isDarkTheme).thenReturn(false);

        KanbanTemplate template = new KanbanTemplate(webServer);

        String result = template.generate();

        assertThat(result).doesNotContain("--bg: #000;");
    }

    @Test
    void generate_replacesAllPlaceholders() {
        KanbanTemplate template = new KanbanTemplate(webServer);

        String result = template.generate();

        assertThat(result).doesNotContain("${styles}");
        assertThat(result).doesNotContain("${scripts}");
    }

    @Test
    void generate_containsThemeVariablesCSS() {
        KanbanTemplate template = new KanbanTemplate(webServer);

        String result = template.generate();

        assertThat(result).contains(":root { --bg: #fff; }");
    }
}
