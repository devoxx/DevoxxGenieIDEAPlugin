package com.devoxx.genie.service.prompt.response.streaming;

import dev.langchain4j.data.message.AiMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Encodes provider thinking separately from the final answer while keeping the value
 * transportable through the existing AiMessage.text() persistence path.
 */
public final class ThinkingResponseFormatter {

    public static final String THINKING_START = "<!-- devoxx-genie-thinking-start -->";
    public static final String THINKING_END = "<!-- devoxx-genie-thinking-end -->";

    private ThinkingResponseFormatter() {
    }

    public static @NotNull String format(@NotNull String thinking, @NotNull String answer) {
        if (thinking.isBlank()) {
            return answer;
        }
        return THINKING_START + "\n"
                + thinking.strip() + "\n"
                + THINKING_END
                + (answer.isBlank() ? "" : "\n\n" + answer);
    }

    public static @NotNull AiMessage format(@NotNull AiMessage aiMessage) {
        String thinking = aiMessage.thinking() == null ? "" : aiMessage.thinking();
        if (thinking.isBlank()) {
            // Nothing to embed — keep the original message (and its attributes) intact.
            return aiMessage;
        }
        String answer = aiMessage.text() == null ? "" : aiMessage.text();
        return AiMessage.from(format(thinking, answer));
    }

    public static @NotNull String extractThinking(@NotNull String content) {
        int start = content.indexOf(THINKING_START);
        int end = content.indexOf(THINKING_END);
        if (start < 0 || end < 0 || end <= start) {
            return "";
        }
        int thinkingStart = start + THINKING_START.length();
        return content.substring(thinkingStart, end).strip();
    }

    public static @NotNull String extractAnswer(@NotNull String content) {
        int start = content.indexOf(THINKING_START);
        int end = content.indexOf(THINKING_END);
        if (start < 0 || end < 0 || end <= start) {
            return content;
        }
        String before = content.substring(0, start).strip();
        String after = content.substring(end + THINKING_END.length()).strip();
        if (before.isEmpty()) {
            return after;
        }
        if (after.isEmpty()) {
            return before;
        }
        return before + "\n\n" + after;
    }
}
