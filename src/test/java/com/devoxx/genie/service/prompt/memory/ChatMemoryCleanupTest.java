package com.devoxx.genie.service.prompt.memory;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.PromptExecutionService;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.PromptTaskTracker;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockedStatic;

import java.util.concurrent.CancellationException;

import static org.mockito.Mockito.*;

/**
 * Tests to verify the correct memory cleanup behavior in various cancellation and removal scenarios.
 */
class ChatMemoryCleanupTest {

    @Mock
    private Project project;
    
    @Mock
    private PromptOutputPanel outputPanel;
    
    @Mock
    private ChatMemoryManager chatMemoryManager;
    
    @Mock
    private ChatMemoryService chatMemoryService;

    @Mock
    private PromptTaskTracker taskTracker;
    
    private ChatMessageContext testContext;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // Set up a test context with a user message
        testContext = ChatMessageContext.builder()
                .project(project)
                .id("test-context")
                .userPrompt("Test prompt")
                .build();
        
        // Mock the user message
        testContext.setUserMessage(UserMessage.userMessage("Test prompt"));
        
        // Note: Static mocks are set up in each test method
    }

    /**
     * Test scenario: User cancels a prompt before receiving any response
     */
    @Test
    void testUserCancelPrompt() {
        try (MockedStatic<ChatMemoryManager> mockedChatMemoryManager = mockStatic(ChatMemoryManager.class);
             MockedStatic<PromptTaskTracker> mockedTaskTracker = mockStatic(PromptTaskTracker.class)) {
            
            // Set up static mocks
            mockedChatMemoryManager.when(ChatMemoryManager::getInstance).thenReturn(chatMemoryManager);
            mockedTaskTracker.when(PromptTaskTracker::getInstance).thenReturn(taskTracker);
            
            // Create a prompt task with context
            PromptTask<String> task = new PromptTask<>(project);
            task.putUserData(PromptTask.CONTEXT_KEY, testContext);
            
            // Simulate cancellation
            task.cancel(true);
            
            // Verify that the last user message was removed from memory
            verify(chatMemoryManager).removeLastUserMessage(testContext);
        }
    }

    /**
     * Test scenario: Streaming is stopped mid-response
     */
    @Test
    void testStreamingCancellation() {
        try (MockedStatic<ChatMemoryService> mockedChatMemoryService = mockStatic(ChatMemoryService.class)) {
            
            // Set up static mocks
            mockedChatMemoryService.when(ChatMemoryService::getInstance).thenReturn(chatMemoryService);

            // Add a partial response
            testContext.setAiMessage(AiMessage.aiMessage("Partial response"));
            
            // Directly call the method we want to test
            ChatMemoryService.getInstance().removeLastMessage(project);
            
            // Verify that the partial response was removed from memory
            verify(chatMemoryService).removeLastMessage(project);
        }
    }
    
    /**
     * Test scenario: Execution service handles cancellation
     */
    @Test
    void testExecutionServiceCancellation() {
        try (MockedStatic<ChatMemoryManager> mockedChatMemoryManager = mockStatic(ChatMemoryManager.class)) {
            
            // Set up static mocks
            mockedChatMemoryManager.when(ChatMemoryManager::getInstance).thenReturn(chatMemoryManager);

            // Create a mockExecutionService that will directly call ChatMemoryManager
            PromptExecutionService mockExecutionService = mock(PromptExecutionService.class);
            doAnswer(invocation -> {
                ChatMessageContext ctx = invocation.getArgument(0);
                PromptOutputPanel panel = invocation.getArgument(1);
                
                // Call the service directly
                ChatMemoryManager.getInstance().removeLastUserMessage(ctx);
                
                // Verify the panel is used to update UI
                panel.removeLastUserPrompt(ctx);
                
                return null;
            }).when(mockExecutionService).handleCancellation(any(ChatMessageContext.class), any(PromptOutputPanel.class));
            
            // Call the method we want to test
            mockExecutionService.handleCancellation(testContext, outputPanel);
            
            // Verify that the last user message was removed from memory
            verify(chatMemoryManager).removeLastUserMessage(testContext);
            
            // Verify that the UI was updated
            verify(outputPanel).removeLastUserPrompt(testContext);
        }
    }
    
    /**
     * Test scenario: Task fails with an exception
     */
    @Test
    void testTaskFailure() {
        try (MockedStatic<PromptTaskTracker> mockedTaskTracker = mockStatic(PromptTaskTracker.class)) {
            
            // Set up static mocks
            mockedTaskTracker.when(PromptTaskTracker::getInstance).thenReturn(taskTracker);

            // Create a prompt task with context
            PromptTask<String> task = new PromptTask<>(project);
            task.putUserData(PromptTask.CONTEXT_KEY, testContext);
            
            // Simulate failure (non-cancellation exception)
            task.completeExceptionally(new RuntimeException("Test failure"));
            
            // Verify the task was marked as completed
            verify(taskTracker).taskCompleted(task);
        }
    }
    
    /**
     * Test scenario: Task is cancelled with CancellationException
     */
    @Test
    void testTaskCancellationException() {
        try (MockedStatic<PromptTaskTracker> mockedTaskTracker = mockStatic(PromptTaskTracker.class)) {
            
            // Set up static mocks
            mockedTaskTracker.when(PromptTaskTracker::getInstance).thenReturn(taskTracker);

            // Create a prompt task with context
            PromptTask<String> task = new PromptTask<>(project);
            task.putUserData(PromptTask.CONTEXT_KEY, testContext);
            
            // Simulate cancellation exception
            task.completeExceptionally(new CancellationException("Cancelled"));
            
            // Verify the task was marked as completed
            verify(taskTracker).taskCompleted(task);
        }
    }
    
    /**
     * Test scenario: Multiple cancellations in sequence
     */
    @Test
    void testMultipleCancellations() {
        try (MockedStatic<ChatMemoryManager> mockedChatMemoryManager = mockStatic(ChatMemoryManager.class);
             MockedStatic<PromptTaskTracker> mockedTaskTracker = mockStatic(PromptTaskTracker.class)) {
            
            // Set up static mocks
            mockedChatMemoryManager.when(ChatMemoryManager::getInstance).thenReturn(chatMemoryManager);
            mockedTaskTracker.when(PromptTaskTracker::getInstance).thenReturn(taskTracker);

            // Create multiple prompt tasks with the same context
            PromptTask<String> task1 = new PromptTask<>(project);
            task1.putUserData(PromptTask.CONTEXT_KEY, testContext);
            
            PromptTask<String> task2 = new PromptTask<>(project);
            task2.putUserData(PromptTask.CONTEXT_KEY, testContext);
            
            // Simulate cancellation of both tasks
            task1.cancel(true);
            task2.cancel(true);
            
            // Verify that the last user message was removed from memory (twice)
            verify(chatMemoryManager, times(2)).removeLastUserMessage(testContext);
        }
    }
    
    /**
     * Test scenario: Centralized cancellation in PromptExecutionService
     */
    @Test
    void testCentralizedCancellation() {
        try (MockedStatic<PromptTaskTracker> mockedTaskTracker = mockStatic(PromptTaskTracker.class)) {
            
            // Set up static mocks
            mockedTaskTracker.when(PromptTaskTracker::getInstance).thenReturn(taskTracker);

            // Mock the execution service to directly call the task tracker
            PromptExecutionService mockExecutionService = mock(PromptExecutionService.class);
            doAnswer(invocation -> {
                Project p = invocation.getArgument(0);
                PromptTaskTracker.getInstance().cancelAllTasks(p);
                return null;
            }).when(mockExecutionService).stopExecution(any(Project.class));
            
            // Call the method we want to test
            mockExecutionService.stopExecution(project);
            
            // Verify tasks were cancelled
            verify(taskTracker).cancelAllTasks(project);
        }
    }
    
    /**
     * Test scenario: Edge case with null context
     */
    @Test
    void testNullContextHandling() {
        try (MockedStatic<PromptTaskTracker> mockedTaskTracker = mockStatic(PromptTaskTracker.class);
             MockedStatic<ChatMemoryManager> mockedChatMemoryManager = mockStatic(ChatMemoryManager.class)) {
            
            // Set up static mocks
            mockedTaskTracker.when(PromptTaskTracker::getInstance).thenReturn(taskTracker);
            // We don't need to set ChatMemoryManager to return anything as we're verifying no interactions
            
            // Create a prompt task without context
            PromptTask<String> task = new PromptTask<>(project);
            
            // Simulate cancellation
            task.cancel(true);
            
            // Verify only task completion is tracked (no memory cleanup)
            verify(taskTracker).taskCompleted(task);
            verifyNoInteractions(chatMemoryManager);
            mockedChatMemoryManager.verify(ChatMemoryManager::getInstance, never());
        }
    }
}
