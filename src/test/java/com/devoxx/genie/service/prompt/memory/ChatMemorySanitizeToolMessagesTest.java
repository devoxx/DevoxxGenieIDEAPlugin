package com.devoxx.genie.service.prompt.memory;

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
 * Tests {@link ChatMemoryManager#sanitizeOrphanedToolMessages(String)}: a cancelled tool loop
 * can leave a dangling tool_use in memory, which breaks all later requests until restart.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatMemorySanitizeToolMessagesTest {

    private static final String KEY = "project-hash-tab";

    @Mock
    private ChatMemoryService mockChatMemoryService;

    private ChatMemoryManager chatMemoryManager;

    @BeforeEach
    void setUp() {
        try (MockedStatic<ChatMemoryService> staticMock = Mockito.mockStatic(ChatMemoryService.class)) {
            staticMock.when(ChatMemoryService::getInstance).thenReturn(mockChatMemoryService);
            chatMemoryManager = new ChatMemoryManager();
        }
        when(mockChatMemoryService.hasMemory(KEY)).thenReturn(true);
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

    private static ToolExecutionResultMessage toolResult(String id) {
        return ToolExecutionResultMessage.from(id, "some_tool", "result");
    }

    @SuppressWarnings("unchecked")
    private List<ChatMessage> capturedRemoval() {
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockChatMemoryService).removeMessagesByKey(eq(KEY), captor.capture());
        return captor.getValue();
    }

    @Test
    void removesDanglingToolUseWithNoResults() {
        ChatMessage userMessage = UserMessage.from("do something");
        AiMessage danglingToolUse = toolUse("call_1");
        when(mockChatMemoryService.getMessagesByKey(KEY))
                .thenReturn(List.of(SystemMessage.from("sys"), userMessage, danglingToolUse));

        chatMemoryManager.sanitizeOrphanedToolMessages(KEY);

        List<ChatMessage> removed = capturedRemoval();
        Assertions.assertEquals(1, removed.size());
        Assertions.assertTrue(removed.contains(danglingToolUse));
    }

    @Test
    void removesDanglingToolUseWithPartialResults() {
        // AiMessage requested two tools but only one result was produced before cancellation.
        ChatMessage userMessage = UserMessage.from("do something");
        AiMessage toolUse = toolUse("call_1", "call_2");
        ToolExecutionResultMessage partial = toolResult("call_1");
        when(mockChatMemoryService.getMessagesByKey(KEY))
                .thenReturn(List.of(userMessage, toolUse, partial));

        chatMemoryManager.sanitizeOrphanedToolMessages(KEY);

        List<ChatMessage> removed = capturedRemoval();
        // Both the tool_use and the partial result must go.
        Assertions.assertEquals(2, removed.size());
        Assertions.assertTrue(removed.contains(toolUse));
        Assertions.assertTrue(removed.contains(partial));
    }

    @Test
    void removesTrailingResultsWithNoOriginatingToolUse() {
        ChatMessage userMessage = UserMessage.from("do something");
        ToolExecutionResultMessage orphanResult = toolResult("call_1");
        when(mockChatMemoryService.getMessagesByKey(KEY))
                .thenReturn(List.of(userMessage, orphanResult));

        chatMemoryManager.sanitizeOrphanedToolMessages(KEY);

        List<ChatMessage> removed = capturedRemoval();
        Assertions.assertEquals(1, removed.size());
        Assertions.assertTrue(removed.contains(orphanResult));
    }

    @Test
    void keepsValidCompletedToolExchange() {
        // A complete request/result pair must be preserved.
        ChatMessage userMessage = UserMessage.from("do something");
        AiMessage toolUse = toolUse("call_1");
        ToolExecutionResultMessage result = toolResult("call_1");
        when(mockChatMemoryService.getMessagesByKey(KEY))
                .thenReturn(List.of(userMessage, toolUse, result));

        chatMemoryManager.sanitizeOrphanedToolMessages(KEY);

        verify(mockChatMemoryService, never()).removeMessagesByKey(eq(KEY), anyList());
    }

    @Test
    void noopWhenTailIsPlainAiMessage() {
        ChatMessage userMessage = UserMessage.from("do something");
        AiMessage finalAnswer = AiMessage.from("here is the answer");
        when(mockChatMemoryService.getMessagesByKey(KEY))
                .thenReturn(List.of(userMessage, finalAnswer));

        chatMemoryManager.sanitizeOrphanedToolMessages(KEY);

        verify(mockChatMemoryService, never()).removeMessagesByKey(eq(KEY), anyList());
    }

    @Test
    void noopWhenMemoryMissing() {
        when(mockChatMemoryService.hasMemory(KEY)).thenReturn(false);

        chatMemoryManager.sanitizeOrphanedToolMessages(KEY);

        verify(mockChatMemoryService, never()).getMessagesByKey(KEY);
        verify(mockChatMemoryService, never()).removeMessagesByKey(eq(KEY), anyList());
    }
}
