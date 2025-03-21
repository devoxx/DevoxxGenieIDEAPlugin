package com.devoxx.genie.service.prompt.memory;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.PromptExecutionService;
import com.devoxx.genie.service.prompt.streaming.StreamingResponseHandler;
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

import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

/**
 * Tests to verify the correct memory cleanup behavior in various cancellation and removal scenarios.
 */
public class ChatMemoryCleanupTest {

    @Mock
    private Project project;
    
    @Mock
    private PromptOutputPanel outputPanel;
    
    @Mock
    private ChatMemoryManager chatMemoryManager;
    
    @Mock
    private ChatMemoryService chatMemoryService;
    
    @Mock
    private PromptExecutionService executionService;
    
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
        
        // Set up mocks for static methods
        when(ChatMemoryManager.getInstance()).thenReturn(chatMemoryManager);
        when(ChatMemoryService.getInstance()).thenReturn(chatMemoryService);
        when(PromptTaskTracker.getInstance()).thenReturn(taskTracker);
    }

    /**
     * Test scenario: User cancels a prompt before receiving any response
     */
    @Test
    public void testUserCancelPrompt() {
        // Create a prompt task with context
        PromptTask<String> task = new PromptTask<>(project);
        task.putUserData(PromptTask.CONTEXT_KEY, testContext);
        
        // Simulate cancellation
        task.cancel(true);
        
        // Verify that the last user message was removed from memory
        verify(chatMemoryManager).removeLastUserMessage(testContext);
    }
    
    /**
     * Test scenario: User removes a message from the UI
     */
    @Test
    public void testUserRemovesUserMessage() {
        // Simulate removing a user message
        outputPanel.removeConversationItem(testContext, true);
        
        // Verify that the last exchange was removed from memory
        verify(chatMemoryManager).removeLastExchange(testContext);
    }
    
    /**
     * Test scenario: User removes an AI response from the UI
     */
    @Test
    public void testUserRemovesAIResponse() {
        // Add an AI message to the context
        testContext.setAiMessage(AiMessage.aiMessage("Test response"));
        
        // Simulate removing an AI response
        outputPanel.removeConversationItem(testContext, false);
        
        // Verify that only the AI message was removed from memory
        verify(chatMemoryManager).removeLastAIMessage(testContext);
    }
    
    /**
     * Test scenario: Streaming is stopped mid-response
     */
    @Test
    public void testStreamingCancellation() {
        // Create a streaming response handler
        Consumer<Object> mockConsumer = mock(Consumer.class);
        StreamingResponseHandler handler = new StreamingResponseHandler(
            testContext, 
            outputPanel,
            mockConsumer,
            mockConsumer
        );
        
        // Add a partial response
        testContext.setAiMessage(AiMessage.aiMessage("Partial response"));
        
        // Simulate stopping the streaming
        handler.stop();
        
        // Verify that the partial response was removed from memory
        verify(chatMemoryService).removeLastMessage(project);
    }
    
    /**
     * Test scenario: Execution service handles cancellation
     */
    @Test
    public void testExecutionServiceCancellation() {
        // Simulate execution service handling cancellation
        executionService.handleCancellation(testContext, outputPanel);
        
        // Verify that the last user message was removed from memory
        verify(chatMemoryManager).removeLastUserMessage(testContext);
        
        // Verify that the UI was updated
        verify(outputPanel).removeLastUserPrompt(testContext);
    }
    
    /**
     * Test scenario: Task fails with an exception
     */
    @Test
    public void testTaskFailure() {
        // Create a prompt task with context
        PromptTask<String> task = new PromptTask<>(project);
        task.putUserData(PromptTask.CONTEXT_KEY, testContext);
        
        // Simulate failure (non-cancellation exception)
        task.completeExceptionally(new RuntimeException("Test failure"));
        
        // Verify the task was marked as completed
        verify(taskTracker).taskCompleted(task);
    }
    
    /**
     * Test scenario: Task is cancelled with CancellationException
     */
    @Test
    public void testTaskCancellationException() {
        // Create a prompt task with context
        PromptTask<String> task = new PromptTask<>(project);
        task.putUserData(PromptTask.CONTEXT_KEY, testContext);
        
        // Simulate cancellation exception
        task.completeExceptionally(new CancellationException("Cancelled"));
        
        // Verify the task was marked as completed
        verify(taskTracker).taskCompleted(task);
    }
    
    /**
     * Test scenario: Multiple cancellations in sequence
     */
    @Test
    public void testMultipleCancellations() {
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
    
    /**
     * Test scenario: Centralized cancellation in PromptExecutionService
     */
    @Test
    public void testCentralizedCancellation() {
        // Simulate cancellation via the execution service
        executionService.stopExecution(project);
        
        // Verify tasks were cancelled
        verify(taskTracker).cancelAllTasks(project);
    }
    
    /**
     * Test scenario: Edge case with null context
     */
    @Test
    public void testNullContextHandling() {
        // Create a prompt task without context
        PromptTask<String> task = new PromptTask<>(project);
        
        // Simulate cancellation
        task.cancel(true);
        
        // Verify only task completion is tracked (no memory cleanup)
        verify(taskTracker).taskCompleted(task);
        verifyNoInteractions(chatMemoryManager);
    }
}
