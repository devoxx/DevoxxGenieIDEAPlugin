package com.devoxx.genie.service.prompt.steering;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #1241 end-to-end mechanism test against the real langchain4j dependency:
 * a steering message offered WHILE the agent loop is executing a tool must appear
 * in the NEXT round-trip request, positioned after the completed tool results
 * (never creating a dangling tool_use), and must appear exactly once in
 * subsequent round trips (persisted via chat memory, not re-injected).
 */
class MidTaskSteeringRoundTripTest {

    private static final String MEMORY_KEY = "project-tab1";
    private static final String STEERING_TEXT = "use snake_case for the API json";

    interface Assistant {
        String chat(String userMessage);
    }

    interface StreamingAssistant {
        TokenStream chat(String userMessage);
    }

    /**
     * Fake model that requests the {@code do_work} tool a fixed number of times,
     * then answers. Records every request's messages for assertions.
     */
    private static final class RecordingToolLoopingModel implements ChatModel {
        final List<List<ChatMessage>> requests = new CopyOnWriteArrayList<>();
        private final int toolCalls;
        private int issued = 0;

        RecordingToolLoopingModel(int toolCalls) {
            this.toolCalls = toolCalls;
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            requests.add(request.messages());
            if (issued >= toolCalls) {
                return ChatResponse.builder().aiMessage(AiMessage.from("done")).build();
            }
            issued++;
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                            .id("call-" + issued)
                            .name("do_work")
                            .arguments("{}")
                            .build()))
                    .build();
        }
    }

    /** Streaming twin of {@link RecordingToolLoopingModel}. */
    private static final class RecordingStreamingToolLoopingModel implements StreamingChatModel {
        final List<List<ChatMessage>> requests = new CopyOnWriteArrayList<>();
        private final int toolCalls;
        private int issued = 0;

        RecordingStreamingToolLoopingModel(int toolCalls) {
            this.toolCalls = toolCalls;
        }

        @Override
        public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
            requests.add(request.messages());
            if (issued >= toolCalls) {
                handler.onPartialResponse("done");
                handler.onCompleteResponse(ChatResponse.builder()
                        .aiMessage(AiMessage.from("done"))
                        .build());
                return;
            }
            issued++;
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                            .id("call-" + issued)
                            .name("do_work")
                            .arguments("{}")
                            .build()))
                    .build());
        }
    }

    /** Tool provider whose executor simulates the user typing a steering message mid-task. */
    private static ToolProvider steeringOnFirstCallToolProvider(SteeringMessageQueue queue) {
        ToolExecutor executor = new ToolExecutor() {
            private boolean steered = false;

            @Override
            public String execute(ToolExecutionRequest request, Object memoryId) {
                if (!steered) {
                    steered = true;
                    queue.offer(MEMORY_KEY, STEERING_TEXT);
                }
                return "work done";
            }
        };
        ToolSpecification spec = ToolSpecification.builder()
                .name("do_work")
                .description("Does some work")
                .parameters(JsonObjectSchema.builder().build())
                .build();
        return req -> ToolProviderResult.builder().add(spec, executor).build();
    }

    private static void assertSteeringInjectedSafely(List<List<ChatMessage>> requests,
                                                     MessageWindowChatMemory memory) {
        // Round trip 1: no steering yet
        assertThat(countSteeringMessages(requests.get(0))).isZero();

        // Round trip 2: steering message present, at the tail, AFTER the tool result
        List<ChatMessage> second = requests.get(1);
        assertThat(countSteeringMessages(second)).isEqualTo(1);
        assertThat(second.get(second.size() - 1)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) second.get(second.size() - 1)).singleText()).isEqualTo(STEERING_TEXT);
        assertThat(second.get(second.size() - 2)).isInstanceOf(ToolExecutionResultMessage.class);

        // Round trip 3: still exactly once (from memory, not re-injected)
        assertThat(countSteeringMessages(requests.get(2))).isEqualTo(1);

        // Persisted in chat memory for later turns
        assertThat(memory.messages().stream()
                .filter(m -> m instanceof UserMessage u && STEERING_TEXT.equals(u.singleText())))
                .hasSize(1);
    }

    private static long countSteeringMessages(List<ChatMessage> messages) {
        return messages.stream()
                .filter(m -> m instanceof UserMessage u && STEERING_TEXT.equals(u.singleText()))
                .count();
    }

    @Test
    void nonStreaming_steeringMessageArrivesInNextRoundTripExactlyOnce() {
        SteeringMessageQueue queue = new SteeringMessageQueue();
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(100);
        RecordingToolLoopingModel model = new RecordingToolLoopingModel(2);

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> memory)
                .toolProvider(steeringOnFirstCallToolProvider(queue))
                .chatRequestTransformer(new SteeringMessageInjector(queue, MEMORY_KEY, memory))
                .build();

        String answer = assistant.chat("implement the endpoints");

        assertThat(answer).isEqualTo("done");
        assertThat(model.requests).hasSize(3);
        assertSteeringInjectedSafely(model.requests, memory);
    }

    @Test
    void streaming_steeringMessageArrivesInNextRoundTripExactlyOnce() throws Exception {
        SteeringMessageQueue queue = new SteeringMessageQueue();
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(100);
        RecordingStreamingToolLoopingModel model = new RecordingStreamingToolLoopingModel(2);

        StreamingAssistant assistant = AiServices.builder(StreamingAssistant.class)
                .streamingChatModel(model)
                .chatMemoryProvider(memoryId -> memory)
                .toolProvider(steeringOnFirstCallToolProvider(queue))
                .chatRequestTransformer(new SteeringMessageInjector(queue, MEMORY_KEY, memory))
                .build();

        CompletableFuture<ChatResponse> done = new CompletableFuture<>();
        assistant.chat("implement the endpoints")
                .onPartialResponse(token -> { })
                .onCompleteResponse(done::complete)
                .onError(done::completeExceptionally)
                .start();
        ChatResponse response = done.get(10, TimeUnit.SECONDS);

        assertThat(response.aiMessage().text()).isEqualTo("done");
        assertThat(model.requests).hasSize(3);
        assertSteeringInjectedSafely(model.requests, memory);
    }
}
