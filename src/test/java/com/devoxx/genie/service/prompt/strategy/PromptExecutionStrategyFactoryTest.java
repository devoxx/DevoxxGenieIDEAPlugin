package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightPlatformTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PromptExecutionStrategyFactoryTest extends LightPlatformTestCase {

    @Mock
    private Project project;
    
    @Mock
    private ChatMessageContext context;
    
    @Mock
    private DevoxxGenieStateService stateService;
    
    private PromptExecutionStrategyFactory factory;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        // Set up default behaviors
        when(context.getProject()).thenReturn(project);
        
        factory = new PromptExecutionStrategyFactory();
    }

    @Test
    public void testCreateStrategy_WebSearch() {
        // Set up context for web search
        when(context.isWebSearchRequested()).thenReturn(true);
        
        // Create strategy
        PromptExecutionStrategy strategy = factory.createStrategy(context);
        
        // Verify strategy type
        assertTrue(strategy instanceof WebSearchPromptStrategy);
    }
    
    @Test
    public void testCreateStrategy_StreamingEnabled() {
        // Set up context without web search
        when(context.isWebSearchRequested()).thenReturn(false);
        
        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Set up streaming mode as enabled
            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getStreamMode()).thenReturn(true);
            
            // Create strategy
            PromptExecutionStrategy strategy = factory.createStrategy(context);
            
            // Verify strategy type
            assertTrue(strategy instanceof StreamingPromptStrategy);
        }
    }
    
    @Test
    public void testCreateStrategy_NonStreaming() {
        // Set up context without web search
        when(context.isWebSearchRequested()).thenReturn(false);
        
        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Set up streaming mode as disabled
            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getStreamMode()).thenReturn(false);
            
            // Create strategy
            PromptExecutionStrategy strategy = factory.createStrategy(context);
            
            // Verify strategy type
            assertTrue(strategy instanceof NonStreamingPromptStrategy);
        }
    }
    
    @Test
    public void testCreateStrategy_StreamingNull() {
        // Set up context without web search
        when(context.isWebSearchRequested()).thenReturn(false);
        
        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Set up streaming mode as null (default to non-streaming)
            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getStreamMode()).thenReturn(null);
            
            // Create strategy
            PromptExecutionStrategy strategy = factory.createStrategy(context);
            
            // Verify strategy type
            assertTrue(strategy instanceof NonStreamingPromptStrategy);
        }
    }
}
