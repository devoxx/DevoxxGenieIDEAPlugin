package com.devoxx.genie.model.automation;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventContextTest {

    @Test
    void toPromptBlock_includesAllFields() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("errorCount", "3");
        metadata.put("warningCount", "1");

        EventContext ctx = EventContext.builder()
                .eventType(IdeEventType.BUILD_FAILED)
                .content("ERROR: cannot find symbol\nERROR: incompatible types")
                .filePaths(List.of("src/Main.java", "src/Utils.java"))
                .metadata(metadata)
                .build();

        String block = ctx.toPromptBlock();

        assertThat(block).contains("Event: Build Failed");
        assertThat(block).contains("Time:");
        assertThat(block).contains("src/Main.java");
        assertThat(block).contains("src/Utils.java");
        assertThat(block).contains("errorCount: 3");
        assertThat(block).contains("warningCount: 1");
        assertThat(block).contains("ERROR: cannot find symbol");
        assertThat(block).startsWith("--- Event Context ---");
        assertThat(block).endsWith("--- End Context ---");
    }

    @Test
    void toPromptBlock_emptyOptionalFields() {
        EventContext ctx = EventContext.builder()
                .eventType(IdeEventType.FILE_OPENED)
                .build();

        String block = ctx.toPromptBlock();

        assertThat(block).contains("Event: File Opened");
        assertThat(block).doesNotContain("Files:");
        assertThat(block).doesNotContain("Details:");
    }

    @Test
    void builder_defaults() {
        EventContext ctx = EventContext.builder()
                .eventType(IdeEventType.FILE_SAVED)
                .build();

        assertThat(ctx.getContent()).isEmpty();
        assertThat(ctx.getFilePaths()).isEmpty();
        assertThat(ctx.getMetadata()).isEmpty();
        assertThat(ctx.getTimestamp()).isNotEmpty();
    }
}
