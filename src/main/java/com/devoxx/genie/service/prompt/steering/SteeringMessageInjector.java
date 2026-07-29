package com.devoxx.genie.service.prompt.steering;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * AiServices chatRequestTransformer that injects pending steering messages
 * into the agent loop (issue #1241). Langchain4j applies the transformer to
 * every round-trip request, whose message list always ends with completed
 * tool results — so appending at the tail can never create a dangling
 * tool_use. The steering message is also committed to chat memory so it
 * survives into later round trips (which re-read memory) and later turns,
 * without being re-injected.
 */
@Slf4j
public class SteeringMessageInjector implements BiFunction<ChatRequest, Object, ChatRequest> {

    /**
     * Framing prepended to injected steering text. Without it, models see the new
     * message before they have produced their answer to the current request and
     * simply pivot — the original question never gets answered.
     */
    public static final String STEERING_CONTEXT_NOTE =
            "[The user sent the message below while you were still working on their previous request. "
            + "Do not drop that request: complete it (including its final answer), and also address this "
            + "new input — apply it as a course correction if it changes the current work, or answer it "
            + "additionally if it is a separate question.]";

    private final SteeringMessageQueue queue;
    private final String memoryKey;
    private final ChatMemory chatMemory;

    public SteeringMessageInjector(SteeringMessageQueue queue, String memoryKey, ChatMemory chatMemory) {
        this.queue = queue;
        this.memoryKey = memoryKey;
        this.chatMemory = chatMemory;
    }

    @Override
    public ChatRequest apply(ChatRequest request, Object memoryId) {
        List<String> pending = queue.drain(memoryKey);
        if (pending.isEmpty()) {
            return request;
        }

        // Join into a single UserMessage: consecutive same-role messages are
        // rejected or merged inconsistently across providers.
        UserMessage steeringMessage = UserMessage.from(
                STEERING_CONTEXT_NOTE + "\n\n" + String.join("\n\n", pending));
        log.info("Injecting {} steering message(s) into running agent loop for key {}", pending.size(), memoryKey);

        List<ChatMessage> messages = new ArrayList<>(request.messages());
        messages.add(steeringMessage);
        // The current request's messages were already read from memory, so add
        // the steering message to memory for subsequent round trips and turns.
        chatMemory.add(steeringMessage);

        return ChatRequest.builder()
                .messages(messages)
                .parameters(request.parameters())
                .build();
    }
}
