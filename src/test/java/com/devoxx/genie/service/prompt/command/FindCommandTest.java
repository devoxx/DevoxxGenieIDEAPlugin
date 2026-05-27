package com.devoxx.genie.service.prompt.command;

import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindCommandTest {

    @Mock
    private ChatMessageContext context;
    
    @Mock
    private PromptOutputPanel panel;
    
    @Mock
    private Project project;
    
    @Mock
    private DevoxxGenieStateService stateService;
    
    private FindCommand command;

    @BeforeEach
    public void setUp() {
        command = new FindCommand();
        // Removed the unnecessary stubbing from here
    }

    @Test
    void testMatches_WithFindCommand() {
        // Test with exact find command
        assertTrue(command.matches(Constant.COMMAND_PREFIX + Constant.FIND_COMMAND));
        
        // Test with find command and search query
        assertTrue(command.matches(Constant.COMMAND_PREFIX + Constant.FIND_COMMAND + " search query"));
        
        // Test with find command and leading/trailing whitespace
        assertTrue(command.matches("  " + Constant.COMMAND_PREFIX + Constant.FIND_COMMAND + "  "));
    }
    
    @Test
    void testMatches_WithNonFindCommand() {
        // Test with non-find command
        assertFalse(command.matches(Constant.COMMAND_PREFIX + "not-find"));
        
        // Test with plain text
        assertFalse(command.matches("How do I find something?"));
        
        // Test with find command without prefix
        assertFalse(command.matches(Constant.FIND_COMMAND));
    }
    
    @Test
    void testProcess_WithRAGEnabled() {
        // After task-222, RAG has only one switch (ragEnabled in settings). The per-session
        // ragActivated toggle was removed, so /find succeeds whenever RAG is enabled.
        when(context.getUserPrompt()).thenReturn(Constant.COMMAND_PREFIX + Constant.FIND_COMMAND + " search query");

        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getRagEnabled()).thenReturn(true);

            Optional<String> result = command.process(context, panel);

            assertTrue(result.isPresent());
            assertEquals("search query", result.get());

            verify(context).setCommandName(Constant.FIND_COMMAND);

            try (MockedStatic<NotificationUtil> notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class)) {
                notificationUtilMockedStatic.verify(() ->
                    NotificationUtil.sendNotification(any(), anyString()),
                    never());
            }
        }
    }

    @Test
    void testProcess_WithRAGDisabled() {
        when(context.getProject()).thenReturn(project);

        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
             MockedStatic<NotificationUtil> notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class)) {

            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getRagEnabled()).thenReturn(false);

            Optional<String> result = command.process(context, panel);

            assertFalse(result.isPresent());

            notificationUtilMockedStatic.verify(() ->
                NotificationUtil.sendNotification(
                    eq(project),
                    eq("The /find command requires RAG to be enabled in settings")));

            verify(context, never()).setCommandName(anyString());
        }
    }
}
