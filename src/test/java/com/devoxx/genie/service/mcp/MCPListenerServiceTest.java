package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.agent.AgentMessage;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPType;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MCPListenerServiceTest {

    private List<MCPMessage> capturedMcpMessages;
    private List<AgentMessage> capturedAgentMessages;
    private MCPListenerService listener;

    @BeforeEach
    void setUp() {
        capturedMcpMessages = new ArrayList<>();
        capturedAgentMessages = new ArrayList<>();
    }

    // -- Early return cases --

    @Test
    void onRequest_emptyMessagesList_doesNothing() {
        listener = createListener(false, true);

        ChatModelRequestContext ctx = createContextWithMockedMessages(List.of());
        listener.onRequest(ctx);

        assertThat(capturedMcpMessages).isEmpty();
        assertThat(capturedAgentMessages).isEmpty();
    }

    @Test
    void onRequest_singleMessage_doesNothing() {
        listener = createListener(false, true);

        ChatModelRequestContext ctx = createContextWithMockedMessages(List.of(
                UserMessage.from("hello")
        ));
        listener.onRequest(ctx);

        assertThat(capturedMcpMessages).isEmpty();
        assertThat(capturedAgentMessages).isEmpty();
    }

    @Test
    void onRequest_twoMessages_doesNothing() {
        listener = createListener(false, true);

        ChatModelRequestContext ctx = createContextWithMockedMessages(List.of(
                SystemMessage.from("system"),
                UserMessage.from("hello")
        ));
        listener.onRequest(ctx);

        assertThat(capturedMcpMessages).isEmpty();
        assertThat(capturedAgentMessages).isEmpty();
    }

    // -- ToolExecutionResultMessage at penultimate position --

    @Test
    void onRequest_toolExecutionResultMessage_justLogs() {
        listener = createListener(false, true);

        ToolExecutionResultMessage toolResult = ToolExecutionResultMessage.from("id-1", "read_file", "file contents");
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                toolResult,
                UserMessage.from("continue")
        ));
        listener.onRequest(ctx);

        // ToolExecutionResultMessage only triggers debug logging, no publishing
        assertThat(capturedMcpMessages).isEmpty();
        assertThat(capturedAgentMessages).isEmpty();
    }

    // -- AiMessage with text, non-agent mode --

    @Test
    void onRequest_aiMessageWithText_nonAgentMode_postsMcpMessage() {
        listener = createListener(false, true);

        AiMessage aiMessage = AiMessage.from("Here is my analysis...");
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                aiMessage,
                UserMessage.from("continue")
        ));
        listener.onRequest(ctx);

        assertThat(capturedMcpMessages).hasSize(1);
        assertThat(capturedMcpMessages.get(0).getType()).isEqualTo(MCPType.AI_MSG);
        assertThat(capturedMcpMessages.get(0).getContent()).isEqualTo("Here is my analysis...");
        assertThat(capturedAgentMessages).isEmpty();
    }

    // -- AiMessage with text, agent mode --

    @Test
    void onRequest_aiMessageWithText_agentMode_postsAgentMessage() {
        listener = createListener(true, true);

        AiMessage aiMessage = AiMessage.from("Thinking step by step...");
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                aiMessage,
                UserMessage.from("continue")
        ));
        listener.onRequest(ctx);

        assertThat(capturedAgentMessages).hasSize(1);
        assertThat(capturedAgentMessages.get(0).getType()).isEqualTo(AgentType.INTERMEDIATE_RESPONSE);
        assertThat(capturedAgentMessages.get(0).getResult()).isEqualTo("Thinking step by step...");
        assertThat(capturedMcpMessages).isEmpty();
    }

    // -- AiMessage with text, agent mode but debug logs disabled --
    // Intermediate responses are always published regardless of debug log setting
    // (the UI decides how to display them based on showToolActivityInChat setting)

    @Test
    void onRequest_aiMessageWithText_agentMode_debugLogsDisabled_stillPostsAgentMessage() {
        listener = createListener(true, false);

        AiMessage aiMessage = AiMessage.from("Thinking...");
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                aiMessage,
                UserMessage.from("continue")
        ));
        listener.onRequest(ctx);

        assertThat(capturedAgentMessages).hasSize(1);
        assertThat(capturedAgentMessages.get(0).getType()).isEqualTo(AgentType.INTERMEDIATE_RESPONSE);
        assertThat(capturedAgentMessages.get(0).getResult()).isEqualTo("Thinking...");
        assertThat(capturedMcpMessages).isEmpty();
    }

    // -- AiMessage with null/empty text --

    @Test
    void onRequest_aiMessageWithNullText_skipsTextPosting() {
        listener = createListener(false, true);

        // AiMessage with tool requests but no text
        AiMessage aiMessage = AiMessage.from(
                ToolExecutionRequest.builder().name("read_file").arguments("{}").build()
        );
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                aiMessage,
                UserMessage.from("continue")
        ));
        listener.onRequest(ctx);

        // Should post TOOL_MSG but not AI_MSG
        assertThat(capturedMcpMessages).hasSize(1);
        assertThat(capturedMcpMessages.get(0).getType()).isEqualTo(MCPType.TOOL_MSG);
    }

    @Test
    void onRequest_aiMessageWithEmptyText_skipsTextPosting() {
        listener = createListener(false, true);

        // AiMessage with empty text and tool execution requests
        AiMessage aiMessage = new AiMessage("", List.of(
                ToolExecutionRequest.builder().name("write_file").arguments("{\"path\":\"/tmp\"}").build()
        ));
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                aiMessage,
                UserMessage.from("continue")
        ));
        listener.onRequest(ctx);

        // Should only post TOOL_MSG, not AI_MSG (empty text is skipped)
        assertThat(capturedMcpMessages).hasSize(1);
        assertThat(capturedMcpMessages.get(0).getType()).isEqualTo(MCPType.TOOL_MSG);
    }

    // -- AiMessage with tool execution requests --

    @Test
    void onRequest_aiMessageWithToolRequests_nonAgentMode_postsToolMsg() {
        listener = createListener(false, true);

        AiMessage aiMessage = AiMessage.from(
                ToolExecutionRequest.builder()
                        .name("read_file")
                        .arguments("{\"path\": \"/tmp/test.txt\"}")
                        .build()
        );
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                aiMessage,
                UserMessage.from("continue")
        ));
        listener.onRequest(ctx);

        assertThat(capturedMcpMessages).hasSize(1);
        MCPMessage msg = capturedMcpMessages.get(0);
        assertThat(msg.getType()).isEqualTo(MCPType.TOOL_MSG);
        assertThat(msg.getContent()).isEqualTo("{\"path\": \"/tmp/test.txt\"}");
    }

    @Test
    void onRequest_aiMessageWithToolRequests_agentMode_doesNotPostMcpToolMsg() {
        listener = createListener(true, true);

        AiMessage aiMessage = new AiMessage("I'll read the file", List.of(
                ToolExecutionRequest.builder()
                        .name("read_file")
                        .arguments("{\"path\": \"/tmp/test.txt\"}")
                        .build()
        ));
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                aiMessage,
                UserMessage.from("continue")
        ));
        listener.onRequest(ctx);

        // In agent mode: text goes to agent message, tool requests are NOT posted to MCP panel
        assertThat(capturedAgentMessages).hasSize(1);
        assertThat(capturedMcpMessages).isEmpty();
    }

    // -- AiMessage with both text and tool requests, non-agent mode --

    @Test
    void onRequest_aiMessageWithTextAndToolRequests_nonAgentMode_postsBoth() {
        listener = createListener(false, true);

        AiMessage aiMessage = new AiMessage("Let me read that file", List.of(
                ToolExecutionRequest.builder()
                        .name("read_file")
                        .arguments("{\"path\": \"/tmp/test.txt\"}")
                        .build()
        ));
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                aiMessage,
                UserMessage.from("continue")
        ));
        listener.onRequest(ctx);

        // Should post both AI_MSG and TOOL_MSG
        assertThat(capturedMcpMessages).hasSize(2);
        assertThat(capturedMcpMessages.get(0).getType()).isEqualTo(MCPType.AI_MSG);
        assertThat(capturedMcpMessages.get(0).getContent()).isEqualTo("Let me read that file");
        assertThat(capturedMcpMessages.get(1).getType()).isEqualTo(MCPType.TOOL_MSG);
        assertThat(capturedMcpMessages.get(1).getContent()).isEqualTo("{\"path\": \"/tmp/test.txt\"}");
    }

    // -- Agent message publisher exception --

    @Test
    void onRequest_agentMessagePublisherThrows_doesNotPropagate() {
        Consumer<AgentMessage> throwingPublisher = msg -> {
            throw new RuntimeException("bus error");
        };

        listener = new MCPListenerService(
                () -> true,
                () -> true,
                capturedMcpMessages::add,
                throwingPublisher
        );

        AiMessage aiMessage = AiMessage.from("Some reasoning");
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                aiMessage,
                UserMessage.from("continue")
        ));

        // Should not throw
        listener.onRequest(ctx);
    }

    // -- Non-ChatMessage types at penultimate position --

    @Test
    void onRequest_userMessageAtPenultimate_doesNothing() {
        listener = createListener(false, true);

        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                UserMessage.from("hello"),
                UserMessage.from("world")
        ));
        listener.onRequest(ctx);

        assertThat(capturedMcpMessages).isEmpty();
        assertThat(capturedAgentMessages).isEmpty();
    }

    // -- Helpers --

    private MCPListenerService createListener(boolean agentMode, boolean agentDebugLogsEnabled) {
        return new MCPListenerService(
                () -> agentMode,
                () -> agentDebugLogsEnabled,
                capturedMcpMessages::add,
                capturedAgentMessages::add
        );
    }

    private ChatModelRequestContext createContext(List<ChatMessage> messages) {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .build();
        return new ChatModelRequestContext(chatRequest, null, Map.of());
    }

    /**
     * Creates a context with a mocked ChatRequest for cases where
     * ChatRequest.builder() rejects the messages (e.g. empty list).
     */
    private ChatModelRequestContext createContextWithMockedMessages(List<ChatMessage> messages) {
        ChatRequest chatRequest = mock(ChatRequest.class);
        when(chatRequest.messages()).thenReturn(messages);
        when(chatRequest.toString()).thenReturn("MockedChatRequest");
        return new ChatModelRequestContext(chatRequest, null, Map.of());
    }
}
