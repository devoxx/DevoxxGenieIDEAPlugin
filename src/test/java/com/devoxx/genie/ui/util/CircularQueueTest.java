package com.devoxx.genie.ui.util;

import dev.langchain4j.data.message.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CircularQueueTest {

    @Test
    void testQueueOverflow() {
        CircularQueue<ChatMessage> circularQueue = new CircularQueue<>(3);

        circularQueue.add(SystemMessage.from("Java Developer"));
        circularQueue.add(UserMessage.from("Hey"));
        circularQueue.add(UserMessage.from("Write me a hello world in Java"));

        assertThat(circularQueue.size()).isEqualTo(3);

        circularQueue.add(UserMessage.from("And unit test"));
        assertThat(circularQueue.size()).isEqualTo(3);

        circularQueue.add(UserMessage.from("What was the first message"));
        assertThat(circularQueue.size()).isEqualTo(3);

        circularQueue.asList().stream().findFirst().ifPresent(message -> {
            assertThat(message).isInstanceOf(SystemMessage.class);
            assertThat(message).extracting(ChatMessage::type).isEqualTo(ChatMessageType.SYSTEM);
        });

    }
}
