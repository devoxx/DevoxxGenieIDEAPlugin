package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.nonstreaming.NonStreamingPromptExecutionService;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonStreamingPromptStrategyTest {

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
    private NonStreamingPromptExecutionService mockPromptExecutionService;

    @Mock
    private ExecutorService mockExecutor;
    
    @Mock
    private LanguageModel mockLanguageModel;
    
    @Mock
    private ModelProvider mockModelProvider;
    
    @Mock
    private FileListManager mockFileListManager;
    
    private NonStreamingPromptStrategy strategy;
    
    // Static mocks
    private MockedStatic<ApplicationManager> applicationManagerMock;
    private MockedStatic<ChatMemoryManager> chatMemoryManagerMock;
    private MockedStatic<ThreadPoolManager> threadPoolManagerMock;
    private MockedStatic<MessageCreationService> messageCreationServiceMock;
    private MockedStatic<NonStreamingPromptExecutionService> promptExecutionServiceMock;
    private MockedStatic<FileListManager> fileListManagerMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup all static mocks first
        applicationManagerMock = mockStatic(ApplicationManager.class);
        chatMemoryManagerMock = mockStatic(ChatMemoryManager.class);
        threadPoolManagerMock = mockStatic(ThreadPoolManager.class);
        messageCreationServiceMock = mockStatic(MessageCreationService.class);
        promptExecutionServiceMock = mockStatic(NonStreamingPromptExecutionService.class);
        fileListManagerMock = mockStatic(FileListManager.class);
        
        // Setup Application mock
        com.intellij.openapi.application.Application mockApplication = mock(com.intellij.openapi.application.Application.class);
        applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(mockApplication);
        
        // Setup services in the application
        when(mockApplication.getService(ChatMemoryManager.class)).thenReturn(mockChatMemoryManager);
        when(mockApplication.getService(ThreadPoolManager.class)).thenReturn(mockThreadPoolManager);
        when(mockApplication.getService(MessageCreationService.class)).thenReturn(mockMessageCreationService);
        when(mockApplication.getService(NonStreamingPromptExecutionService.class)).thenReturn(mockPromptExecutionService);
        
        // Setup static getInstance methods
        chatMemoryManagerMock.when(ChatMemoryManager::getInstance).thenReturn(mockChatMemoryManager);
        threadPoolManagerMock.when(ThreadPoolManager::getInstance).thenReturn(mockThreadPoolManager);
        messageCreationServiceMock.when(MessageCreationService::getInstance).thenReturn(mockMessageCreationService);
        promptExecutionServiceMock.when(NonStreamingPromptExecutionService::getInstance).thenReturn(mockPromptExecutionService);
        fileListManagerMock.when(FileListManager::getInstance).thenReturn(mockFileListManager);
        
        // Mock file list methods to avoid NullPointerException
        when(mockFileListManager.isEmpty(any(Project.class))).thenReturn(true);
        
        // Mock the language model and provider to avoid NullPointerException in ResponseHeaderPanel
        when(mockModelProvider.getName()).thenReturn("Test Provider");
        when(mockLanguageModel.getProvider()).thenReturn(mockModelProvider);
        when(mockLanguageModel.getModelName()).thenReturn("Test Model");
        // Important: This links the language model to the chat message context
        when(mockChatMessageContext.getLanguageModel()).thenReturn(mockLanguageModel);
        
        // Also mock the createdOn date
        when(mockChatMessageContext.getCreatedOn()).thenReturn(java.time.LocalDateTime.now());
        
        // Setup other required mocks
        when(mockThreadPoolManager.getPromptExecutionPool()).thenReturn(mockExecutor);
        when(mockPromptExecutionService.executeQuery(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(mockChatMessageContext.getProject()).thenReturn(mockProject);
        
        // IMPORTANT: Set up the ChatMemoryService mock inside the ChatMemoryManager
        // This is the key to fixing the NullPointerException
        try {
            java.lang.reflect.Field chatMemoryServiceField = ChatMemoryManager.class.getDeclaredField("chatMemoryService");
            chatMemoryServiceField.setAccessible(true);
            
            // Create a mock ChatMemoryService
            com.devoxx.genie.service.prompt.memory.ChatMemoryService mockChatMemoryService = 
                mock(com.devoxx.genie.service.prompt.memory.ChatMemoryService.class);
            
            // Mock isEmpty to return false to avoid initializing memory
            when(mockChatMemoryService.isEmpty(any(Project.class))).thenReturn(false);
            
            // Set the field value
            chatMemoryServiceField.set(mockChatMemoryManager, mockChatMemoryService);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to set chatMemoryService field", e);
        }
        
        // Create the strategy with direct dependency injection
        strategy = new NonStreamingPromptStrategy(
            mockProject, 
            mockChatMemoryManager, 
            mockThreadPoolManager, 
            mockPromptExecutionService
        );
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // Close all static mocks to prevent memory leaks
        if (applicationManagerMock != null) applicationManagerMock.close();
        if (chatMemoryManagerMock != null) chatMemoryManagerMock.close();
        if (threadPoolManagerMock != null) threadPoolManagerMock.close();
        if (messageCreationServiceMock != null) messageCreationServiceMock.close();
        if (promptExecutionServiceMock != null) promptExecutionServiceMock.close();
        if (fileListManagerMock != null) fileListManagerMock.close();
    }
}
