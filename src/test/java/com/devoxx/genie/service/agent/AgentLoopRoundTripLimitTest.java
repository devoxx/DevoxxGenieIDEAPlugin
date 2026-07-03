package com.devoxx.genie.service.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reproduces issue #1188: the user-configurable agent tool-call limit (up to 500)
 * was silently capped at 100 because Langchain4j's {@code ToolService} defaults
 * {@code maxToolCallingRoundTrips} to 100 and kills the run with
 * "Something is wrong, exceeded 100 tool calling round trips" before the plugin's
 * own {@link AgentLoopTracker} limit is ever reached.
 *
 * <p>The fix threads the tracker's limit (plus a wrap-up grace margin) into every
 * tool-using {@code AiServices} builder via {@link AgentLoopTracker#getMaxToolCallingRoundTrips()}.
 */
class AgentLoopRoundTripLimitTest {

    private static final int TRACKER_LIMIT_ABOVE_LC4J_DEFAULT = 120;

    interface Assistant {
        String chat(String userMessage);
    }

    /**
     * Fake model that keeps requesting the {@code read_file} tool until either it has
     * issued the requested number of tool calls or the tracker tells it to wrap up,
     * then returns a final text answer.
     */
    private static final class ToolLoopingChatModel implements ChatModel {
        private final int toolCallsToAttempt;
        private int issued = 0;

        private ToolLoopingChatModel(int toolCallsToAttempt) {
            this.toolCallsToAttempt = toolCallsToAttempt;
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            List<ChatMessage> messages = request.messages();
            ChatMessage last = messages.get(messages.size() - 1);
            boolean askedToWrapUp = last instanceof ToolExecutionResultMessage toolResult
                    && toolResult.text().contains("Agent loop limit reached");
            if (askedToWrapUp || issued >= toolCallsToAttempt) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from("done after " + issued + " tool calls"))
                        .build();
            }
            issued++;
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                            .id(String.valueOf(issued))
                            .name("read_file")
                            .arguments("{}")
                            .build()))
                    .build();
        }
    }

    private static ToolProvider mockToolProvider() {
        ToolExecutor executor = (req, id) -> "file content";
        ToolSpecification spec = ToolSpecification.builder()
                .name("read_file")
                .description("Reads a file")
                .parameters(JsonObjectSchema.builder().build())
                .build();
        return req -> ToolProviderResult.builder().add(spec, executor).build();
    }

    @Test
    void trackerLimitAbove100_completesWhenRoundTripLimitFollowsTrackerLimit() {
        AgentLoopTracker tracker = new AgentLoopTracker(mockToolProvider(), TRACKER_LIMIT_ABOVE_LC4J_DEFAULT);

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(new ToolLoopingChatModel(TRACKER_LIMIT_ABOVE_LC4J_DEFAULT))
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(1000))
                .toolProvider(tracker)
                .maxToolCallingRoundTrips(tracker.getMaxToolCallingRoundTrips())
                .build();

        String answer = assistant.chat("do lots of tool calls");

        assertThat(answer).isEqualTo("done after " + TRACKER_LIMIT_ABOVE_LC4J_DEFAULT + " tool calls");
        assertThat(tracker.getCallCount()).isEqualTo(TRACKER_LIMIT_ABOVE_LC4J_DEFAULT);
    }

    @Test
    void modelThatOverrunsTrackerLimit_isWrappedUpGracefullyWithinGraceMargin() {
        AgentLoopTracker tracker = new AgentLoopTracker(mockToolProvider(), TRACKER_LIMIT_ABOVE_LC4J_DEFAULT);

        // Model tries more calls than the tracker allows: calls beyond the limit get the
        // "Agent loop limit reached" error string and the model then wraps up. The extra
        // round trip for the wrap-up must fit within the grace margin.
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(new ToolLoopingChatModel(TRACKER_LIMIT_ABOVE_LC4J_DEFAULT + 50))
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(1000))
                .toolProvider(tracker)
                .maxToolCallingRoundTrips(tracker.getMaxToolCallingRoundTrips())
                .build();

        String answer = assistant.chat("do lots of tool calls");

        assertThat(answer).startsWith("done after");
    }

    /**
     * Characterization of the issue #1188 root cause: without the explicit override,
     * Langchain4j's default of 100 round trips kills the run with an execution error
     * even though the plugin's own limit (e.g. 500) has not been reached. This test
     * documents why the {@code maxToolCallingRoundTrips} override must not be removed.
     */
    @Test
    void withoutRoundTripOverride_langchain4jKillsLoopAt100() {
        AgentLoopTracker tracker = new AgentLoopTracker(mockToolProvider(), TRACKER_LIMIT_ABOVE_LC4J_DEFAULT);

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(new ToolLoopingChatModel(TRACKER_LIMIT_ABOVE_LC4J_DEFAULT))
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(1000))
                .toolProvider(tracker)
                .build();

        assertThatThrownBy(() -> assistant.chat("do lots of tool calls"))
                .hasMessageContaining("100 tool calling round trips");
    }

    @Test
    void roundTripLimit_exceedsTrackerLimitByGraceMargin() {
        AgentLoopTracker tracker = new AgentLoopTracker(mockToolProvider(), 500);

        assertThat(tracker.getMaxToolCallingRoundTrips()).isGreaterThan(500);
    }
}
