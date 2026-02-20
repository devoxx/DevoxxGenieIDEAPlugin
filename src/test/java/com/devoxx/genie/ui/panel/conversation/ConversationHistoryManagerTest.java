package com.devoxx.genie.ui.panel.conversation;

import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.conversationhistory.ConversationHistoryPanel;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConversationHistoryManagerTest {

    @Mock
    private Project project;

    @Mock
    private ConversationHistoryPanel historyPanel;

    @Mock
    private MessageRenderer messageRenderer;

    private ConversationHistoryManager manager;

    @BeforeEach
    void setUp() {
        manager = new ConversationHistoryManager(project, historyPanel, messageRenderer);
    }

    @Test
    void testLoadConversationHistory_DelegatesToHistoryPanel() {
        manager.loadConversationHistory();

        verify(historyPanel).loadConversations();
    }

    @Test
    void testRestoreConversation_EmptyMessages_SetsAndClearsRestorationFlag() {
        Conversation conversation = createConversation("conv-1", null, null);
        conversation.setMessages(new ArrayList<>());

        when(messageRenderer.isInitialized()).thenReturn(true);

        manager.restoreConversation(conversation);

        verify(messageRenderer).setRestorationInProgress(true);
        verify(messageRenderer).setRestorationInProgress(false);
        verify(messageRenderer, never()).clearWithoutWelcome();
    }

    @Test
    void testRestoreConversation_NullMessages_SetsAndClearsRestorationFlag() {
        Conversation conversation = createConversation("conv-2", null, null);
        conversation.setMessages(null);

        manager.restoreConversation(conversation);

        verify(messageRenderer).setRestorationInProgress(true);
        verify(messageRenderer).setRestorationInProgress(false);
        verify(messageRenderer, never()).clearWithoutWelcome();
    }

    @Test
    void testRestoreConversation_UserAndAiPair_AddsCompleteChatMessage() {
        Conversation conversation = createConversation("conv-3", "OpenAI", "gpt-4");
        conversation.setExecutionTimeMs(1500);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "Hello, how are you?"));
        messages.add(createChatMessage(false, "I'm doing well, thank you!"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);

        manager.restoreConversation(conversation);

        verify(messageRenderer).setRestorationInProgress(true);
        verify(messageRenderer).clearWithoutWelcome();
        verify(messageRenderer).addCompleteChatMessage(any(ChatMessageContext.class));
        verify(messageRenderer).scrollToTop();
        verify(messageRenderer).clearRestorationFlag();
        verify(messageRenderer).setRestorationInProgress(false);
    }

    @Test
    void testRestoreConversation_MultipleMessagePairs_AddsAllPairs() {
        Conversation conversation = createConversation("conv-4", "Anthropic", "claude-3");
        conversation.setExecutionTimeMs(2000);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "Question 1"));
        messages.add(createChatMessage(false, "Answer 1"));
        messages.add(createChatMessage(true, "Question 2"));
        messages.add(createChatMessage(false, "Answer 2"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);

        manager.restoreConversation(conversation);

        // Should add 2 complete chat messages (2 user/ai pairs)
        verify(messageRenderer, times(2)).addCompleteChatMessage(any(ChatMessageContext.class));
        verify(messageRenderer, never()).addUserMessageOnly(any());
    }

    @Test
    void testRestoreConversation_UserMessageWithoutAiResponse_AddsUserMessageOnly() {
        Conversation conversation = createConversation("conv-5", "OpenAI", "gpt-4");
        conversation.setExecutionTimeMs(500);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "Unanswered question"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);

        manager.restoreConversation(conversation);

        verify(messageRenderer).addUserMessageOnly(any(ChatMessageContext.class));
        verify(messageRenderer, never()).addCompleteChatMessage(any());
    }

    @Test
    void testRestoreConversation_FirstMessageIsAi_CreatesEmptyUserMessage() {
        Conversation conversation = createConversation("conv-6", "OpenAI", "gpt-4");
        conversation.setExecutionTimeMs(800);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(false, "AI starts the conversation"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);

        manager.restoreConversation(conversation);

        // The first AI message should be paired with empty user prompt
        ArgumentCaptor<ChatMessageContext> captor = ArgumentCaptor.forClass(ChatMessageContext.class);
        verify(messageRenderer).addCompleteChatMessage(captor.capture());

        ChatMessageContext captured = captor.getValue();
        assertThat(captured.getUserPrompt()).isEmpty();
        assertThat(captured.getAiMessage()).isNotNull();
        assertThat(captured.getAiMessage().text()).isEqualTo("AI starts the conversation");
    }

    @Test
    void testRestoreConversation_ContextHasStableMessageIds() {
        Conversation conversation = createConversation("conv-7", "OpenAI", "gpt-4");
        conversation.setExecutionTimeMs(1000);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "User message"));
        messages.add(createChatMessage(false, "AI message"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);

        manager.restoreConversation(conversation);

        ArgumentCaptor<ChatMessageContext> captor = ArgumentCaptor.forClass(ChatMessageContext.class);
        verify(messageRenderer).addCompleteChatMessage(captor.capture());

        ChatMessageContext captured = captor.getValue();
        assertThat(captured.getId()).isEqualTo("conv-7_msg_0");
    }

    @Test
    void testRestoreConversation_PopulatesModelInfo() {
        Conversation conversation = createConversation("conv-8", "Anthropic", "claude-3-opus");
        conversation.setApiKeyUsed(true);
        conversation.setExecutionTimeMs(1200);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "Hello"));
        messages.add(createChatMessage(false, "Hi there!"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);

        manager.restoreConversation(conversation);

        ArgumentCaptor<ChatMessageContext> captor = ArgumentCaptor.forClass(ChatMessageContext.class);
        verify(messageRenderer).addCompleteChatMessage(captor.capture());

        ChatMessageContext captured = captor.getValue();
        assertThat(captured.getLanguageModel()).isNotNull();
        assertThat(captured.getLanguageModel().getModelName()).isEqualTo("claude-3-opus");
        assertThat(captured.getLanguageModel().getProvider()).isEqualTo(ModelProvider.Anthropic);
        assertThat(captured.getLanguageModel().isApiKeyUsed()).isTrue();
    }

    @Test
    void testRestoreConversation_UnknownProvider_DoesNotThrow() {
        Conversation conversation = createConversation("conv-9", "UnknownProvider", "some-model");
        conversation.setExecutionTimeMs(500);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "Test"));
        messages.add(createChatMessage(false, "Response"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);

        // Should not throw even with an unknown provider
        manager.restoreConversation(conversation);

        ArgumentCaptor<ChatMessageContext> captor = ArgumentCaptor.forClass(ChatMessageContext.class);
        verify(messageRenderer).addCompleteChatMessage(captor.capture());

        // Model should be set but provider should be null because of IllegalArgumentException
        ChatMessageContext captured = captor.getValue();
        assertThat(captured.getLanguageModel()).isNotNull();
        assertThat(captured.getLanguageModel().getModelName()).isEqualTo("some-model");
        assertThat(captured.getLanguageModel().getProvider()).isNull();
    }

    @Test
    void testRestoreConversation_NoModelName_DoesNotSetLanguageModel() {
        Conversation conversation = createConversation("conv-10", null, null);
        conversation.setExecutionTimeMs(500);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "Test"));
        messages.add(createChatMessage(false, "Response"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);

        manager.restoreConversation(conversation);

        ArgumentCaptor<ChatMessageContext> captor = ArgumentCaptor.forClass(ChatMessageContext.class);
        verify(messageRenderer).addCompleteChatMessage(captor.capture());

        ChatMessageContext captured = captor.getValue();
        assertThat(captured.getLanguageModel()).isNull();
    }

    @Test
    void testRestoreConversation_EmptyModelName_DoesNotSetLanguageModel() {
        Conversation conversation = createConversation("conv-11", "OpenAI", "");
        conversation.setExecutionTimeMs(500);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "Test"));
        messages.add(createChatMessage(false, "Response"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);

        manager.restoreConversation(conversation);

        ArgumentCaptor<ChatMessageContext> captor = ArgumentCaptor.forClass(ChatMessageContext.class);
        verify(messageRenderer).addCompleteChatMessage(captor.capture());

        ChatMessageContext captured = captor.getValue();
        assertThat(captured.getLanguageModel()).isNull();
    }

    @Test
    void testRestoreConversation_BrowserNotInitialized_DefersRestoration() {
        Conversation conversation = createConversation("conv-12", "OpenAI", "gpt-4");
        conversation.setExecutionTimeMs(500);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "Test"));
        messages.add(createChatMessage(false, "Response"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(false);

        manager.restoreConversation(conversation);

        verify(messageRenderer).ensureBrowserInitialized(any(Runnable.class));
        // Should not directly process messages
        verify(messageRenderer, never()).clearWithoutWelcome();
    }

    @Test
    void testRestoreConversation_DefaultExecutionTime_UsesOneSecond() {
        Conversation conversation = createConversation("conv-13", "OpenAI", "gpt-4");
        conversation.setExecutionTimeMs(0); // Zero should default to 1000

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "Test"));
        messages.add(createChatMessage(false, "Response"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);

        manager.restoreConversation(conversation);

        ArgumentCaptor<ChatMessageContext> captor = ArgumentCaptor.forClass(ChatMessageContext.class);
        verify(messageRenderer).addCompleteChatMessage(captor.capture());

        ChatMessageContext captured = captor.getValue();
        assertThat(captured.getExecutionTimeMs()).isEqualTo(1000);
    }

    @Test
    void testRestoreConversation_PositiveExecutionTime_UsesActualValue() {
        Conversation conversation = createConversation("conv-14", "OpenAI", "gpt-4");
        conversation.setExecutionTimeMs(5000);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "Test"));
        messages.add(createChatMessage(false, "Response"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);

        manager.restoreConversation(conversation);

        ArgumentCaptor<ChatMessageContext> captor = ArgumentCaptor.forClass(ChatMessageContext.class);
        verify(messageRenderer).addCompleteChatMessage(captor.capture());

        ChatMessageContext captured = captor.getValue();
        assertThat(captured.getExecutionTimeMs()).isEqualTo(5000);
    }

    @Test
    void testRestoreConversation_NullApiKeyUsed_DefaultsToFalse() {
        Conversation conversation = createConversation("conv-15", "OpenAI", "gpt-4");
        conversation.setApiKeyUsed(null);
        conversation.setExecutionTimeMs(500);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "Test"));
        messages.add(createChatMessage(false, "Response"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);

        manager.restoreConversation(conversation);

        ArgumentCaptor<ChatMessageContext> captor = ArgumentCaptor.forClass(ChatMessageContext.class);
        verify(messageRenderer).addCompleteChatMessage(captor.capture());

        ChatMessageContext captured = captor.getValue();
        assertThat(captured.getLanguageModel().isApiKeyUsed()).isFalse();
    }

    @Test
    void testRestoreConversation_MixedMessages_HandlesProperly() {
        // User, AI, User (no AI response), AI (unpaired)
        Conversation conversation = createConversation("conv-16", "OpenAI", "gpt-4");
        conversation.setExecutionTimeMs(1000);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "Q1"));
        messages.add(createChatMessage(false, "A1"));
        messages.add(createChatMessage(true, "Q2 (no answer)"));
        messages.add(createChatMessage(false, "Unpaired AI"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);

        manager.restoreConversation(conversation);

        // Q1+A1 => addCompleteChatMessage
        // Q2 => next message is AI but Q2 pairs with it... wait, let me re-read logic
        // Actually: Q2 at index 2, next is AI at index 3 => they pair => addCompleteChatMessage
        verify(messageRenderer, times(2)).addCompleteChatMessage(any(ChatMessageContext.class));
        verify(messageRenderer, never()).addUserMessageOnly(any());
    }

    @Test
    void testRestoreConversation_ConsecutiveUserMessages_FirstAddedAsUserOnly() {
        Conversation conversation = createConversation("conv-17", "OpenAI", "gpt-4");
        conversation.setExecutionTimeMs(1000);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "User Q1"));
        messages.add(createChatMessage(true, "User Q2")); // Next is also user, so Q1 has no AI
        messages.add(createChatMessage(false, "AI response to Q2"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);

        manager.restoreConversation(conversation);

        // Q1 -> next is user (Q2), so Q1 goes as addUserMessageOnly
        // Q2 -> next is AI, so Q2+A2 goes as addCompleteChatMessage
        verify(messageRenderer).addUserMessageOnly(any(ChatMessageContext.class));
        verify(messageRenderer).addCompleteChatMessage(any(ChatMessageContext.class));
    }

    @Test
    void testRestoreConversation_ErrorDuringProcessing_ClearsRestorationFlag() {
        Conversation conversation = createConversation("conv-18", "OpenAI", "gpt-4");
        conversation.setExecutionTimeMs(500);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(true, "Test"));
        messages.add(createChatMessage(false, "Response"));
        conversation.setMessages(messages);

        when(messageRenderer.isInitialized()).thenReturn(true);
        doThrow(new RuntimeException("Test error")).when(messageRenderer).clearWithoutWelcome();

        assertThatThrownBy(() -> manager.restoreConversation(conversation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test error");

        // Verify restoration flag is cleared even on error
        verify(messageRenderer, atLeastOnce()).setRestorationInProgress(false);
        verify(messageRenderer).clearRestorationFlag();
    }

    // Helper methods

    private Conversation createConversation(String id, String llmProvider, String modelName) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setLlmProvider(llmProvider);
        conversation.setModelName(modelName);
        return conversation;
    }

    private ChatMessage createChatMessage(boolean isUser, String content) {
        ChatMessage message = new ChatMessage();
        message.setUser(isUser);
        message.setContent(content);
        return message;
    }
}
