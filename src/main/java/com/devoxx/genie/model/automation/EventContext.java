package com.devoxx.genie.model.automation;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;

/**
 * Runtime context produced by an IDE event and injected into an agent's prompt.
 * Each event listener populates the relevant fields; unused fields remain empty.
 */
@Data
@Builder
public class EventContext {

    /** The event that produced this context. */
    @NotNull
    private final IdeEventType eventType;

    /** ISO-8601 timestamp of when the event fired. */
    @Builder.Default
    private final String timestamp = Instant.now().toString();

    /** Primary text payload: diff, error output, stack trace, file content, etc. */
    @Builder.Default
    private final String content = "";

    /** Affected file paths (changed files, created file, opened file, etc.). */
    @Builder.Default
    private final List<String> filePaths = Collections.emptyList();

    /** Structured key-value metadata specific to the event category. */
    @Builder.Default
    private final Map<String, String> metadata = Collections.emptyMap();

    /**
     * Renders this context into a human-readable block suitable for prompt injection.
     */
    public String toPromptBlock() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Event Context ---\n");
        sb.append("Event: ").append(eventType.getDisplayName()).append('\n');
        sb.append("Time: ").append(timestamp).append('\n');

        if (!filePaths.isEmpty()) {
            sb.append("Files:\n");
            for (String path : filePaths) {
                sb.append("  - ").append(path).append('\n');
            }
        }

        if (!metadata.isEmpty()) {
            sb.append("Details:\n");
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
            }
        }

        if (!content.isEmpty()) {
            sb.append("\n").append(content).append('\n');
        }

        sb.append("--- End Context ---");
        return sb.toString();
    }
}
