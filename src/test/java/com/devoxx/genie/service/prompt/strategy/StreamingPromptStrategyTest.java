package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.response.streaming.StreamingResponseHandler;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StreamingPromptStrategyTest {

    @Mock
    private Project mockProject;
    
    @Mock
    private ChatMemoryManager mockChatMemoryManager;
    
    @Mock
    private ThreadPoolManager mockThreadPoolManager;
    
    @Mock
    private MessageCreationService mockMessageCreationService;
    
    @Mock
    private ChatMessageContext mockChatMessageContext;
    
    @Mock
    private PromptOutputPanel mockPanel;
    
    @Mock
    private PromptTask<PromptResult> mockResultTask;
    
    @Mock
    private StreamingChatLanguageModel mockStreamingModel;
    
    @Mock
    private ChatMemoryService mockChatMemoryService;
    
    @Mock 
    private ExecutorService mockExecutor;
    
    @Mock
    private LanguageModel mockLanguageModel;
    
    @Mock
    private ModelProvider mockModelProvider;
    
    private StreamingPromptStrategy strategy;

    // Add these fields to hold the static mocks
    private MockedStatic<ApplicationManager> applicationManagerMock;
    private MockedStatic<ChatMemoryManager> chatMemoryManagerMock;
    private MockedStatic<ThreadPoolManager> threadPoolManagerMock;
    private MockedStatic<MessageCreationService> messageCreationServiceMock;
    private MockedStatic<ChatMemoryService> chatMemoryServiceMock;
    private MockedStatic<FileListManager> fileListManagerMock;
    
    @Mock
    private FileListManager mockFileListManager;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup all static mocks first
        applicationManagerMock = mockStatic(ApplicationManager.class);
        chatMemoryManagerMock = mockStatic(ChatMemoryManager.class);
        threadPoolManagerMock = mockStatic(ThreadPoolManager.class);
        messageCreationServiceMock = mockStatic(MessageCreationService.class);
        chatMemoryServiceMock = mockStatic(ChatMemoryService.class);
        fileListManagerMock = mockStatic(FileListManager.class);
        
        // Setup Application mock
        com.intellij.openapi.application.Application mockApplication = mock(com.intellij.openapi.application.Application.class);
        applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(mockApplication);
        
        // Setup services in the application
        when(mockApplication.getService(ChatMemoryManager.class)).thenReturn(mockChatMemoryManager);
        when(mockApplication.getService(ThreadPoolManager.class)).thenReturn(mockThreadPoolManager);
        // when(mockApplication.getService(MessageCreationService.class)).thenReturn(mockMessageCreationService);
        when(mockApplication.getService(ChatMemoryService.class)).thenReturn(mockChatMemoryService);
        
        // Setup static getInstance methods
        chatMemoryManagerMock.when(ChatMemoryManager::getInstance).thenReturn(mockChatMemoryManager);
        threadPoolManagerMock.when(ThreadPoolManager::getInstance).thenReturn(mockThreadPoolManager);
        messageCreationServiceMock.when(MessageCreationService::getInstance).thenReturn(mockMessageCreationService);
        chatMemoryServiceMock.when(ChatMemoryService::getInstance).thenReturn(mockChatMemoryService);
        fileListManagerMock.when(FileListManager::getInstance).thenReturn(mockFileListManager);
        
        // Mock file list methods to avoid NullPointerException
        when(mockFileListManager.isEmpty(any(Project.class))).thenReturn(true);
        
        // Now configure the regular mocks
        when(mockThreadPoolManager.getPromptExecutionPool()).thenReturn(mockExecutor);
        when(mockChatMessageContext.getProject()).thenReturn(mockProject);
        when(mockChatMessageContext.getStreamingChatLanguageModel()).thenReturn(mockStreamingModel);
        // Mock userPrompt to prevent "text cannot be null or blank" exception
        when(mockChatMessageContext.getUserPrompt()).thenReturn("Test user prompt");
        
        // Mock the behavior of addUserMessageToContext to set a user message on the context
        doAnswer(invocation -> {
            ChatMessageContext ctx = invocation.getArgument(0);
            when(ctx.getUserMessage()).thenReturn(dev.langchain4j.data.message.UserMessage.from("Test user prompt"));
            return null;
        }).when(mockMessageCreationService).addUserMessageToContext(any(ChatMessageContext.class));

        List<ChatMessage> messages = new ArrayList<>();
        when(mockChatMemoryService.getMessages(any(Project.class))).thenReturn(messages);
        
        // Mock the language model and provider to avoid NullPointerException in ResponseHeaderPanel
        when(mockModelProvider.getName()).thenReturn("Test Provider");
        when(mockLanguageModel.getProvider()).thenReturn(mockModelProvider);
        when(mockLanguageModel.getModelName()).thenReturn("Test Model");
        // Important: This links the language model to the chat message context
        when(mockChatMessageContext.getLanguageModel()).thenReturn(mockLanguageModel);
        
        // Also mock the createdOn date
        when(mockChatMessageContext.getCreatedOn()).thenReturn(java.time.LocalDateTime.now());
        
        // Mock executor to execute runnables immediately to ensure message flow happens in test
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(mockExecutor).execute(any(Runnable.class));
        
        // Create the strategy with direct dependency injection to avoid Application Manager calls
        strategy = new StreamingPromptStrategy(mockProject, mockChatMemoryManager, mockThreadPoolManager, mockMessageCreationService);
    }

    @Test
    void executeStrategySpecific_shouldFollowCorrectMessageFlowOrder() {
        // Arrange
        // Use inOrder to verify the exact sequence of method calls
        org.mockito.InOrder inOrder = inOrder(mockChatMemoryManager);
        
        // Act
        strategy.executeStrategySpecific(mockChatMessageContext, mockPanel, mockResultTask);
        
        // Assert
        // Verify the correct message flow order
        inOrder.verify(mockChatMemoryManager).prepareMemory(mockChatMessageContext);
        // The MessageCreationService.addUserMessageToContext and addUserMessage calls happen inside prepareMemory
        
        // Verify that streaming starts with the context containing the user message
        verify(mockChatMemoryService).getMessages(mockProject);
        // The streaming call is made with the messages from the chat memory service
        verify(mockStreamingModel).chat(anyList(), any(StreamingResponseHandler.class));
    }
    
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // Close all static mocks to prevent memory leaks
        if (applicationManagerMock != null) applicationManagerMock.close();
        if (chatMemoryManagerMock != null) chatMemoryManagerMock.close();
        if (threadPoolManagerMock != null) threadPoolManagerMock.close();
        if (messageCreationServiceMock != null) messageCreationServiceMock.close();
        if (chatMemoryServiceMock != null) chatMemoryServiceMock.close();
        if (fileListManagerMock != null) fileListManagerMock.close();
    }
}
