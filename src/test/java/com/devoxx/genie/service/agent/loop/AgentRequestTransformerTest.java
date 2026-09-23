package com.devoxx.genie.service.agent.loop;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRequestTransformerTest {

    private static final String BIG = "z".repeat(10_000);

    private static ToolSpecification spec(String name) {
        return ToolSpecification.builder().name(name).description("tool " + name).build();
    }

    private static List<ChatMessage> messagesWithToolResults(int count) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(UserMessage.from("q"));
        for (int i = 0; i < count; i++) {
            messages.add(AiMessage.from("step " + i));
            messages.add(ToolExecutionResultMessage.from("id" + i, "read_file", BIG));
        }
        return messages;
    }

    private static ChatRequest request(List<ChatMessage> messages, List<ToolSpecification> specs) {
        return ChatRequest.builder()
                .messages(messages)
                .parameters(ChatRequestParameters.builder().toolSpecifications(specs).temperature(0.3).build())
                .build();
    }

    @Test
    void compactsOlderToolResults_andRecordsMetrics() {
        AgentRunContext context = new AgentRunContext(false, true, false, 0);
        ChatRequest original = request(messagesWithToolResults(6), List.of(spec("read_file")));

        ChatRequest transformed = context.requestTransformer().apply(original, "mem");

        long originalChars = AgentRequestTransformer.estimateChars(original);
        long transformedChars = AgentRequestTransformer.estimateChars(transformed);
        assertThat(transformedChars).isLessThan(originalChars);
        assertThat(context.getMetrics().getCompactedResults()).isEqualTo(2);
        assertThat(context.getMetrics().getRoundTrips()).isEqualTo(1);
        assertThat(context.getMetrics().getRequestChars()).isEqualTo(transformedChars);
        // Parameters survive the rebuild.
        assertThat(transformed.temperature()).isEqualTo(0.3);
        assertThat(transformed.toolSpecifications()).hasSize(1);
    }

    @Test
    void hidesLockedDeferredToolSpecs() {
        AgentRunContext context = new AgentRunContext(false, false, true, 0);
        context.getDeferredTools().defer(List.of(spec("mcp_a"), spec("mcp_b")));
        context.getDeferredTools().unlock("mcp_b");

        ChatRequest transformed = context.requestTransformer().apply(
                request(messagesWithToolResults(1), List.of(spec("read_file"), spec("mcp_a"), spec("mcp_b"))), "mem");

        assertThat(transformed.toolSpecifications()).extracting(ToolSpecification::name)
                .containsExactly("read_file", "mcp_b");
        assertThat(transformed.temperature()).isEqualTo(0.3);
        assertThat(context.getMetrics().getHiddenToolSpecs()).isEqualTo(1);
    }

    @Test
    void leavesProviderSpecificParametersUntouched() {
        AgentRunContext context = new AgentRunContext(false, false, true, 0);
        context.getDeferredTools().defer(List.of(spec("mcp_a")));
        ChatRequestParameters custom = new CustomParameters(List.of(spec("read_file"), spec("mcp_a")));
        ChatRequest original = ChatRequest.builder().messages(List.of(UserMessage.from("q"))).parameters(custom).build();

        ChatRequest transformed = context.requestTransformer().apply(original, "mem");

        assertThat(transformed.parameters()).isSameAs(custom);
    }

    @Test
    void returnsSameRequest_whenNothingToDo() {
        AgentRunContext context = AgentRunContext.disabled();
        ChatRequest original = request(messagesWithToolResults(10), List.of(spec("read_file")));

        assertThat(context.requestTransformer().apply(original, "mem")).isSameAs(original);
        assertThat(context.getMetrics().getRoundTrips()).isEqualTo(1);
    }

    @Test
    void chain_appliesInOrderAndSkipsNulls() {
        List<String> order = new ArrayList<>();
        BiFunction<ChatRequest, Object, ChatRequest> first = (r, m) -> { order.add("first"); return r; };
        BiFunction<ChatRequest, Object, ChatRequest> second = (r, m) -> { order.add("second"); return r; };

        AgentRequestTransformer.chain(first, null, second).apply(request(List.of(UserMessage.from("q")), List.of()), "m");

        assertThat(order).containsExactly("first", "second");
    }

    /** Stand-in for a provider-specific parameters subtype. */
    private static final class CustomParameters extends DefaultChatRequestParameters {
        CustomParameters(List<ToolSpecification> specs) {
            super(DefaultChatRequestParameters.builder().toolSpecifications(specs));
        }
    }
}
