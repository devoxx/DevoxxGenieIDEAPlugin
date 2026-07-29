package com.devoxx.genie.service.prompt.steering;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #1241: the injector runs as an AiServices chatRequestTransformer on every
 * agent-loop round trip. It appends pending steering messages at the tail of the
 * request (always a valid position — after completed tool results) and commits
 * them to chat memory so they persist for later turns.
 */
class SteeringMessageInjectorTest {

    private static final String MEMORY_KEY = "project-tab1";

    private final SteeringMessageQueue queue = new SteeringMessageQueue();
    private final MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(100);
    private final SteeringMessageInjector injector = new SteeringMessageInjector(queue, MEMORY_KEY, memory);

    private static ChatRequest requestWithToolResultTail() {
        return ChatRequest.builder()
                .messages(List.of(
                        UserMessage.from("implement the endpoints"),
                        AiMessage.from("calling tool"),
                        ToolExecutionResultMessage.from("id1", "read_file", "file content")))
                .build();
    }

    @Test
    void noPendingMessages_returnsRequestUnchanged() {
        ChatRequest request = requestWithToolResultTail();

        ChatRequest result = injector.apply(request, MEMORY_KEY);

        assertThat(result.messages()).isEqualTo(request.messages());
        assertThat(memory.messages()).isEmpty();
    }

    @Test
    void pendingMessage_isAppendedAtRequestTail() {
        queue.offer(MEMORY_KEY, "use snake_case for the API json");

        ChatRequest result = injector.apply(requestWithToolResultTail(), MEMORY_KEY);

        assertThat(result.messages()).hasSize(4);
        ChatMessage last = result.messages().get(result.messages().size() - 1);
        assertThat(last).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) last).singleText()).isEqualTo("use snake_case for the API json");
    }

    @Test
    void pendingMessage_isCommittedToChatMemory() {
        queue.offer(MEMORY_KEY, "use snake_case for the API json");

        injector.apply(requestWithToolResultTail(), MEMORY_KEY);

        assertThat(memory.messages()).hasSize(1);
        assertThat(memory.messages().get(0)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) memory.messages().get(0)).singleText())
                .isEqualTo("use snake_case for the API json");
    }

    @Test
    void pendingMessage_drainsTheQueue() {
        queue.offer(MEMORY_KEY, "correction");

        injector.apply(requestWithToolResultTail(), MEMORY_KEY);

        assertThat(queue.hasPending(MEMORY_KEY)).isFalse();
    }

    @Test
    void multiplePendingMessages_areJoinedIntoOneUserMessageInOrder() {
        queue.offer(MEMORY_KEY, "first correction");
        queue.offer(MEMORY_KEY, "second correction");

        ChatRequest result = injector.apply(requestWithToolResultTail(), MEMORY_KEY);

        // Joined into a single UserMessage: consecutive same-role messages are
        // rejected or merged inconsistently across providers.
        assertThat(result.messages()).hasSize(4);
        UserMessage last = (UserMessage) result.messages().get(result.messages().size() - 1);
        assertThat(last.singleText()).contains("first correction").contains("second correction");
        assertThat(last.singleText().indexOf("first correction"))
                .isLessThan(last.singleText().indexOf("second correction"));
    }

    @Test
    void requestParameters_arePreserved() {
        queue.offer(MEMORY_KEY, "correction");
        ChatRequestParameters parameters = ChatRequestParameters.builder()
                .modelName("some-model")
                .temperature(0.3)
                .build();
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from("prompt")))
                .parameters(parameters)
                .build();

        ChatRequest result = injector.apply(request, MEMORY_KEY);

        assertThat(result.parameters().modelName()).isEqualTo("some-model");
        assertThat(result.parameters().temperature()).isEqualTo(0.3);
    }

    @Test
    void messagesForOtherMemoryKeys_areNotInjected() {
        queue.offer("other-key", "not for this run");

        ChatRequest request = requestWithToolResultTail();
        ChatRequest result = injector.apply(request, MEMORY_KEY);

        assertThat(result.messages()).isEqualTo(request.messages());
        assertThat(queue.hasPending("other-key")).isTrue();
    }
}
