package com.devoxx.genie.service.trace;

import com.devoxx.genie.service.debug.SecretRedactor;
import com.devoxx.genie.service.debug.SecretRedactor.TraceRedactionOptions;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Prepares a buffered turn for storage: passes every free-text field (content, reasoning,
 * tool-call arguments) through {@link SecretRedactor#redactForTrace}, then truncates oversized
 * tool results. Runs before anything reaches {@code conversations.db}.
 */
public final class TraceRedaction {

    private TraceRedaction() {}

    /** Options for this machine: home directory and OS username (when enabled) plus custom patterns. */
    public static @NotNull TraceRedactionOptions optionsFor(@NotNull TraceSettings settings) {
        String home = settings.redactHomeDirectory() ? System.getProperty("user.home") : null;
        String user = settings.redactHomeDirectory() ? System.getProperty("user.name") : null;
        return TraceRedactionOptions.of(home, user, settings.customPatterns());
    }

    public static @NotNull List<TraceEvent> apply(@NotNull List<TraceEvent> events, @NotNull TraceSettings settings,
                                                  @NotNull TraceRedactionOptions options) {
        return events.stream()
                // Redact before truncating: cutting first could leave half a secret that no
                // longer matches its pattern.
                .map(e -> e.mapText(text -> SecretRedactor.redactForTrace(text, options).text()))
                .map(e -> truncate(e, settings.maxToolResultChars()))
                .toList();
    }

    static @NotNull TraceEvent truncate(@NotNull TraceEvent event, int maxChars) {
        if (event.role() != TraceEvent.Role.TOOL || maxChars <= 0) {
            return event;
        }
        String content = event.content();
        if (content == null || content.length() <= maxChars) {
            return event;
        }
        return new TraceEvent(event.role(), truncateText(content, maxChars), event.reasoning(), event.toolCalls(),
                event.toolCallId(), event.toolName(), event.error(), event.model(), event.subAgentId(),
                event.timestampMs());
    }

    static @NotNull String truncateText(@NotNull String text, int maxChars) {
        return text.substring(0, maxChars) + "\n…[truncated " + (text.length() - maxChars) + " characters]";
    }
}
