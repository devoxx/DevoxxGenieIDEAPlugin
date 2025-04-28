package com.devoxx.genie.service.prompt;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.cancellation.PromptCancellationService;
import com.devoxx.genie.service.prompt.command.PromptCommandProcessor;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.strategy.PromptExecutionStrategy;
import com.devoxx.genie.service.prompt.strategy.PromptExecutionStrategyFactory;
import com.devoxx.genie.service.prompt.threading.PromptTask;
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

import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PromptExecutionServiceTest extends LightPlatformTestCase {

    @Mock
    private Project project;
    
    @Mock
    private PromptCommandProcessor commandProcessor;
    
    @Mock
    private PromptExecutionStrategyFactory strategyFactory;
    
    @Mock
    private PromptCancellationService cancellationService;
    
    @Mock
    private PromptOutputPanel panel;
    
    @Mock
    private ChatMessageContext context;
    
    @Mock
    private PromptExecutionStrategy strategy;
    
    @Mock
    private PromptTask<PromptResult> promptTask;
    
    @Mock
    private Runnable enableButtons;
    
    @Mock
    private FileListManager fileListManager;
    
    @Mock
    private ChatMemoryManager chatMemoryManager;
    
    private PromptExecutionService service;
    
    private CompletableFuture<PromptResult> future;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        // Set up default behaviors
        when(context.getProject()).thenReturn(project);
        when(context.getId()).thenReturn("test-context-id");
        
        // Mock static instances
        try (MockedStatic<PromptCommandProcessor> commandProcessorMockedStatic = Mockito.mockStatic(PromptCommandProcessor.class);
             MockedStatic<PromptExecutionStrategyFactory> strategyFactoryMockedStatic = Mockito.mockStatic(PromptExecutionStrategyFactory.class);
             MockedStatic<PromptCancellationService> cancellationServiceMockedStatic = Mockito.mockStatic(PromptCancellationService.class);
             MockedStatic<FileListManager> fileListManagerMockedStatic = Mockito.mockStatic(FileListManager.class);
             MockedStatic<ChatMemoryManager> chatMemoryManagerMockedStatic = Mockito.mockStatic(ChatMemoryManager.class)) {
            
            commandProcessorMockedStatic.when(PromptCommandProcessor::getInstance).thenReturn(commandProcessor);
            strategyFactoryMockedStatic.when(PromptExecutionStrategyFactory::getInstance).thenReturn(strategyFactory);
            cancellationServiceMockedStatic.when(PromptCancellationService::getInstance).thenReturn(cancellationService);
            fileListManagerMockedStatic.when(FileListManager::getInstance).thenReturn(fileListManager);
            chatMemoryManagerMockedStatic.when(ChatMemoryManager::getInstance).thenReturn(chatMemoryManager);
            
            // Create service with mocked dependencies
            service = new PromptExecutionService(project);
        }
        
        // Set up strategy factory behavior
        when(strategyFactory.createStrategy(any(ChatMessageContext.class))).thenReturn(strategy);
        
        // Set up PromptTask behavior
        future = new CompletableFuture<>();
        when(strategy.execute(any(ChatMessageContext.class), any(PromptOutputPanel.class))).thenReturn(promptTask);
        when(promptTask.whenComplete(any())).thenReturn(promptTask);
        
        // Mock ApplicationManager for invokeLater
        try (MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class)) {
            Application mockApplication = Mockito.mock(Application.class);
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(mockApplication);
            
            doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(mockApplication).invokeLater(any());
        }
    }

    @Test
    public void testExecutePrompt_WithCommandProcessingSuccess() {
        // Set up command processor to return a processed prompt
        when(commandProcessor.processCommands(any(ChatMessageContext.class), any(PromptOutputPanel.class)))
            .thenReturn(Optional.of("Processed prompt"));
        
        // Set up cancellation service to not cancel any executions
        when(cancellationService.cancelAllExecutions(project)).thenReturn(0);
        
        // Execute the prompt
        service.executePrompt(context, panel, enableButtons);
        
        // Verify strategy was created and executed
        verify(strategyFactory).createStrategy(context);
        verify(strategy).execute(context, panel);
        
        // Verify registration with cancellation service
        verify(cancellationService).registerExecution(project, "test-context-id", strategy, panel);
    }
    
    @Test
    public void testExecutePrompt_WithCommandProcessingHalt() {
        // Set up command processor to signal execution should stop
        when(commandProcessor.processCommands(any(ChatMessageContext.class), any(PromptOutputPanel.class)))
            .thenReturn(Optional.empty());
        
        // Set up cancellation service to not cancel any executions
        when(cancellationService.cancelAllExecutions(project)).thenReturn(0);
        
        // Execute the prompt
        service.executePrompt(context, panel, enableButtons);
        
        // Verify strategy was not created or executed
        verify(strategyFactory, never()).createStrategy(context);
        verify(strategy, never()).execute(context, panel);
        
        // Verify buttons were enabled
        verify(enableButtons).run();
    }
    
    @Test
    public void testExecutePrompt_WithExistingExecution() {
        // Set up cancellation service to indicate it cancelled existing executions
        when(cancellationService.cancelAllExecutions(project)).thenReturn(1);
        
        // Execute the prompt
        service.executePrompt(context, panel, enableButtons);
        
        // Verify command processor was never called
        verify(commandProcessor, never()).processCommands(any(ChatMessageContext.class), any(PromptOutputPanel.class));
        
        // Verify strategy was not created or executed
        verify(strategyFactory, never()).createStrategy(context);
        verify(strategy, never()).execute(context, panel);
        
        // Verify buttons were enabled
        verify(enableButtons).run();
    }
    
    @Test
    public void testExecutePrompt_WithSuccessfulCompletion() {
        // Set up command processor to return a processed prompt
        when(commandProcessor.processCommands(any(ChatMessageContext.class), any(PromptOutputPanel.class)))
            .thenReturn(Optional.of("Processed prompt"));
        
        // Set up cancellation service to not cancel any executions
        when(cancellationService.cancelAllExecutions(project)).thenReturn(0);
        
        // Set up task to complete successfully
        PromptResult mockResult = Mockito.mock(PromptResult.class);
        
        // Set up task completion handling
        doAnswer(invocation -> {
            // Extract the BiConsumer from the whenComplete call and invoke it with a successful result
            ((java.util.function.BiConsumer<PromptResult, Throwable>) invocation.getArgument(0))
                .accept(mockResult, null);
            return promptTask;
        }).when(promptTask).whenComplete(any());
        
        // Execute the prompt
        service.executePrompt(context, panel, enableButtons);
        
        // Verify unregistration with cancellation service
        verify(cancellationService).unregisterExecution(project, "test-context-id");
        
        // Verify cleanup
        verify(enableButtons).run();
        verify(fileListManager).storeAddedFiles(project);
    }
    
    @Test
    public void testExecutePrompt_WithErrorCompletion() {
        // Set up command processor to return a processed prompt
        when(commandProcessor.processCommands(any(ChatMessageContext.class), any(PromptOutputPanel.class)))
            .thenReturn(Optional.of("Processed prompt"));
        
        // Set up cancellation service to not cancel any executions
        when(cancellationService.cancelAllExecutions(project)).thenReturn(0);
        
        // Set up task to complete with an error
        RuntimeException testException = new RuntimeException("Test exception");
        
        // Set up task completion handling
        doAnswer(invocation -> {
            // Extract the BiConsumer from the whenComplete call and invoke it with an error
            ((java.util.function.BiConsumer<PromptResult, Throwable>) invocation.getArgument(0))
                .accept(null, testException);
            return promptTask;
        }).when(promptTask).whenComplete(any());
        
        // Execute the prompt
        service.executePrompt(context, panel, enableButtons);
        
        // Verify unregistration with cancellation service
        verify(cancellationService).unregisterExecution(project, "test-context-id");
        
        // Verify cleanup
        verify(enableButtons).run();
        verify(fileListManager).storeAddedFiles(project);
    }
    
    @Test
    public void testExecutePrompt_WithCancellationException() {
        // Set up command processor to return a processed prompt
        when(commandProcessor.processCommands(any(ChatMessageContext.class), any(PromptOutputPanel.class)))
            .thenReturn(Optional.of("Processed prompt"));
        
        // Set up cancellation service to not cancel any executions
        when(cancellationService.cancelAllExecutions(project)).thenReturn(0);
        
        // Set up task to complete with a cancellation exception
        CancellationException cancellationException = new CancellationException("Cancelled");
        
        // Set up task completion handling
        doAnswer(invocation -> {
            // Extract the BiConsumer from the whenComplete call and invoke it with a cancellation exception
            ((java.util.function.BiConsumer<PromptResult, Throwable>) invocation.getArgument(0))
                .accept(null, cancellationException);
            return promptTask;
        }).when(promptTask).whenComplete(any());
        
        // Execute the prompt
        service.executePrompt(context, panel, enableButtons);
        
        // Verify unregistration with cancellation service
        verify(cancellationService).unregisterExecution(project, "test-context-id");
        
        // Verify cleanup
        verify(enableButtons).run();
        verify(fileListManager).storeAddedFiles(project);
    }
    
    @Test
    public void testStopExecution() {
        // Set up cancellation service to return a count of cancelled executions
        when(cancellationService.cancelAllExecutions(project)).thenReturn(2);
        
        // Call stop execution
        service.stopExecution(project);
        
        // Verify cancellation service was called
        verify(cancellationService).cancelAllExecutions(project);
    }
    
    @Test
    public void testCancelExecution() {
        // Call cancel execution
        service.cancelExecution("test-context-id");
        
        // Verify cancellation service was called with correct parameters
        verify(cancellationService).cancelExecution(project, "test-context-id");
    }
    
    @Test
    public void testHandleCancellation() {
        // Call handle cancellation
        service.handleCancellation(context, panel);
        
        // Verify memory manager and panel were updated
        verify(chatMemoryManager).removeLastUserMessage(context);
    }
}
