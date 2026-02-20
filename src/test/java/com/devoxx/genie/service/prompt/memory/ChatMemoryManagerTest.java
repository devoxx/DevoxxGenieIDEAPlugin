package com.devoxx.genie.service.prompt.memory;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatMemoryManagerTest {

    @Mock
    private ChatMemoryService mockChatMemoryService;

    @Mock
    private Project mockProject;

    @Mock
    private ChatMessageContext mockContext;

    private ChatMemoryManager chatMemoryManager;

    @BeforeEach
    void setUp() {
        lenient().when(mockContext.getProject()).thenReturn(mockProject);
        lenient().when(mockContext.getId()).thenReturn("test-id");

        try (MockedStatic<ChatMemoryService> chatMemoryServiceMockedStatic = Mockito.mockStatic(ChatMemoryService.class)) {
            chatMemoryServiceMockedStatic.when(ChatMemoryService::getInstance).thenReturn(mockChatMemoryService);
            chatMemoryManager = new ChatMemoryManager();
        }
    }

    @Test
    void testAddUserMessagePreservesImageContent() {
        // Create a multimodal UserMessage with text + image
        UserMessage multimodalMessage = UserMessage.from(
                TextContent.from("Describe this image"),
                ImageContent.from("base64data", "image/png")
        );

        when(mockContext.getUserMessage()).thenReturn(multimodalMessage);

        // Capture the message added to memory
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        chatMemoryManager.addUserMessage(mockContext);

        verify(mockChatMemoryService).addMessage(eq(mockProject), messageCaptor.capture());

        ChatMessage captured = messageCaptor.getValue();
        assertInstanceOf(UserMessage.class, captured);
        UserMessage capturedUserMessage = (UserMessage) captured;

        // Should still be multimodal
        assertFalse(capturedUserMessage.hasSingleText());

        List<Content> contents = capturedUserMessage.contents();
        assertEquals(2, contents.size());
        assertInstanceOf(TextContent.class, contents.get(0));
        assertInstanceOf(ImageContent.class, contents.get(1));

        // Verify the image data is preserved
        ImageContent imageContent = (ImageContent) contents.get(1);
        assertEquals("image/png", imageContent.image().mimeType());
        assertEquals("base64data", imageContent.image().base64Data());
    }

    @Test
    void testAddUserMessageTextOnlyPath() {
        UserMessage textMessage = UserMessage.from("Hello world");
        when(mockContext.getUserMessage()).thenReturn(textMessage);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        chatMemoryManager.addUserMessage(mockContext);

        verify(mockChatMemoryService).addMessage(eq(mockProject), messageCaptor.capture());

        ChatMessage captured = messageCaptor.getValue();
        assertInstanceOf(UserMessage.class, captured);
        UserMessage capturedUserMessage = (UserMessage) captured;

        // Should be single text
        assertTrue(capturedUserMessage.hasSingleText());
        assertEquals("Hello world", capturedUserMessage.singleText());
    }

    @Test
    void testAddUserMessageWithTemplateVariablesInMultimodal() {
        // Template variables like {{var}} should be escaped in text but images preserved
        UserMessage multimodalMessage = UserMessage.from(
                TextContent.from("Describe {{this}} image"),
                ImageContent.from("imagedata", "image/jpeg")
        );

        when(mockContext.getUserMessage()).thenReturn(multimodalMessage);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        chatMemoryManager.addUserMessage(mockContext);

        verify(mockChatMemoryService).addMessage(eq(mockProject), messageCaptor.capture());

        ChatMessage captured = messageCaptor.getValue();
        UserMessage capturedUserMessage = (UserMessage) captured;

        // Should still be multimodal
        assertFalse(capturedUserMessage.hasSingleText());

        List<Content> contents = capturedUserMessage.contents();
        assertEquals(2, contents.size());

        // Text should have template variables escaped
        TextContent textContent = (TextContent) contents.get(0);
        assertNotNull(textContent.text());

        // Image should be preserved as-is
        ImageContent imageContent = (ImageContent) contents.get(1);
        assertEquals("imagedata", imageContent.image().base64Data());
    }
}
