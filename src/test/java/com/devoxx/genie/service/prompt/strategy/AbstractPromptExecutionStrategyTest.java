package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbstractPromptExecutionStrategyTest {

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
    
    private TestPromptExecutionStrategy testStrategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockStaticDependencies();
        
        // Use the constructor that bypasses ApplicationManager.getApplication()
        testStrategy = new TestPromptExecutionStrategy(
            mockProject, 
            mockChatMemoryManager, 
            mockThreadPoolManager,
            mockMessageCreationService
        );
    }
    
    private void mockStaticDependencies() {
        // Mock ApplicationManager first
        try (MockedStatic<ApplicationManager> applicationManagerMockedStatic = mockStatic(ApplicationManager.class)) {
            // Create a mock Application
            com.intellij.openapi.application.Application mockApplication = mock(com.intellij.openapi.application.Application.class);
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(mockApplication);
            
            // Setup services in the application
            when(mockApplication.getService(ChatMemoryManager.class)).thenReturn(mockChatMemoryManager);
            when(mockApplication.getService(ThreadPoolManager.class)).thenReturn(mockThreadPoolManager);
            when(mockApplication.getService(MessageCreationService.class)).thenReturn(mockMessageCreationService);
            
            // Mock static getInstance methods
            try (var chatMemoryManagerStaticMock = mockStatic(ChatMemoryManager.class);
                 var threadPoolManagerStaticMock = mockStatic(ThreadPoolManager.class);
                 var messageCreationServiceStaticMock = mockStatic(MessageCreationService.class)) {
                
                chatMemoryManagerStaticMock.when(ChatMemoryManager::getInstance)
                        .thenReturn(mockChatMemoryManager);
                
                threadPoolManagerStaticMock.when(ThreadPoolManager::getInstance)
                        .thenReturn(mockThreadPoolManager);
                
                messageCreationServiceStaticMock.when(MessageCreationService::getInstance)
                        .thenReturn(mockMessageCreationService);
            }
        }
    }

    @Test
    void prepareMemory_shouldCallMessageCreationServiceBeforeChatMemoryManager() {
        // Arrange
        when(mockProject.getName()).thenReturn("TestProject");
        
        // Act
        testStrategy.prepareMemory(mockChatMessageContext);
        
        // Assert - verify correct order of operations
        InOrder inOrder = inOrder(mockChatMemoryManager, mockMessageCreationService);
        inOrder.verify(mockChatMemoryManager).prepareMemory(mockChatMessageContext);
        inOrder.verify(mockMessageCreationService).addUserMessageToContext(mockChatMessageContext);
        inOrder.verify(mockChatMemoryManager).addUserMessage(mockChatMessageContext);
    }
    
    /**
     * Test implementation of the abstract class
     */
    private static class TestPromptExecutionStrategy extends AbstractPromptExecutionStrategy {
        
        public TestPromptExecutionStrategy(Project project) {
            super(project);
        }
        
        @Override
        protected void executeStrategySpecific(ChatMessageContext context, 
                                             com.devoxx.genie.ui.panel.PromptOutputPanel panel, 
                                             com.devoxx.genie.service.prompt.threading.PromptTask<com.devoxx.genie.service.prompt.result.PromptResult> resultTask) {
            // No implementation needed for test
        }
        
        @Override
        protected String getStrategyName() {
            return "Test Strategy";
        }
        
        @Override
        public void cancel() {
            // No implementation needed for test
        }
        
        /**
         * Create a constructor that doesn't rely on ApplicationManager for tests
         */
        public TestPromptExecutionStrategy(Project project, 
                                          ChatMemoryManager chatMemoryManager,
                                          ThreadPoolManager threadPoolManager) {
            super(project, chatMemoryManager, threadPoolManager);
        }
        
        /**
         * Create a constructor that also passes MessageCreationService for tests
         */
        public TestPromptExecutionStrategy(Project project, 
                                          ChatMemoryManager chatMemoryManager,
                                          ThreadPoolManager threadPoolManager,
                                          MessageCreationService messageCreationService) {
            super(project, chatMemoryManager, threadPoolManager, messageCreationService);
        }
    }
}
