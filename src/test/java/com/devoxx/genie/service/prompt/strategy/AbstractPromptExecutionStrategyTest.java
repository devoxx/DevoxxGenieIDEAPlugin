package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.error.ExecutionException;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightPlatformTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.CancellationException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbstractPromptExecutionStrategyTest extends LightPlatformTestCase {

    @Mock
    private Project project;
    
    @Mock
    private ChatMessageContext context;
    
    @Mock
    private PromptOutputPanel panel;
    
    @Mock
    private ChatMemoryManager chatMemoryManager;
    
    @Mock
    private ThreadPoolManager threadPoolManager;
    
    @Mock
    private PromptTask<PromptResult> resultTask;
    
    private TestPromptExecutionStrategy strategy;

    /**
     * Concrete implementation of AbstractPromptExecutionStrategy for testing.
     */
    private class TestPromptExecutionStrategy extends AbstractPromptExecutionStrategy {
        private boolean executeSpecificCalled = false;
        private boolean throwErrorInExecuteSpecific = false;
        private boolean throwCancellationInExecuteSpecific = false;
        
        public TestPromptExecutionStrategy(Project project) {
            super(project);
        }
        
        @Override
        protected void executeStrategySpecific(
                ChatMessageContext context,
                PromptOutputPanel panel,
                PromptTask<PromptResult> resultTask) {
            executeSpecificCalled = true;
            if (throwErrorInExecuteSpecific) {
                throw new RuntimeException("Test exception");
            }
            if (throwCancellationInExecuteSpecific) {
                throw new CancellationException("Test cancellation");
            }
        }
        
        @Override
        protected String getStrategyName() {
            return "TestStrategy";
        }
        
        public boolean wasExecuteSpecificCalled() {
            return executeSpecificCalled;
        }
        
        public void setThrowErrorInExecuteSpecific(boolean throwError) {
            this.throwErrorInExecuteSpecific = throwError;
        }
        
        public void setThrowCancellationInExecuteSpecific(boolean throwCancellation) {
            this.throwCancellationInExecuteSpecific = throwCancellation;
        }
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        // Set up default behaviors
        when(context.getProject()).thenReturn(project);
        when(context.getId()).thenReturn("test-context-id");
        
        // Set up mock static instances
        try (MockedStatic<ChatMemoryManager> chatMemoryManagerMockedStatic = Mockito.mockStatic(ChatMemoryManager.class);
             MockedStatic<ThreadPoolManager> threadPoolManagerMockedStatic = Mockito.mockStatic(ThreadPoolManager.class)) {
            
            chatMemoryManagerMockedStatic.when(ChatMemoryManager::getInstance).thenReturn(chatMemoryManager);
            threadPoolManagerMockedStatic.when(ThreadPoolManager::getInstance).thenReturn(threadPoolManager);
            
            // Create strategy with mocked dependencies
            strategy = new TestPromptExecutionStrategy(project);
        }
        
        // Set up PromptTask.whenComplete behavior
        doAnswer(invocation -> {
            // Just return the task itself
            return resultTask;
        }).when(resultTask).whenComplete(any());
        
        // Set up PromptTask.putUserData behavior
        doAnswer(invocation -> {
            // Do nothing
            return null;
        }).when(resultTask).putUserData(any(), any());
        
        // Stub PromptTask
        when(resultTask.isCancelled()).thenReturn(false);
    }

    @Test
    public void testExecute_Success() {
        // Execute the strategy
        PromptTask<PromptResult> task = strategy.execute(context, panel);
        
        // Verify task has context data
        verify(resultTask).putUserData(PromptTask.CONTEXT_KEY, context);
        
        // Verify panel was updated
        verify(panel).addUserPrompt(context);
        
        // Verify strategy-specific execution
        assertTrue(strategy.wasExecuteSpecificCalled());
        
        // Verify task completion handler was added
        verify(resultTask).whenComplete(any());
    }
    
    @Test
    public void testExecute_WithError() {
        // Set up strategy to throw an error
        strategy.setThrowErrorInExecuteSpecific(true);
        
        try (MockedStatic<PromptErrorHandler> errorHandlerMockedStatic = Mockito.mockStatic(PromptErrorHandler.class)) {
            // Execute the strategy
            PromptTask<PromptResult> task = strategy.execute(context, panel);
            
            // Verify error handling
            errorHandlerMockedStatic.verify(() -> 
                PromptErrorHandler.handleException(eq(project), any(ExecutionException.class), eq(context)));
            
            // Verify failure result was completed
            verify(resultTask).complete(any(PromptResult.class));
        }
    }
    
    @Test
    public void testExecute_WithCancellation() {
        // Set up strategy to throw a cancellation
        strategy.setThrowCancellationInExecuteSpecific(true);
        
        // Execute the strategy
        PromptTask<PromptResult> task = strategy.execute(context, panel);
        
        // Verify cancellation handling
        verify(resultTask).cancel(true);
        
        // Verify error handler was not called
        try (MockedStatic<PromptErrorHandler> errorHandlerMockedStatic = Mockito.mockStatic(PromptErrorHandler.class)) {
            errorHandlerMockedStatic.verify(() -> 
                PromptErrorHandler.handleException(any(), any(), any()), 
                never());
        }
    }
    
    @Test
    public void testHandleTaskCompletion_WithCancellation() {
        // Set up task as cancelled
        when(resultTask.isCancelled()).thenReturn(true);
        
        // Call handleTaskCompletion directly
        strategy.handleTaskCompletion(resultTask, context, panel);
        
        // Extract and call the completion handler
        doAnswer(invocation -> {
            // Extract the BiConsumer from the whenComplete call and invoke it
            ((java.util.function.BiConsumer<PromptResult, Throwable>) invocation.getArgument(0))
                .accept(null, null);
            return resultTask;
        }).when(resultTask).whenComplete(any());
        
        // Execute the strategy to trigger the completion handler
        strategy.execute(context, panel);
        
        // Verify cancellation cleanup
        verify(panel).removeLastUserPrompt(context);
        verify(chatMemoryManager).removeLastUserMessage(context);
    }
    
    @Test
    public void testPrepareMemory() {
        // Call prepareMemory
        strategy.prepareMemory(context);
        
        // Verify memory manager calls
        verify(chatMemoryManager).prepareMemory(context);
        verify(chatMemoryManager).addUserMessage(context);
    }
}
