package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.agent.AgentType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MCPListenerServiceTest {

    private List<ActivityMessage> capturedMessages;
    private MCPListenerService listener;

    // Simulates the last message in an agent loop turn (tool result followed by next LLM call)
    private static final ToolExecutionResultMessage TOOL_RESULT =
            ToolExecutionResultMessage.from("id-1", "some_tool", "result");

    @BeforeEach
    void setUp() {
        capturedMessages = new ArrayList<>();
    }

    // -- Early return cases --

    @Test
    void onRequest_emptyMessagesList_doesNothing() {
        listener = createListener(false, true);

        ChatModelRequestContext ctx = createContextWithMockedMessages(List.of());
        listener.onRequest(ctx);

        assertThat(capturedMessages).isEmpty();
    }

    @Test
    void onRequest_singleMessage_doesNothing() {
        listener = createListener(false, true);

        ChatModelRequestContext ctx = createContextWithMockedMessages(List.of(
                UserMessage.from("hello")
        ));
        listener.onRequest(ctx);

        assertThat(capturedMessages).isEmpty();
    }

    @Test
    void onRequest_twoMessages_doesNothing() {
        listener = createListener(false, true);

        ChatModelRequestContext ctx = createContextWithMockedMessages(List.of(
                SystemMessage.from("system"),
                UserMessage.from("hello")
        ));
        listener.onRequest(ctx);

        assertThat(capturedMessages).isEmpty();
    }

    // -- Last message is UserMessage → skip (prevents stale response flash) --

    @Test
    void onRequest_lastMessageIsUserMessage_skipsToPreventStaleFlash() {
        listener = createListener(true, true);

        // Simulates: 2nd prompt's first LLM call where the second-to-last message
        // is the previous prompt's final AI response. Publishing this would flash
        // old content under the new "Thinking..." indicator.
        AiMessage previousResponse = AiMessage.from("Previous prompt's final response");
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                previousResponse,
                UserMessage.from("New user prompt")
        ));
        listener.onRequest(ctx);

        assertThat(capturedMessages).isEmpty();
    }

    @Test
    void onRequest_lastMessageIsUserMessage_nonAgentMode_alsoSkips() {
        listener = createListener(false, true);

        AiMessage previousResponse = AiMessage.from("Previous response");
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                previousResponse,
                UserMessage.from("New prompt")
        ));
        listener.onRequest(ctx);

        assertThat(capturedMessages).isEmpty();
    }

    // -- ToolExecutionResultMessage at penultimate position --

    @Test
    void onRequest_toolExecutionResultMessage_justLogs() {
        listener = createListener(false, true);

        ToolExecutionResultMessage toolResult = ToolExecutionResultMessage.from("id-1", "read_file", "file contents");
        // Use a simpler setup: toolResult at penultimate, followed by non-UserMessage
        ChatModelRequestContext ctx2 = createContext(List.of(
                SystemMessage.from("system"),
                toolResult,
                AiMessage.from("continue processing")
        ));
        listener.onRequest(ctx2);

        // ToolExecutionResultMessage only triggers debug logging, no publishing
        // (the penultimate message is toolResult, not AiMessage)
        assertThat(capturedMessages).isEmpty();
    }

    // -- AiMessage with text, non-agent mode (in agent loop: last msg is tool result) --

    @Test
    void onRequest_aiMessageWithText_nonAgentMode_postsMcpMessage() {
        listener = createListener(false, true);

        AiMessage aiMessage = AiMessage.from("Here is my analysis...");
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                aiMessage,
                TOOL_RESULT
        ));
        listener.onRequest(ctx);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0).getSource()).isEqualTo(ActivitySource.MCP);
        assertThat(capturedMessages.get(0).getMcpType()).isEqualTo(MCPType.AI_MSG);
        assertThat(capturedMessages.get(0).getContent()).isEqualTo("Here is my analysis...");
    }

    // -- AiMessage with text, agent mode --

    @Test
    void onRequest_aiMessageWithText_agentMode_postsAgentMessage() {
        listener = createListener(true, true);

        AiMessage aiMessage = AiMessage.from("Thinking step by step...");
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                aiMessage,
                TOOL_RESULT
        ));
        listener.onRequest(ctx);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0).getSource()).isEqualTo(ActivitySource.AGENT);
        assertThat(capturedMessages.get(0).getAgentType()).isEqualTo(AgentType.INTERMEDIATE_RESPONSE);
        assertThat(capturedMessages.get(0).getResult()).isEqualTo("Thinking step by step...");
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
                TOOL_RESULT
        ));
        listener.onRequest(ctx);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0).getSource()).isEqualTo(ActivitySource.AGENT);
        assertThat(capturedMessages.get(0).getAgentType()).isEqualTo(AgentType.INTERMEDIATE_RESPONSE);
        assertThat(capturedMessages.get(0).getResult()).isEqualTo("Thinking...");
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
                TOOL_RESULT
        ));
        listener.onRequest(ctx);

        // Should post TOOL_MSG but not AI_MSG
        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0).getMcpType()).isEqualTo(MCPType.TOOL_MSG);
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
                TOOL_RESULT
        ));
        listener.onRequest(ctx);

        // Should only post TOOL_MSG, not AI_MSG (empty text is skipped)
        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0).getMcpType()).isEqualTo(MCPType.TOOL_MSG);
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
                TOOL_RESULT
        ));
        listener.onRequest(ctx);

        assertThat(capturedMessages).hasSize(1);
        ActivityMessage msg = capturedMessages.get(0);
        assertThat(msg.getSource()).isEqualTo(ActivitySource.MCP);
        assertThat(msg.getMcpType()).isEqualTo(MCPType.TOOL_MSG);
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
                TOOL_RESULT
        ));
        listener.onRequest(ctx);

        // In agent mode: text goes to agent message, tool requests are NOT posted to MCP panel
        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0).getSource()).isEqualTo(ActivitySource.AGENT);
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
                TOOL_RESULT
        ));
        listener.onRequest(ctx);

        // Should post both AI_MSG and TOOL_MSG
        assertThat(capturedMessages).hasSize(2);
        assertThat(capturedMessages.get(0).getMcpType()).isEqualTo(MCPType.AI_MSG);
        assertThat(capturedMessages.get(0).getContent()).isEqualTo("Let me read that file");
        assertThat(capturedMessages.get(1).getMcpType()).isEqualTo(MCPType.TOOL_MSG);
        assertThat(capturedMessages.get(1).getContent()).isEqualTo("{\"path\": \"/tmp/test.txt\"}");
    }

    // -- Activity message publisher exception --

    @Test
    void onRequest_activityMessagePublisherThrows_doesNotPropagate() {
        listener = new MCPListenerService(
                () -> true,
                () -> true,
                msg -> { throw new RuntimeException("bus error"); }
        );

        AiMessage aiMessage = AiMessage.from("Some reasoning");
        ChatModelRequestContext ctx = createContext(List.of(
                SystemMessage.from("system"),
                aiMessage,
                TOOL_RESULT
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

        assertThat(capturedMessages).isEmpty();
    }

    // -- Helpers --

    private MCPListenerService createListener(boolean agentMode, boolean agentDebugLogsEnabled) {
        return new MCPListenerService(
                () -> agentMode,
                () -> agentDebugLogsEnabled,
                capturedMessages::add
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
