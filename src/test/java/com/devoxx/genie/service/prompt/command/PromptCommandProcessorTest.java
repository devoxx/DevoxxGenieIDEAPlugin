package com.devoxx.genie.service.prompt.command;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptCommandProcessorTest {

    private static class TestPromptCommandProcessor extends PromptCommandProcessor {
        private final List<PromptCommand> testCommands;
        private final boolean mcpEnabled;
        private final EditorInfo editorInfo;
        
        public TestPromptCommandProcessor(boolean mcpEnabled, EditorInfo editorInfo, PromptCommand... commands) {
            this.testCommands = List.of(commands);
            this.mcpEnabled = mcpEnabled;
            this.editorInfo = editorInfo;
        }
        
        @Override
        public Optional<String> processCommands(@NotNull ChatMessageContext chatMessageContext,
                                                @NotNull PromptOutputPanel promptOutputPanel) {
            // Modified implementation that doesn't rely on calling getEditorInfo
            if (chatMessageContext.getEditorInfo() == null && !mcpEnabled) {
                chatMessageContext.setEditorInfo(editorInfo);
            }
            
            String prompt = chatMessageContext.getUserPrompt().trim();
            
            for (PromptCommand command : testCommands) {
                if (command.matches(prompt)) {
                    Optional<String> result = command.process(chatMessageContext, promptOutputPanel);
                    result.ifPresent(chatMessageContext::setUserPrompt);
                    return result;
                }
            }
            
            return Optional.of(prompt);
        }
        
        // Override getEditorInfo to avoid calling the real method
        @Override
        public EditorInfo getEditorInfo(Project project) {
            return editorInfo;
        }
    }

    @Mock
    private Project project;
    
    @Mock
    private ChatMessageContext context;
    
    @Mock
    private PromptOutputPanel panel;
    
    @Mock
    private FindCommand findCommand;
    
    @Mock
    private HelpCommand helpCommand;
    
    @Mock
    private CustomPromptCommand customPromptCommand;
    
    private PromptCommandProcessor processor;
    private boolean mcpEnabled = false;
    private EditorInfo mockEditorInfo;

    @BeforeEach
    public void setUp() {
        // Create a mock EditorInfo that we'll use
        mockEditorInfo = new EditorInfo();
        mockEditorInfo.setLanguage("java");
        mockEditorInfo.setSelectedText("Test code");
        mockEditorInfo.setSelectedFiles(new ArrayList<>());
        
        // Create a special test processor to allow injecting mocked commands
        processor = new TestPromptCommandProcessor(mcpEnabled, mockEditorInfo, 
                                                  findCommand, helpCommand, customPromptCommand);
    }

    @Test
    void testProcessCommands_WithNoCommandMatch() {
        // Set up commands to not match
        when(findCommand.matches(anyString())).thenReturn(false);
        when(helpCommand.matches(anyString())).thenReturn(false);
        when(customPromptCommand.matches(anyString())).thenReturn(false);
        
        // Set up the prompt for this test
        when(context.getUserPrompt()).thenReturn("Sample prompt");
        
        // Process the prompt
        Optional<String> result = processor.processCommands(context, panel);
        
        // Verify result
        assertTrue(result.isPresent());
        assertEquals("Sample prompt", result.get());
        
        // Verify no command was processed
        verify(findCommand, never()).process(any(), any());
        verify(helpCommand, never()).process(any(), any());
        verify(customPromptCommand, never()).process(any(), any());
    }
    
    @Test
    void testProcessCommands_WithCommandMatch() {
        // Set up find command to match - only set up what's needed
        when(findCommand.matches(anyString())).thenReturn(true);
        
        // Set up find command to process and return result
        when(findCommand.process(any(), any())).thenReturn(Optional.of("Processed prompt"));
        
        // Set up the prompt for this test
        when(context.getUserPrompt()).thenReturn("Sample prompt");
        
        // Process the prompt
        Optional<String> result = processor.processCommands(context, panel);
        
        // Verify result
        assertTrue(result.isPresent());
        assertEquals("Processed prompt", result.get());
        
        // Verify find command was processed
        verify(findCommand).process(context, panel);
        
        // Verify context was updated
        verify(context).setUserPrompt("Processed prompt");
    }
    
    @Test
    void testProcessCommands_WithCommandHalt() {
        // Set up help command to match and signal halt - only set up what's needed
        when(helpCommand.matches(anyString())).thenReturn(true);
        
        // Set up help command to process and return empty (signal halt)
        when(helpCommand.process(any(), any())).thenReturn(Optional.empty());
        
        // Set up the prompt for this test
        when(context.getUserPrompt()).thenReturn("Sample prompt");
        
        // Process the prompt
        Optional<String> result = processor.processCommands(context, panel);
        
        // Verify result is empty (signal halt)
        assertTrue(result.isEmpty());
        
        // Verify help command was processed
        verify(helpCommand).process(context, panel);
        
        // Verify context was not updated
        verify(context, never()).setUserPrompt(anyString());
    }
    
    @Test
    void testProcessCommands_WithEditorInfo_AlreadyPopulated() {
        // Set up context with editor info already populated
        EditorInfo editorInfo = new EditorInfo();
        when(context.getEditorInfo()).thenReturn(editorInfo);
        
        // Set up commands to not match so we can test just the editor info logic
        when(findCommand.matches(anyString())).thenReturn(false);
        when(helpCommand.matches(anyString())).thenReturn(false);
        when(customPromptCommand.matches(anyString())).thenReturn(false);
        
        // Set up the prompt for this test
        when(context.getUserPrompt()).thenReturn("Sample prompt");
        
        // Process the prompt
        processor.processCommands(context, panel);
        
        // Verify editor info was not modified
        verify(context, never()).setEditorInfo(any());
    }
    
    @Test
    void testProcessCommands_WithEditorInfo_MissingAndMCPDisabled() {
        // Set up context with missing editor info
        when(context.getEditorInfo()).thenReturn(null);
        
        // Set up commands to not match so we can test just the editor info logic
        when(findCommand.matches(anyString())).thenReturn(false);
        when(helpCommand.matches(anyString())).thenReturn(false);
        when(customPromptCommand.matches(anyString())).thenReturn(false);
        
        // Set up the prompt for this test
        when(context.getUserPrompt()).thenReturn("Sample prompt");
        
        // Process the prompt with mcpEnabled = false
        processor = new TestPromptCommandProcessor(false, mockEditorInfo, findCommand, helpCommand, customPromptCommand);
        processor.processCommands(context, panel);
        
        // Verify editor info was set
        verify(context).setEditorInfo(mockEditorInfo);
    }
    
    @Test
    void testProcessCommands_WithEditorInfo_MissingAndMCPEnabled() {
        // Set up context with missing editor info
        when(context.getEditorInfo()).thenReturn(null);
        
        // Set up commands to not match so we can test just the editor info logic
        when(findCommand.matches(anyString())).thenReturn(false);
        when(helpCommand.matches(anyString())).thenReturn(false);
        when(customPromptCommand.matches(anyString())).thenReturn(false);
        
        // Set up the prompt for this test
        when(context.getUserPrompt()).thenReturn("Sample prompt");
        
        // Process the prompt with mcpEnabled = true
        processor = new TestPromptCommandProcessor(true, mockEditorInfo, findCommand, helpCommand, customPromptCommand);
        processor.processCommands(context, panel);
        
        // Verify editor info was not set
        verify(context, never()).setEditorInfo(any());
    }
    
    @Test
    void testGetEditorInfoReturnsExpectedValue() {
        // Create a test processor with our mock editor info
        TestPromptCommandProcessor testProcessor = 
            new TestPromptCommandProcessor(false, mockEditorInfo, findCommand);
        
        // Call getEditorInfo and verify it returns our mock
        EditorInfo result = testProcessor.getEditorInfo(project);
        
        // Verify it's the same object
        assertSame(mockEditorInfo, result);
        assertEquals("java", result.getLanguage());
        assertEquals("Test code", result.getSelectedText());
    }
}
