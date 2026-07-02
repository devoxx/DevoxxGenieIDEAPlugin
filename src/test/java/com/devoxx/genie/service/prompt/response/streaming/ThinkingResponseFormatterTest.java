package com.devoxx.genie.service.prompt.response.streaming;

import dev.langchain4j.data.message.AiMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThinkingResponseFormatterTest {

    @Test
    void format_withThinkingAndAnswer_roundTripsThroughExtractors() {
        String content = ThinkingResponseFormatter.format("I should reason first.", "The answer is 42.");

        assertThat(ThinkingResponseFormatter.extractThinking(content)).isEqualTo("I should reason first.");
        assertThat(ThinkingResponseFormatter.extractAnswer(content)).isEqualTo("The answer is 42.");
    }

    @Test
    void format_withBlankThinking_returnsAnswerUnchanged() {
        assertThat(ThinkingResponseFormatter.format("  ", "The answer is 42."))
                .isEqualTo("The answer is 42.");
    }

    @Test
    void format_aiMessageWithThinking_embedsMarkers() {
        AiMessage aiMessage = AiMessage.builder()
                .thinking("I should reason first.")
                .text("The answer is 42.")
                .build();

        AiMessage formatted = ThinkingResponseFormatter.format(aiMessage);

        assertThat(ThinkingResponseFormatter.extractThinking(formatted.text())).isEqualTo("I should reason first.");
        assertThat(ThinkingResponseFormatter.extractAnswer(formatted.text())).isEqualTo("The answer is 42.");
    }

    @Test
    void format_aiMessageWithoutThinking_returnsSameMessage() {
        AiMessage aiMessage = AiMessage.from("The answer is 42.");

        assertThat(ThinkingResponseFormatter.format(aiMessage)).isSameAs(aiMessage);
    }

    @Test
    void format_aiMessageWithThinkingButNullText_doesNotThrow() {
        AiMessage aiMessage = AiMessage.builder()
                .thinking("Only reasoning, no answer yet.")
                .build();

        AiMessage formatted = ThinkingResponseFormatter.format(aiMessage);

        assertThat(ThinkingResponseFormatter.extractThinking(formatted.text()))
                .isEqualTo("Only reasoning, no answer yet.");
        assertThat(ThinkingResponseFormatter.extractAnswer(formatted.text())).isEmpty();
    }

    @Test
    void extractors_withPlainContent_returnAnswerOnly() {
        assertThat(ThinkingResponseFormatter.extractThinking("plain answer")).isEmpty();
        assertThat(ThinkingResponseFormatter.extractAnswer("plain answer")).isEqualTo("plain answer");
    }
}
