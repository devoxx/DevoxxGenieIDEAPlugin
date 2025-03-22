package com.devoxx.genie.service.prompt.cancellation;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.strategy.PromptExecutionStrategy;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.PromptTaskTracker;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightPlatformTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PromptCancellationServiceTest extends LightPlatformTestCase {

    @Mock
    private Project project;
    
    @Mock
    private PromptExecutionStrategy strategy1;
    
    @Mock
    private PromptExecutionStrategy strategy2;
    
    @Mock
    private PromptOutputPanel panel1;
    
    @Mock
    private PromptOutputPanel panel2;
    
    @Mock
    private PromptTaskTracker taskTracker;
    
    @Mock
    private ChatMemoryManager chatMemoryManager;
    
    @Mock
    private PromptTask<?> task1;
    
    @Mock
    private PromptTask<?> task2;
    
    @Mock
    private ChatMessageContext context1;
    
    @Mock
    private ChatMessageContext context2;
    
    @Mock
    private Application application;
    
    private PromptCancellationService service;
    
    private final String PROJECT_HASH = "project-hash";
    private final String CONTEXT_ID_1 = "context-id-1";
    private final String CONTEXT_ID_2 = "context-id-2";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        // Set up default behaviors
        when(project.getLocationHash()).thenReturn(PROJECT_HASH);
        
        // Mock static instances
        try (MockedStatic<PromptTaskTracker> taskTrackerMockedStatic = Mockito.mockStatic(PromptTaskTracker.class);
             MockedStatic<ChatMemoryManager> chatMemoryManagerMockedStatic = Mockito.mockStatic(ChatMemoryManager.class);
             MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class)) {
            
            taskTrackerMockedStatic.when(PromptTaskTracker::getInstance).thenReturn(taskTracker);
            chatMemoryManagerMockedStatic.when(ChatMemoryManager::getInstance).thenReturn(chatMemoryManager);
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
            
            when(application.getService(PromptCancellationService.class)).thenReturn(null);
            
            // Create service
            service = new PromptCancellationService();
        }
        
        // Create custom matcher for the getTaskByContextId method
        when(taskTracker.getTaskByContextId(eq(project), eq(CONTEXT_ID_1))).thenAnswer(invocation -> task1);
        when(taskTracker.getTaskByContextId(eq(project), eq(CONTEXT_ID_2))).thenAnswer(invocation -> task2);
        
        // Set up task context data
        when(task1.getUserData(PromptTask.CONTEXT_KEY)).thenReturn(context1);
        when(task2.getUserData(PromptTask.CONTEXT_KEY)).thenReturn(context2);
        
        // Set up Application.invokeLater behavior
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(application).invokeLater(any(Runnable.class));
    }

    @Test
    public void testRegisterAndUnregisterExecution() {
        // Register executions
        service.registerExecution(project, CONTEXT_ID_1, strategy1, panel1);
        service.registerExecution(project, CONTEXT_ID_2, strategy2, panel2);
        
        // Test cancellation to verify registrations worked
        when(taskTracker.cancelAllTasks(project)).thenReturn(0);
        
        int count = service.cancelAllExecutions(project);
        
        // Verify both strategies were cancelled
        verify(strategy1).cancel();
        verify(strategy2).cancel();
        assertEquals(2, count);
        
        // Unregister one execution
        service.unregisterExecution(project, CONTEXT_ID_1);
        
        // Reset mocks for another test
        Mockito.reset(strategy1, strategy2);
        when(taskTracker.cancelAllTasks(project)).thenReturn(0);
        
        // Now only one strategy should be cancelled
        count = service.cancelAllExecutions(project);
        
        verify(strategy1, never()).cancel();
        verify(strategy2).cancel();
        assertEquals(1, count);
        
        // Unregister the other execution
        service.unregisterExecution(project, CONTEXT_ID_2);
        
        // Reset mocks for another test
        Mockito.reset(strategy1, strategy2);
        when(taskTracker.cancelAllTasks(project)).thenReturn(0);
        
        // Now no strategies should be cancelled
        count = service.cancelAllExecutions(project);
        
        verify(strategy1, never()).cancel();
        verify(strategy2, never()).cancel();
        assertEquals(0, count);
    }
    
    @Test
    public void testCancelExecution() {
        // Register executions
        service.registerExecution(project, CONTEXT_ID_1, strategy1, panel1);
        service.registerExecution(project, CONTEXT_ID_2, strategy2, panel2);
        
        // Cancel one execution
        service.cancelExecution(project, CONTEXT_ID_1);
        
        // Verify the strategy was cancelled
        verify(strategy1).cancel();
        verify(strategy2, never()).cancel();
        
        // Verify the task was cancelled
        verify(taskTracker).cancelTaskByContextId(project, CONTEXT_ID_1);
        
        // Verify memory cleanup
        verify(chatMemoryManager).removeLastUserMessage(context1);
        
        // Verify panel update
        verify(panel1).removeLastUserPrompt(context1);
        
        // Verify unregistration
        Mockito.reset(strategy1, strategy2);
        when(taskTracker.cancelAllTasks(project)).thenReturn(0);
        
        // Only strategy2 should still be active
        int count = service.cancelAllExecutions(project);
        
        verify(strategy1, never()).cancel();
        verify(strategy2).cancel();
        assertEquals(1, count);
    }
    
    @Test
    public void testCancelAllExecutions() {
        // Register executions
        service.registerExecution(project, CONTEXT_ID_1, strategy1, panel1);
        service.registerExecution(project, CONTEXT_ID_2, strategy2, panel2);
        
        // Mock taskTracker to return 1 additional cancellation
        when(taskTracker.cancelAllTasks(project)).thenReturn(1);
        
        // Cancel all executions
        int count = service.cancelAllExecutions(project);
        
        // Verify both strategies were cancelled
        verify(strategy1).cancel();
        verify(strategy2).cancel();
        
        // Verify taskTracker was called
        verify(taskTracker).cancelAllTasks(project);
        
        // Verify memory cleanup
        verify(chatMemoryManager).removeLastUserMessage(context1);
        verify(chatMemoryManager).removeLastUserMessage(context2);
        
        // Verify panel updates
        verify(panel1).removeLastUserPrompt(context1);
        verify(panel2).removeLastUserPrompt(context2);
        
        // Verify count (2 from our service + 1 from taskTracker)
        assertEquals(3, count);
        
        // Verify unregistration
        Mockito.reset(strategy1, strategy2);
        when(taskTracker.cancelAllTasks(project)).thenReturn(0);
        
        // Now no strategies should be cancelled
        count = service.cancelAllExecutions(project);
        
        verify(strategy1, never()).cancel();
        verify(strategy2, never()).cancel();
        assertEquals(0, count);
    }
    
    @Test
    public void testCleanupCancelledExecution_WithNullPanel() {
        // Register execution with strategy only
        service.registerExecution(project, CONTEXT_ID_1, strategy1, panel1);
        
        // Set up taskTracker to return a task without panel
        when(taskTracker.getTaskByContextId(eq(project), eq(CONTEXT_ID_1))).thenAnswer(invocation -> task1);
        
        // Cancel the execution
        service.cancelExecution(project, CONTEXT_ID_1);
        
        // Verify memory cleanup
        verify(chatMemoryManager).removeLastUserMessage(context1);
        
        // Verify panel update
        verify(panel1).removeLastUserPrompt(context1);
    }
    
    @Test
    public void testCleanupCancelledExecution_WithNullContext() {
        // Register execution
        service.registerExecution(project, CONTEXT_ID_1, strategy1, panel1);
        
        // Set up task with null context
        when(task1.getUserData(PromptTask.CONTEXT_KEY)).thenReturn(null);
        
        // Cancel the execution
        service.cancelExecution(project, CONTEXT_ID_1);
        
        // Verify memory cleanup was not called
        verify(chatMemoryManager, never()).removeLastUserMessage(any());
        
        // Verify panel update was not called
        verify(panel1, never()).removeLastUserPrompt(any());
    }
}
