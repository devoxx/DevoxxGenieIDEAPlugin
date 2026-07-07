package com.devoxx.genie.service.prompt.memory;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reproduces issue #1193: an agent tool loop that dies between writing the
 * AiMessage(tool_calls) and its ToolExecutionResultMessage(s) — e.g. a hallucinated
 * tool name (Langchain4j default strategy: throw), a round-trip-limit throw, or a
 * streaming error — leaves a dangling tool_use tail in chat memory. Every subsequent
 * request in the conversation is then rejected by OpenAI-compatible providers with
 * "An assistant message with 'tool_calls' must be followed by tool messages responding
 * to each 'tool_call_id'".
 *
 * <p>The fix: {@link ChatMemoryManager#prepareMemory(ChatMessageContext)} sanitizes
 * the orphaned tool tail before every new prompt, so a corrupted conversation heals
 * itself on the next message instead of being broken until restart.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatMemoryPrepareMemorySanitizeTest {

    private static final String PROJECT_HASH = "project-hash";

    @Mock
    private ChatMemoryService mockChatMemoryService;

    @Mock
    private Project mockProject;

    private ChatMemoryManager chatMemoryManager;

    @BeforeEach
    void setUp() {
        try (MockedStatic<ChatMemoryService> staticMock = Mockito.mockStatic(ChatMemoryService.class)) {
            staticMock.when(ChatMemoryService::getInstance).thenReturn(mockChatMemoryService);
            chatMemoryManager = new ChatMemoryManager();
        }
        when(mockProject.getLocationHash()).thenReturn(PROJECT_HASH);
        when(mockChatMemoryService.hasMemory(PROJECT_HASH)).thenReturn(true);
        when(mockChatMemoryService.isEmptyByKey(PROJECT_HASH)).thenReturn(false);
    }

    private ChatMessageContext context() {
        return ChatMessageContext.builder()
                .id("ctx-1")
                .project(mockProject)
                .userPrompt("next prompt")
                .build();
    }

    private static AiMessage toolUse(String... ids) {
        ToolExecutionRequest[] requests = new ToolExecutionRequest[ids.length];
        for (int i = 0; i < ids.length; i++) {
            requests[i] = ToolExecutionRequest.builder()
                    .id(ids[i])
                    .name("some_tool")
                    .arguments("{}")
                    .build();
        }
        return AiMessage.from(requests);
    }

    @SuppressWarnings("unchecked")
    private List<ChatMessage> capturedRemoval() {
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockChatMemoryService).removeMessagesByKey(eq(PROJECT_HASH), captor.capture());
        return captor.getValue();
    }

    @Test
    void prepareMemoryRemovesDanglingToolUseLeftByFailedToolLoop() {
        // A previous turn died after the AiMessage(tool_calls) was added to memory
        // but before any tool result was written (issue #1193).
        AiMessage danglingToolUse = toolUse("call_1");
        when(mockChatMemoryService.getMessagesByKey(PROJECT_HASH))
                .thenReturn(List.of(
                        SystemMessage.from("sys"),
                        UserMessage.from("previous prompt"),
                        danglingToolUse));

        chatMemoryManager.prepareMemory(context());

        List<ChatMessage> removed = capturedRemoval();
        Assertions.assertEquals(1, removed.size());
        Assertions.assertTrue(removed.contains(danglingToolUse));
    }

    @Test
    void prepareMemoryRemovesToolUseWithPartialResults() {
        // The turn died after only one of two tool results was written.
        AiMessage toolUse = toolUse("call_1", "call_2");
        ToolExecutionResultMessage partial = ToolExecutionResultMessage.from(
                ToolExecutionRequest.builder().id("call_1").name("some_tool").arguments("{}").build(),
                "result");
        when(mockChatMemoryService.getMessagesByKey(PROJECT_HASH))
                .thenReturn(List.of(
                        UserMessage.from("previous prompt"),
                        toolUse,
                        partial));

        chatMemoryManager.prepareMemory(context());

        List<ChatMessage> removed = capturedRemoval();
        Assertions.assertEquals(2, removed.size());
        Assertions.assertTrue(removed.contains(toolUse));
        Assertions.assertTrue(removed.contains(partial));
    }

    @Test
    void prepareMemoryKeepsHealthyConversationIntact() {
        AiMessage toolUse = toolUse("call_1");
        ToolExecutionResultMessage result = ToolExecutionResultMessage.from(
                ToolExecutionRequest.builder().id("call_1").name("some_tool").arguments("{}").build(),
                "result");
        when(mockChatMemoryService.getMessagesByKey(PROJECT_HASH))
                .thenReturn(List.of(
                        UserMessage.from("previous prompt"),
                        toolUse,
                        result,
                        AiMessage.from("final answer")));

        chatMemoryManager.prepareMemory(context());

        verify(mockChatMemoryService, never()).removeMessagesByKey(eq(PROJECT_HASH), anyList());
    }
}
