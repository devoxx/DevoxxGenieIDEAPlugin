package com.devoxx.genie.service.prompt.error;

import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.exception.ModelNotActiveException;
import com.devoxx.genie.service.exception.ProviderUnavailableException;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.ui.util.NotificationUtil;
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

import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PromptErrorHandlerTest extends LightPlatformTestCase {

    @Mock
    private Project project;
    
    @Mock
    private ChatMessageContext context;
    
    @Mock
    private ChatMemoryManager chatMemoryManager;
    
    @Mock
    private Application application;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        // Set up ApplicationManager
        try (MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class)) {
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
            
            // Set up invokeLater to run immediately
            doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(application).invokeLater(any(Runnable.class));
        }
    }

    @Test
    public void testHandleException_WithPromptException() {
        // Create a PromptException
        PromptException exception = new ExecutionException(
                "Test error", 
                new RuntimeException("Original error"), 
                PromptException.ErrorSeverity.ERROR, 
                true);
        
        try (MockedStatic<ErrorHandler> errorHandlerMockedStatic = Mockito.mockStatic(ErrorHandler.class);
             MockedStatic<NotificationUtil> notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class);
             MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class)) {
            
            // Set up statics
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
            
            // Handle the exception
            PromptErrorHandler.handleException(project, exception, context);
            
            // Verify ErrorHandler was called
            errorHandlerMockedStatic.verify(() -> 
                ErrorHandler.handleError(eq(project), eq(exception)));
            
            // Verify notification was shown
            notificationUtilMockedStatic.verify(() -> 
                NotificationUtil.sendNotification(eq(project), anyString()));
        }
    }
    
    @Test
    public void testHandleException_WithModelNotActiveException() {
        // Create a ModelNotActiveException
        ModelNotActiveException exception = new ModelNotActiveException("Test model not active");
        
        try (MockedStatic<ErrorHandler> errorHandlerMockedStatic = Mockito.mockStatic(ErrorHandler.class);
             MockedStatic<NotificationUtil> notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class);
             MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class)) {
            
            // Set up statics
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
            
            // Handle the exception
            PromptErrorHandler.handleException(project, exception, context);
            
            // Verify ErrorHandler was called with a ModelException
            errorHandlerMockedStatic.verify(() -> 
                ErrorHandler.handleError(eq(project), any(ModelException.class)));
            
            // Verify notification was shown
            notificationUtilMockedStatic.verify(() -> 
                NotificationUtil.sendNotification(eq(project), anyString()));
        }
    }
    
    @Test
    public void testHandleException_WithProviderUnavailableException() {
        // Create a ProviderUnavailableException
        ProviderUnavailableException exception = new ProviderUnavailableException("Test provider unavailable");
        
        try (MockedStatic<ErrorHandler> errorHandlerMockedStatic = Mockito.mockStatic(ErrorHandler.class);
             MockedStatic<NotificationUtil> notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class);
             MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class)) {
            
            // Set up statics
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
            
            // Handle the exception
            PromptErrorHandler.handleException(project, exception, context);
            
            // Verify ErrorHandler was called with a ModelException
            errorHandlerMockedStatic.verify(() -> 
                ErrorHandler.handleError(eq(project), any(ModelException.class)));
            
            // Verify notification was shown
            notificationUtilMockedStatic.verify(() -> 
                NotificationUtil.sendNotification(eq(project), anyString()));
        }
    }
    
    @Test
    public void testHandleException_WithTimeoutException() {
        // Create a TimeoutException
        TimeoutException exception = new TimeoutException("Test timeout");
        
        try (MockedStatic<ErrorHandler> errorHandlerMockedStatic = Mockito.mockStatic(ErrorHandler.class);
             MockedStatic<NotificationUtil> notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class);
             MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class)) {
            
            // Set up statics
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
            
            // Handle the exception
            PromptErrorHandler.handleException(project, exception, context);
            
            // Verify ErrorHandler was called with an ExecutionException
            errorHandlerMockedStatic.verify(() -> 
                ErrorHandler.handleError(eq(project), any(ExecutionException.class)));
            
            // Verify notification was shown
            notificationUtilMockedStatic.verify(() -> 
                NotificationUtil.sendNotification(eq(project), anyString()));
        }
    }
    
    @Test
    public void testHandleException_WithGenericException() {
        // Create a generic exception
        RuntimeException exception = new RuntimeException("Test generic error");
        
        try (MockedStatic<ErrorHandler> errorHandlerMockedStatic = Mockito.mockStatic(ErrorHandler.class);
             MockedStatic<NotificationUtil> notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class);
             MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class)) {
            
            // Set up statics
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
            
            // Handle the exception
            PromptErrorHandler.handleException(project, exception, context);
            
            // Verify ErrorHandler was called with an ExecutionException
            errorHandlerMockedStatic.verify(() -> 
                ErrorHandler.handleError(eq(project), any(ExecutionException.class)));
            
            // Verify notification was shown
            notificationUtilMockedStatic.verify(() -> 
                NotificationUtil.sendNotification(eq(project), anyString()));
        }
    }
    
    @Test
    public void testHandleException_WithMemoryException_AndContext() {
        // Create a MemoryException
        MemoryException exception = new MemoryException("Test memory error");
        
        try (MockedStatic<ErrorHandler> errorHandlerMockedStatic = Mockito.mockStatic(ErrorHandler.class);
             MockedStatic<NotificationUtil> notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class);
             MockedStatic<ChatMemoryManager> chatMemoryManagerMockedStatic = Mockito.mockStatic(ChatMemoryManager.class);
             MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class)) {
            
            // Set up statics
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
            chatMemoryManagerMockedStatic.when(ChatMemoryManager::getInstance).thenReturn(chatMemoryManager);
            
            // Handle the exception
            PromptErrorHandler.handleException(project, exception, context);
            
            // Verify ErrorHandler was called
            errorHandlerMockedStatic.verify(() -> 
                ErrorHandler.handleError(eq(project), eq(exception)));
            
            // Verify notification was shown
            notificationUtilMockedStatic.verify(() -> 
                NotificationUtil.sendNotification(eq(project), anyString()));
            
            // Verify recovery action was performed
            verify(chatMemoryManager).removeLastExchange(context);
        }
    }
    
    @Test
    public void testHandleException_WithNonVisibleException() {
        // Create a non-visible execution exception
        ExecutionException exception = new ExecutionException(
                "Test non-visible error", 
                new RuntimeException("Original error"), 
                PromptException.ErrorSeverity.INFO, 
                false);
        
        try (MockedStatic<ErrorHandler> errorHandlerMockedStatic = Mockito.mockStatic(ErrorHandler.class);
             MockedStatic<NotificationUtil> notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class);
             MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class)) {
            
            // Set up statics
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
            
            // Handle the exception
            PromptErrorHandler.handleException(project, exception, context);
            
            // Verify ErrorHandler was called
            errorHandlerMockedStatic.verify(() -> 
                ErrorHandler.handleError(eq(project), eq(exception)));
            
            // Verify notification was not shown
            notificationUtilMockedStatic.verify(() -> 
                NotificationUtil.sendNotification(any(), anyString()), 
                never());
        }
    }
    
    @Test
    public void testHandleException_WithoutContext() {
        // Create an exception
        ExecutionException exception = new ExecutionException("Test error without context");
        
        try (MockedStatic<ErrorHandler> errorHandlerMockedStatic = Mockito.mockStatic(ErrorHandler.class);
             MockedStatic<NotificationUtil> notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class);
             MockedStatic<ChatMemoryManager> chatMemoryManagerMockedStatic = Mockito.mockStatic(ChatMemoryManager.class);
             MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class)) {
            
            // Set up statics
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
            chatMemoryManagerMockedStatic.when(ChatMemoryManager::getInstance).thenReturn(chatMemoryManager);
            
            // Handle the exception without context
            PromptErrorHandler.handleException(project, exception);
            
            // Verify ErrorHandler was called
            errorHandlerMockedStatic.verify(() -> 
                ErrorHandler.handleError(eq(project), eq(exception)));
            
            // Verify no recovery actions were performed
            verify(chatMemoryManager, never()).removeLastExchange(any());
        }
    }
}
