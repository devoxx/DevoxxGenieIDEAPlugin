package com.devoxx.genie.service.automation;

import com.devoxx.genie.model.automation.EventContext;
import com.devoxx.genie.model.automation.IdeEventType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplateRendererTest {

    private EventContext buildContext() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("errorCount", "2");
        metadata.put("severity", "high");

        return EventContext.builder()
                .eventType(IdeEventType.BUILD_FAILED)
                .timestamp("2026-03-06T10:00:00Z")
                .content("ERROR: cannot find symbol\nERROR: method not found")
                .filePaths(List.of("src/Main.java", "src/Utils.java"))
                .metadata(metadata)
                .build();
    }

    @Test
    void render_contextVariable() {
        EventContext ctx = buildContext();
        String result = PromptTemplateRenderer.render("Fix these errors:\n\n{{context}}", ctx);

        assertThat(result).startsWith("Fix these errors:");
        assertThat(result).contains("--- Event Context ---");
        assertThat(result).contains("Build Failed");
        assertThat(result).contains("src/Main.java");
    }

    @Test
    void render_contentVariable() {
        EventContext ctx = buildContext();
        String result = PromptTemplateRenderer.render("Errors:\n{{content}}", ctx);

        assertThat(result).isEqualTo("Errors:\nERROR: cannot find symbol\nERROR: method not found");
    }

    @Test
    void render_filesVariable() {
        EventContext ctx = buildContext();
        String result = PromptTemplateRenderer.render("Changed files:\n{{files}}", ctx);

        assertThat(result).isEqualTo("Changed files:\nsrc/Main.java\nsrc/Utils.java");
    }

    @Test
    void render_eventVariable() {
        EventContext ctx = buildContext();
        String result = PromptTemplateRenderer.render("Event: {{event}}", ctx);

        assertThat(result).isEqualTo("Event: Build Failed");
    }

    @Test
    void render_timestampVariable() {
        EventContext ctx = buildContext();
        String result = PromptTemplateRenderer.render("At: {{timestamp}}", ctx);

        assertThat(result).isEqualTo("At: 2026-03-06T10:00:00Z");
    }

    @Test
    void render_metaVariable() {
        EventContext ctx = buildContext();
        String result = PromptTemplateRenderer.render("Count: {{meta.errorCount}}, Sev: {{meta.severity}}", ctx);

        assertThat(result).isEqualTo("Count: 2, Sev: high");
    }

    @Test
    void render_missingMetaKey_returnsEmpty() {
        EventContext ctx = buildContext();
        String result = PromptTemplateRenderer.render("Missing: {{meta.nonexistent}}", ctx);

        assertThat(result).isEqualTo("Missing: ");
    }

    @Test
    void render_unknownVariable_leftAsIs() {
        EventContext ctx = buildContext();
        String result = PromptTemplateRenderer.render("Unknown: {{foobar}}", ctx);

        assertThat(result).isEqualTo("Unknown: {{foobar}}");
    }

    @Test
    void render_noVariables_appendsContextBlock() {
        EventContext ctx = buildContext();
        String result = PromptTemplateRenderer.render("Fix all build errors.", ctx);

        assertThat(result).startsWith("Fix all build errors.\n\n--- Event Context ---");
        assertThat(result).contains("ERROR: cannot find symbol");
    }

    @Test
    void render_multipleVariables() {
        EventContext ctx = buildContext();
        String result = PromptTemplateRenderer.render(
                "{{event}} at {{timestamp}}: {{meta.errorCount}} errors in {{files}}", ctx);

        assertThat(result).isEqualTo(
                "Build Failed at 2026-03-06T10:00:00Z: 2 errors in src/Main.java\nsrc/Utils.java");
    }

    @Test
    void render_specialRegexCharsInContent() {
        EventContext ctx = EventContext.builder()
                .eventType(IdeEventType.BUILD_FAILED)
                .content("Error at $1 with \\n and (group)")
                .build();

        String result = PromptTemplateRenderer.render("Output: {{content}}", ctx);

        assertThat(result).isEqualTo("Output: Error at $1 with \\n and (group)");
    }
}
