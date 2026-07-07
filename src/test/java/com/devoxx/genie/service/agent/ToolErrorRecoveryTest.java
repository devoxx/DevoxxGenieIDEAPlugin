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
 * Reproduces the root cause of issue #1193: when the model requests a tool that is not
 * registered (disabled in settings, or a hallucinated name — common with models like
 * DeepSeek whose system prompt instructs tool usage), Langchain4j's default
 * {@code HallucinatedToolNameStrategy.THROW_EXCEPTION} kills the tool loop <em>after</em>
 * the AiMessage(tool_calls) was already written to chat memory but <em>before</em> any
 * tool result is written. The dangling tool_calls tail then breaks every subsequent
 * request with "An assistant message with 'tool_calls' must be followed by tool messages
 * responding to each 'tool_call_id'".
 *
 * <p>The fix: {@link ToolErrorRecovery#configure(AiServices)} replaces the throwing
 * default with a strategy that returns an error tool-result, keeping memory consistent
 * and giving the model a chance to self-correct.
 */
class ToolErrorRecoveryTest {

    interface Assistant {
        String chat(String userMessage);
    }

    /**
     * Fake model that first requests a tool that does not exist, then — if it receives
     * an error tool-result back — recovers by calling the real tool, then answers.
     */
    private static final class HallucinatingChatModel implements ChatModel {
        private int requests = 0;

        @Override
        public ChatResponse doChat(ChatRequest request) {
            requests++;
            List<ChatMessage> messages = request.messages();
            ChatMessage last = messages.get(messages.size() - 1);

            if (requests == 1) {
                // Hallucinated tool name on the first round trip
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                                .id("call_1")
                                .name("no_such_tool")
                                .arguments("{}")
                                .build()))
                        .build();
            }

            if (last instanceof ToolExecutionResultMessage toolResult
                    && toolResult.text().contains("no_such_tool")) {
                // Model self-corrects after being told the tool doesn't exist
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                                .id("call_2")
                                .name("read_file")
                                .arguments("{}")
                                .build()))
                        .build();
            }

            return ChatResponse.builder()
                    .aiMessage(AiMessage.from("recovered and done"))
                    .build();
        }
    }

    private static ToolProvider realToolProvider() {
        ToolExecutor executor = (req, id) -> "file content";
        ToolSpecification spec = ToolSpecification.builder()
                .name("read_file")
                .description("Reads a file")
                .parameters(JsonObjectSchema.builder().build())
                .build();
        return req -> ToolProviderResult.builder().add(spec, executor).build();
    }

    /**
     * Characterization of the issue #1193 root cause: without the recovery strategy,
     * a hallucinated tool name throws AND leaves a dangling AiMessage(tool_calls) as
     * the tail of chat memory — corrupting the conversation for all later requests.
     * This documents why {@link ToolErrorRecovery} must not be removed.
     */
    @Test
    void withoutRecovery_hallucinatedToolThrowsAndLeavesDanglingToolCallsInMemory() {
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(100);

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(new HallucinatingChatModel())
                .chatMemoryProvider(memoryId -> memory)
                .toolProvider(realToolProvider())
                .build();

        assertThatThrownBy(() -> assistant.chat("do something"))
                .hasMessageContaining("no_such_tool");

        // The corrupted state that produces the issue #1193 API error on the next request:
        List<ChatMessage> messages = memory.messages();
        ChatMessage tail = messages.get(messages.size() - 1);
        assertThat(tail).isInstanceOf(AiMessage.class);
        assertThat(((AiMessage) tail).hasToolExecutionRequests()).isTrue();
    }

    @Test
    void withRecovery_hallucinatedToolGetsErrorResultAndModelSelfCorrects() {
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(100);

        Assistant assistant = ToolErrorRecovery.configure(AiServices.builder(Assistant.class)
                        .chatModel(new HallucinatingChatModel())
                        .chatMemoryProvider(memoryId -> memory)
                        .toolProvider(realToolProvider()))
                .build();

        String answer = assistant.chat("do something");

        assertThat(answer).isEqualTo("recovered and done");
    }

    @Test
    void withRecovery_everyToolCallInMemoryHasItsResult() {
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(100);

        Assistant assistant = ToolErrorRecovery.configure(AiServices.builder(Assistant.class)
                        .chatModel(new HallucinatingChatModel())
                        .chatMemoryProvider(memoryId -> memory)
                        .toolProvider(realToolProvider()))
                .build();

        assistant.chat("do something");

        // Memory must stay consistent: every tool_call id is answered by a result
        List<ChatMessage> messages = memory.messages();
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) instanceof AiMessage aiMessage && aiMessage.hasToolExecutionRequests()) {
                for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                    boolean answered = messages.stream()
                            .filter(m -> m instanceof ToolExecutionResultMessage)
                            .map(m -> (ToolExecutionResultMessage) m)
                            .anyMatch(r -> r.id().equals(request.id()));
                    assertThat(answered)
                            .as("tool_call %s must have a matching tool result", request.id())
                            .isTrue();
                }
            }
        }
    }
}
