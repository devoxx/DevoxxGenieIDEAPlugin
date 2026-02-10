package com.devoxx.genie.ui.webview.template;

import com.devoxx.genie.ui.util.ThemeDetector;
import com.devoxx.genie.ui.webview.WebServer;
import org.jetbrains.annotations.NotNull;

/**
 * Template for generating the Kanban board HTML.
 */
public class KanbanTemplate extends HtmlTemplate {

    public KanbanTemplate(WebServer webServer) {
        super(webServer);
    }

    @Override
    public @NotNull String generate() {
        String htmlTemplate = ResourceLoader.loadResource("webview/html/kanban.html");
        return htmlTemplate
                .replace("${styles}", generateStyles())
                .replace("${scripts}", generateScripts());
    }

    private @NotNull String generateStyles() {
        String themeVariables = ResourceLoader.loadResource("webview/css/theme-variables.css");
        String kanbanCss = ResourceLoader.loadResource("webview/css/kanban.css");

        StringBuilder sb = new StringBuilder();
        sb.append("<style>\n");
        sb.append(themeVariables).append("\n");

        if (ThemeDetector.isDarkTheme()) {
            String darkTheme = ResourceLoader.loadResource("webview/css/dark-theme.css");
            sb.append(darkTheme).append("\n");
        }

        sb.append(kanbanCss).append("\n");
        sb.append("</style>");
        return sb.toString();
    }

    private @NotNull String generateScripts() {
        String kanbanJs = ResourceLoader.loadResource("webview/js/kanban.js");
        StringBuilder sb = new StringBuilder();
        sb.append("<script>\n");
        sb.append(kanbanJs);
        sb.append("\n</script>\n");
        return sb.toString();
    }
}
