package com.devoxx.genie.service.prompt.command;

import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomPromptCommandTest {

    @Mock
    private ChatMessageContext context;
    
    @Mock
    private PromptOutputPanel panel;
    
    @Mock
    private DevoxxGenieStateService stateService;
    
    private CustomPromptCommand command;
    
    private final CustomPrompt testPrompt1 = new CustomPrompt("test", "This is a test prompt template");
    private final CustomPrompt testPrompt2 = new CustomPrompt("debug", "Debug the following code:");

    @Before
    public void setUp() {
        command = new CustomPromptCommand();
    }

    @Test
    public void testMatches_WithCustomCommand() {
        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Set up custom commands
            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getCustomPrompts()).thenReturn(List.of(testPrompt1, testPrompt2));
            
            // Test with exact custom commands
            assertTrue(command.matches(Constant.COMMAND_PREFIX + "test"));
            assertTrue(command.matches(Constant.COMMAND_PREFIX + "debug"));
            
            // Test with custom command and args
            assertTrue(command.matches(Constant.COMMAND_PREFIX + "test argument"));
            
            // Test with custom command and leading/trailing whitespace
            assertTrue(command.matches("  " + Constant.COMMAND_PREFIX + "debug  "));
        }
    }
    
    @Test
    public void testMatches_WithNonCustomCommand() {
        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Set up custom commands
            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getCustomPrompts()).thenReturn(List.of(testPrompt1, testPrompt2));
            
            // Test with non-custom command
            assertFalse(command.matches(Constant.COMMAND_PREFIX + "unknown"));
            
            // Test with plain text (not a command)
            assertFalse(command.matches("How do I test something?"));
            
            // Test with help command (should be handled by HelpCommand)
            assertFalse(command.matches(Constant.COMMAND_PREFIX + Constant.HELP_COMMAND));
        }
    }
    
    @Test
    public void testMatches_WithNoCustomCommands() {
        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Set up empty custom commands
            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getCustomPrompts()).thenReturn(Collections.emptyList());
            
            // Test with command that would match if it existed
            assertFalse(command.matches(Constant.COMMAND_PREFIX + "test"));
        }
    }
    
    @Test
    public void testProcess_WithMatchingCommand() {
        // Set up context
        when(context.getUserPrompt()).thenReturn(Constant.COMMAND_PREFIX + "test additional arguments");
        
        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Set up custom commands
            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getCustomPrompts()).thenReturn(List.of(testPrompt1, testPrompt2));
            
            // Process the command
            Optional<String> result = command.process(context, panel);
            
            // Verify result contains the processed prompt
            assertTrue(result.isPresent());
            assertEquals("This is a test prompt template additional arguments", result.get());
            
            // Verify context was updated with command name
            verify(context).setCommandName("test");
        }
    }
    
    @Test
    public void testProcess_WithNoMatchingCommand() {
        // Set up context
        String originalPrompt = Constant.COMMAND_PREFIX + "unknown command";
        when(context.getUserPrompt()).thenReturn(originalPrompt);
        
        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Set up custom commands
            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getCustomPrompts()).thenReturn(List.of(testPrompt1, testPrompt2));
            
            // Process the command
            Optional<String> result = command.process(context, panel);
            
            // Verify result contains the original prompt
            assertTrue(result.isPresent());
            assertEquals(originalPrompt, result.get());
        }
    }
    
    @Test
    public void testProcess_WithEmptyArgs() {
        // Set up context with no additional arguments
        when(context.getUserPrompt()).thenReturn(Constant.COMMAND_PREFIX + "debug");
        
        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Set up custom commands
            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getCustomPrompts()).thenReturn(List.of(testPrompt1, testPrompt2));
            
            // Process the command
            Optional<String> result = command.process(context, panel);
            
            // Verify result contains just the template prompt with a trailing space
            assertTrue(result.isPresent());
            assertEquals("Debug the following code: ", result.get());
            
            // Verify context was updated with command name
            verify(context).setCommandName("debug");
        }
    }
}
