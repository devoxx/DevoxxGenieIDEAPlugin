package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.conversations.ConversationStorageService;
import com.devoxx.genie.ui.panel.conversation.ConversationManager;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import dev.langchain4j.data.message.AiMessage;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {

    @Mock
    private Project project;

    @Mock
    private MessageBus messageBus;

    @Mock
    private MessageBusConnection messageBusConnection;

    @Mock
    private ConversationStorageService storageService;

    @Mock
    private ConversationManager conversationManager;

    @Mock
    private LanguageModel languageModel;

    @Mock
    private AiMessage aiMessage;

    private MockedStatic<ConversationStorageService> storageServiceMockedStatic;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        when(project.getMessageBus()).thenReturn(messageBus);
        when(messageBus.connect()).thenReturn(messageBusConnection);
        when(project.getName()).thenReturn("TestProject");

        storageServiceMockedStatic = Mockito.mockStatic(ConversationStorageService.class);
        storageServiceMockedStatic.when(ConversationStorageService::getInstance).thenReturn(storageService);

        chatService = new ChatService(project);
    }

    @AfterEach
    void tearDown() {
        storageServiceMockedStatic.close();
    }

    @Test
    void testConstructorSubscribesToConversationTopic() {
        verify(messageBusConnection).subscribe(eq(AppTopics.CONVERSATION_TOPIC), eq(chatService));
    }

    @Test
    void testSetConversationManager() {
        chatService.setConversationManager(conversationManager);
        // No exception, the setter accepts a non-null value
        chatService.setConversationManager(null);
        // No exception, the setter accepts null
    }

    @Test
    void testOnNewConversation_CreatesNewConversation_WhenNoCurrentConversation() {
        when(languageModel.getModelName()).thenReturn("gpt-4");
        when(languageModel.isApiKeyUsed()).thenReturn(true);
        when(languageModel.getProvider()).thenReturn(ModelProvider.OpenAI);
        when(aiMessage.text()).thenReturn("AI response text");

        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt("Hello, how are you?")
                .languageModel(languageModel)
                .aiMessage(aiMessage)
                .executionTimeMs(150)
                .build();

        chatService.onNewConversation(context);

        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(storageService).addConversation(eq(project), conversationCaptor.capture());

        Conversation savedConversation = conversationCaptor.getValue();
        assertThat(savedConversation.getTitle()).isEqualTo("Hello, how are you?");
        assertThat(savedConversation.getModelName()).isEqualTo("gpt-4");
        assertThat(savedConversation.getLlmProvider()).isEqualTo("OpenAI");
        assertThat(savedConversation.getApiKeyUsed()).isTrue();
        assertThat(savedConversation.getExecutionTimeMs()).isEqualTo(150);
        assertThat(savedConversation.getMessages()).hasSize(2);
        assertThat(savedConversation.getMessages().get(0).isUser()).isTrue();
        assertThat(savedConversation.getMessages().get(0).getContent()).isEqualTo("Hello, how are you?");
        assertThat(savedConversation.getMessages().get(1).isUser()).isFalse();
        assertThat(savedConversation.getMessages().get(1).getContent()).isEqualTo("AI response text");
    }

    @Test
    void testOnNewConversation_AppendsToExistingConversation() {
        chatService.setConversationManager(conversationManager);

        Conversation existingConversation = new Conversation();
        existingConversation.setId("existing-id");
        existingConversation.setTitle("Existing conversation");
        existingConversation.setExecutionTimeMs(100);
        existingConversation.setMessages(new java.util.ArrayList<>());
        when(conversationManager.getCurrentConversation()).thenReturn(existingConversation);

        when(languageModel.getModelName()).thenReturn("gpt-4");
        when(languageModel.isApiKeyUsed()).thenReturn(true);
        when(languageModel.getProvider()).thenReturn(ModelProvider.OpenAI);
        when(aiMessage.text()).thenReturn("Second response");

        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt("Follow-up question")
                .languageModel(languageModel)
                .aiMessage(aiMessage)
                .executionTimeMs(200)
                .build();

        chatService.onNewConversation(context);

        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(storageService).addConversation(eq(project), conversationCaptor.capture());

        Conversation savedConversation = conversationCaptor.getValue();
        assertThat(savedConversation.getId()).isEqualTo("existing-id");
        assertThat(savedConversation.getExecutionTimeMs()).isEqualTo(300); // 100 + 200
        assertThat(savedConversation.getMessages()).hasSize(2);
    }

    @Test
    void testOnNewConversation_SkipsWhenProjectNameDoesNotMatch() {
        Project otherProject = mock(Project.class);
        when(otherProject.getName()).thenReturn("OtherProject");

        ChatMessageContext context = ChatMessageContext.builder()
                .project(otherProject)
                .userPrompt("Hello")
                .languageModel(languageModel)
                .aiMessage(aiMessage)
                .build();

        chatService.onNewConversation(context);

        verify(storageService, never()).addConversation(any(), any());
    }

    @Test
    void testOnNewConversation_SkipsWhenPromptIsNull() {
        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt(null)
                .languageModel(languageModel)
                .aiMessage(aiMessage)
                .build();

        chatService.onNewConversation(context);

        verify(storageService, never()).addConversation(any(), any());
    }

    @Test
    void testOnNewConversation_SkipsWhenPromptIsBlank() {
        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt("   ")
                .languageModel(languageModel)
                .aiMessage(aiMessage)
                .build();

        chatService.onNewConversation(context);

        verify(storageService, never()).addConversation(any(), any());
    }

    @Test
    void testOnNewConversation_SetsCurrentConversationOnManager() {
        chatService.setConversationManager(conversationManager);
        when(conversationManager.getCurrentConversation()).thenReturn(null);

        when(languageModel.getModelName()).thenReturn("gpt-4");
        when(languageModel.isApiKeyUsed()).thenReturn(true);
        when(languageModel.getProvider()).thenReturn(ModelProvider.OpenAI);
        when(aiMessage.text()).thenReturn("Response");

        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt("Test prompt")
                .languageModel(languageModel)
                .aiMessage(aiMessage)
                .executionTimeMs(50)
                .build();

        chatService.onNewConversation(context);

        verify(conversationManager).setCurrentConversation(any(Conversation.class));
    }

    @Test
    void testExtractTitle_ShortPrompt() {
        when(languageModel.getModelName()).thenReturn("gpt-4");
        when(languageModel.isApiKeyUsed()).thenReturn(false);
        when(languageModel.getProvider()).thenReturn(ModelProvider.OpenAI);
        when(aiMessage.text()).thenReturn("response");

        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt("Short prompt")
                .languageModel(languageModel)
                .aiMessage(aiMessage)
                .executionTimeMs(10)
                .build();

        chatService.onNewConversation(context);

        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(storageService).addConversation(eq(project), conversationCaptor.capture());

        assertThat(conversationCaptor.getValue().getTitle()).isEqualTo("Short prompt");
    }

    @Test
    void testExtractTitle_LongPromptIsTruncated() {
        when(languageModel.getModelName()).thenReturn("gpt-4");
        when(languageModel.isApiKeyUsed()).thenReturn(false);
        when(languageModel.getProvider()).thenReturn(ModelProvider.OpenAI);
        when(aiMessage.text()).thenReturn("response");

        String longPrompt = "A".repeat(150);
        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt(longPrompt)
                .languageModel(languageModel)
                .aiMessage(aiMessage)
                .executionTimeMs(10)
                .build();

        chatService.onNewConversation(context);

        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(storageService).addConversation(eq(project), conversationCaptor.capture());

        String title = conversationCaptor.getValue().getTitle();
        assertThat(title).hasSize(103); // 100 + "..."
        assertThat(title).endsWith("...");
    }

    @Test
    void testExtractTitle_MultilinePromptCollapsed() {
        when(languageModel.getModelName()).thenReturn("gpt-4");
        when(languageModel.isApiKeyUsed()).thenReturn(false);
        when(languageModel.getProvider()).thenReturn(ModelProvider.OpenAI);
        when(aiMessage.text()).thenReturn("response");

        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt("Line 1\nLine 2\n\tLine 3")
                .languageModel(languageModel)
                .aiMessage(aiMessage)
                .executionTimeMs(10)
                .build();

        chatService.onNewConversation(context);

        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(storageService).addConversation(eq(project), conversationCaptor.capture());

        String title = conversationCaptor.getValue().getTitle();
        assertThat(title).doesNotContain("\n");
        assertThat(title).doesNotContain("\t");
        assertThat(title).isEqualTo("Line 1 Line 2 Line 3");
    }

    @Test
    void testStartNewConversation_WithTitle() {
        // startNewConversation uses the message bus, so mock the syncPublisher
        com.devoxx.genie.ui.listener.ConversationEventListener mockListener = mock(com.devoxx.genie.ui.listener.ConversationEventListener.class);
        when(messageBus.syncPublisher(AppTopics.CONVERSATION_TOPIC)).thenReturn(mockListener);

        chatService.startNewConversation("My Custom Title");

        verify(mockListener).onNewConversation(any(ChatMessageContext.class));
    }

    @Test
    void testStartNewConversation_WithNullTitle() {
        com.devoxx.genie.ui.listener.ConversationEventListener mockListener = mock(com.devoxx.genie.ui.listener.ConversationEventListener.class);
        when(messageBus.syncPublisher(AppTopics.CONVERSATION_TOPIC)).thenReturn(mockListener);

        chatService.startNewConversation(null);

        verify(mockListener).onNewConversation(any(ChatMessageContext.class));
    }

    @Test
    void testStartNewConversation_WithEmptyTitle() {
        com.devoxx.genie.ui.listener.ConversationEventListener mockListener = mock(com.devoxx.genie.ui.listener.ConversationEventListener.class);
        when(messageBus.syncPublisher(AppTopics.CONVERSATION_TOPIC)).thenReturn(mockListener);

        chatService.startNewConversation("   ");

        verify(mockListener).onNewConversation(any(ChatMessageContext.class));
    }
}
