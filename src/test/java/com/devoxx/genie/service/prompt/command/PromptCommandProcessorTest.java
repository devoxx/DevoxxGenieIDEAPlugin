package com.devoxx.genie.service.prompt.command;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.util.FileUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightPlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PromptCommandProcessorTest extends LightPlatformTestCase {

    /**
     * Special test implementation that allows injecting mock commands
     */
    private static class TestPromptCommandProcessor extends PromptCommandProcessor {
        private final List<PromptCommand> testCommands;
        
        public TestPromptCommandProcessor(PromptCommand... commands) {
            this.testCommands = List.of(commands);
        }
        
        @Override
        public Optional<String> processCommands(@NotNull ChatMessageContext chatMessageContext,
                                                @NotNull PromptOutputPanel promptOutputPanel) {
            // Same implementation as parent but using our test commands
            if (chatMessageContext.getEditorInfo() == null && !MCPService.isMCPEnabled()) {
                chatMessageContext.setEditorInfo(getEditorInfo(chatMessageContext.getProject()));
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
    
    @Mock
    private FileEditorManager fileEditorManager;
    
    @Mock
    private Editor editor;
    
    @Mock
    private SelectionModel selectionModel;
    
    @Mock
    private VirtualFile virtualFile;
    
    private PromptCommandProcessor processor;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        // Set up default behaviors
        when(context.getProject()).thenReturn(project);
        when(context.getUserPrompt()).thenReturn("Sample prompt");
        
        // Create a special test processor to allow injecting mocked commands
        processor = new TestPromptCommandProcessor(findCommand, helpCommand, customPromptCommand);
        
        // Set up editor-related mocks
        when(editor.getSelectionModel()).thenReturn(selectionModel);
        when(fileEditorManager.getSelectedTextEditor()).thenReturn(editor);
        
        // Set up for file info
        VirtualFile[] files = new VirtualFile[]{virtualFile};
        when(fileEditorManager.getOpenFiles()).thenReturn(files);
        when(fileEditorManager.getSelectedFiles()).thenReturn(files);
    }

    @Test
    public void testProcessCommands_WithNoCommandMatch() {
        // Set up commands to not match
        when(findCommand.matches(anyString())).thenReturn(false);
        when(helpCommand.matches(anyString())).thenReturn(false);
        when(customPromptCommand.matches(anyString())).thenReturn(false);
        
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
    public void testProcessCommands_WithCommandMatch() {
        // Set up find command to match
        when(findCommand.matches(anyString())).thenReturn(true);
        when(helpCommand.matches(anyString())).thenReturn(false);
        when(customPromptCommand.matches(anyString())).thenReturn(false);
        
        // Set up find command to process and return result
        when(findCommand.process(any(), any())).thenReturn(Optional.of("Processed prompt"));
        
        // Process the prompt
        Optional<String> result = processor.processCommands(context, panel);
        
        // Verify result
        assertTrue(result.isPresent());
        assertEquals("Processed prompt", result.get());
        
        // Verify find command was processed, but others were not
        verify(findCommand).process(context, panel);
        verify(helpCommand, never()).process(any(), any());
        verify(customPromptCommand, never()).process(any(), any());
        
        // Verify context was updated
        verify(context).setUserPrompt("Processed prompt");
    }
    
    @Test
    public void testProcessCommands_WithCommandHalt() {
        // Set up help command to match and signal halt
        when(findCommand.matches(anyString())).thenReturn(false);
        when(helpCommand.matches(anyString())).thenReturn(true);
        when(customPromptCommand.matches(anyString())).thenReturn(false);
        
        // Set up help command to process and return empty (signal halt)
        when(helpCommand.process(any(), any())).thenReturn(Optional.empty());
        
        // Process the prompt
        Optional<String> result = processor.processCommands(context, panel);
        
        // Verify result is empty (signal halt)
        assertTrue(result.isEmpty());
        
        // Verify help command was processed, but others were not
        verify(helpCommand).process(context, panel);
        verify(findCommand, never()).process(any(), any());
        verify(customPromptCommand, never()).process(any(), any());
        
        // Verify context was not updated
        verify(context, never()).setUserPrompt(anyString());
    }
    
    @Test
    public void testProcessCommands_WithEditorInfo_AlreadyPopulated() {
        // Set up context with editor info already populated
        EditorInfo editorInfo = new EditorInfo();
        when(context.getEditorInfo()).thenReturn(editorInfo);
        
        // Process the prompt
        processor.processCommands(context, panel);
        
        // Verify editor info was not modified
        verify(context, never()).setEditorInfo(any());
    }
    
    @Test
    public void testProcessCommands_WithEditorInfo_MissingAndMCPDisabled() {
        // Set up context with missing editor info
        when(context.getEditorInfo()).thenReturn(null);
        
        try (MockedStatic<MCPService> mcpServiceMockedStatic = Mockito.mockStatic(MCPService.class);
             MockedStatic<FileEditorManager> fileEditorManagerMockedStatic = Mockito.mockStatic(FileEditorManager.class);
             MockedStatic<FileUtil> fileUtilMockedStatic = Mockito.mockStatic(FileUtil.class)) {
            
            // Set up MCP as disabled
            mcpServiceMockedStatic.when(MCPService::isMCPEnabled).thenReturn(false);
            
            // Set up file editor manager
            fileEditorManagerMockedStatic.when(() -> FileEditorManager.getInstance(project)).thenReturn(fileEditorManager);
            
            // Set up file type
            fileUtilMockedStatic.when(() -> FileUtil.getFileType(any())).thenReturn("java");
            
            // Process the prompt
            processor.processCommands(context, panel);
            
            // Verify editor info was set
            verify(context).setEditorInfo(any(EditorInfo.class));
        }
    }
    
    @Test
    public void testProcessCommands_WithEditorInfo_MissingAndMCPEnabled() {
        // Set up context with missing editor info
        when(context.getEditorInfo()).thenReturn(null);
        
        try (MockedStatic<MCPService> mcpServiceMockedStatic = Mockito.mockStatic(MCPService.class)) {
            // Set up MCP as enabled
            mcpServiceMockedStatic.when(MCPService::isMCPEnabled).thenReturn(true);
            
            // Process the prompt
            processor.processCommands(context, panel);
            
            // Verify editor info was not set
            verify(context, never()).setEditorInfo(any(EditorInfo.class));
        }
    }
    
    @Test
    public void testGetEditorInfo_WithSelectedText() {
        try (MockedStatic<FileEditorManager> fileEditorManagerMockedStatic = Mockito.mockStatic(FileEditorManager.class);
             MockedStatic<FileUtil> fileUtilMockedStatic = Mockito.mockStatic(FileUtil.class)) {
            
            // Set up file editor manager
            fileEditorManagerMockedStatic.when(() -> FileEditorManager.getInstance(project)).thenReturn(fileEditorManager);
            
            // Set up selected text
            when(selectionModel.getSelectedText()).thenReturn("Selected text");
            
            // Set up file type
            fileUtilMockedStatic.when(() -> FileUtil.getFileType(any())).thenReturn("java");
            
            // Call getEditorInfo
            EditorInfo editorInfo = processor.getEditorInfo(project);
            
            // Verify editor info has selected text
            assertEquals("Selected text", editorInfo.getSelectedText());
            assertEquals("java", editorInfo.getLanguage());
            assertTrue(editorInfo.getSelectedFiles() == null || editorInfo.getSelectedFiles().isEmpty());
        }
    }
    
    @Test
    public void testGetEditorInfo_WithNoSelectedText() {
        try (MockedStatic<FileEditorManager> fileEditorManagerMockedStatic = Mockito.mockStatic(FileEditorManager.class);
             MockedStatic<FileUtil> fileUtilMockedStatic = Mockito.mockStatic(FileUtil.class)) {
            
            // Set up file editor manager
            fileEditorManagerMockedStatic.when(() -> FileEditorManager.getInstance(project)).thenReturn(fileEditorManager);
            
            // Set up no selected text
            when(selectionModel.getSelectedText()).thenReturn(null);
            
            // Set up file type
            fileUtilMockedStatic.when(() -> FileUtil.getFileType(any())).thenReturn("java");
            
            // Call getEditorInfo
            EditorInfo editorInfo = processor.getEditorInfo(project);
            
            // Verify editor info has files instead of selected text
            assertTrue(editorInfo.getSelectedText() == null || editorInfo.getSelectedText().isEmpty());
            assertEquals("java", editorInfo.getLanguage());
            assertEquals(1, editorInfo.getSelectedFiles().size());
            assertEquals(virtualFile, editorInfo.getSelectedFiles().get(0));
        }
    }
    
    @Test
    public void testGetEditorInfo_WithNoEditor() {
        try (MockedStatic<FileEditorManager> fileEditorManagerMockedStatic = Mockito.mockStatic(FileEditorManager.class)) {
            
            // Set up file editor manager with no editor
            fileEditorManagerMockedStatic.when(() -> FileEditorManager.getInstance(project)).thenReturn(fileEditorManager);
            when(fileEditorManager.getSelectedTextEditor()).thenReturn(null);
            
            // Call getEditorInfo
            EditorInfo editorInfo = processor.getEditorInfo(project);
            
            // Verify editor info is empty
            assertTrue(editorInfo.getSelectedText() == null || editorInfo.getSelectedText().isEmpty());
            assertTrue(editorInfo.getLanguage() == null || editorInfo.getLanguage().isEmpty());
            assertTrue(editorInfo.getSelectedFiles() == null || editorInfo.getSelectedFiles().isEmpty());
        }
    }
}
