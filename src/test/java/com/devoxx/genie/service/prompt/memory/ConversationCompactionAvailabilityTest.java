package com.devoxx.genie.service.prompt.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationCompactionAvailabilityTest {

    private static final SystemMessage SYSTEM = SystemMessage.from("system");
    private static final UserMessage SUMMARY =
            UserMessage.from(ConversationCompactionService.SUMMARY_PREFIX + "earlier work");

    @Test
    void emptyHistoryHasNothingToCompact() {
        assertThat(ConversationCompactionService.nothingToCompactReason(List.of())).isNotNull();
        assertThat(ConversationCompactionService.nothingToCompactReason(List.of(SYSTEM))).isNotNull();
    }

    @Test
    void singleExchangeHasNothingToCompact() {
        List<ChatMessage> history = List.of(SYSTEM, UserMessage.from("q1"), AiMessage.from("a1"));

        assertThat(ConversationCompactionService.nothingToCompactReason(history)).isNotNull();
    }

    @Test
    void twoExchangesCanBeCompacted() {
        List<ChatMessage> history = List.of(SYSTEM,
                UserMessage.from("q1"), AiMessage.from("a1"),
                UserMessage.from("q2"), AiMessage.from("a2"));

        assertThat(ConversationCompactionService.nothingToCompactReason(history)).isNull();
    }

    @Test
    void summaryDoesNotCountAsAnExchange() {
        List<ChatMessage> justCompacted = List.of(SYSTEM, SUMMARY, UserMessage.from("q2"), AiMessage.from("a2"));

        assertThat(ConversationCompactionService.nothingToCompactReason(justCompacted))
                .contains("since the last summary");
    }

    @Test
    void newExchangeAfterSummaryCanBeCompactedAgain() {
        List<ChatMessage> history = List.of(SYSTEM, SUMMARY,
                UserMessage.from("q2"), AiMessage.from("a2"),
                UserMessage.from("q3"), AiMessage.from("a3"));

        assertThat(ConversationCompactionService.nothingToCompactReason(history)).isNull();
    }
}
