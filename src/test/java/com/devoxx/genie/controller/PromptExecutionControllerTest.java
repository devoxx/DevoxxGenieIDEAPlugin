package com.devoxx.genie.controller;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.PromptExecutionService;
import com.devoxx.genie.service.prompt.command.PromptCommandProcessor;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.panel.ActionButtonsPanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.panel.conversation.ConversationPanel;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromptExecutionControllerTest {

    @Mock
    private Project project;

    @Mock
    private PromptInputArea promptInputArea;

    @Mock
    private PromptOutputPanel promptOutputPanel;

    @Mock
    private ActionButtonsPanel actionButtonsPanel;

    @Mock
    private PromptExecutionService promptExecutionService;

    @Mock
    private PromptCommandProcessor commandProcessor;

    @Mock
    private ConversationPanel conversationPanel;

    private MockedStatic<PromptExecutionService> executionServiceMockedStatic;
    private MockedStatic<PromptCommandProcessor> commandProcessorMockedStatic;

    private PromptExecutionController controller;

    @BeforeEach
    void setUp() {
        executionServiceMockedStatic = Mockito.mockStatic(PromptExecutionService.class);
        executionServiceMockedStatic
                .when(() -> PromptExecutionService.getInstance(project))
                .thenReturn(promptExecutionService);

        commandProcessorMockedStatic = Mockito.mockStatic(PromptCommandProcessor.class);
        commandProcessorMockedStatic
                .when(PromptCommandProcessor::getInstance)
                .thenReturn(commandProcessor);

        when(promptOutputPanel.getConversationPanel()).thenReturn(conversationPanel);

        controller = new PromptExecutionController(project, promptInputArea, promptOutputPanel, actionButtonsPanel);
    }

    @AfterEach
    void tearDown() {
        executionServiceMockedStatic.close();
        commandProcessorMockedStatic.close();
    }

    @Test
    void testIsPromptRunning_InitiallyFalse() {
        assertThat(controller.isPromptRunning()).isFalse();
    }

    @Test
    void testStartPromptExecution_SetsRunningTrue() {
        controller.startPromptExecution();

        assertThat(controller.isPromptRunning()).isTrue();
        verify(actionButtonsPanel).disableSubmitBtn();
        verify(actionButtonsPanel).disableButtons();
        verify(actionButtonsPanel).startGlowing();
    }

    @Test
    void testEndPromptExecution_SetsRunningFalse() {
        controller.startPromptExecution();
        assertThat(controller.isPromptRunning()).isTrue();

        controller.endPromptExecution();

        assertThat(controller.isPromptRunning()).isFalse();
        verify(actionButtonsPanel).enableButtons();
    }

    @Test
    void testStopPromptExecution_CallsServiceAndEnds() {
        controller.startPromptExecution();

        controller.stopPromptExecution();

        verify(promptExecutionService).stopExecution(project);
        assertThat(controller.isPromptRunning()).isFalse();
        verify(actionButtonsPanel).enableButtons();
    }

    @Test
    void testHandlePromptSubmission_WhenAlreadyRunning_StopsExecution() {
        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt("test prompt")
                .build();

        // Start prompt execution first
        controller.startPromptExecution();
        assertThat(controller.isPromptRunning()).isTrue();

        boolean result = controller.handlePromptSubmission(context);

        assertThat(result).isTrue();
        verify(promptExecutionService).stopExecution(project);
        assertThat(controller.isPromptRunning()).isFalse();
    }

    @Test
    void testHandlePromptSubmission_CommandProcessed_ExecutesPrompt() {
        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt("tell me about Java")
                .build();

        when(promptOutputPanel.isNewConversation()).thenReturn(false);
        when(commandProcessor.processCommands(any(ChatMessageContext.class), eq(promptOutputPanel)))
                .thenReturn(Optional.of("tell me about Java"));

        boolean result = controller.handlePromptSubmission(context);

        assertThat(result).isTrue();
        assertThat(controller.isPromptRunning()).isTrue();
        verify(conversationPanel).addUserPromptMessage(context);
        verify(promptExecutionService).executePrompt(eq(context), eq(promptOutputPanel), any(Runnable.class));
    }

    @Test
    void testHandlePromptSubmission_CommandNotProcessed_ReturnsFalse() {
        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt("/help")
                .build();

        when(promptOutputPanel.isNewConversation()).thenReturn(false);
        when(commandProcessor.processCommands(any(ChatMessageContext.class), eq(promptOutputPanel)))
                .thenReturn(Optional.empty());

        boolean result = controller.handlePromptSubmission(context);

        assertThat(result).isFalse();
        assertThat(controller.isPromptRunning()).isFalse();
        verify(promptExecutionService, never()).executePrompt(any(), any(), any());
    }

    @Test
    void testHandlePromptSubmission_HelpCommand_DoesNotAddUserPromptMessage() {
        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt("/help")
                .build();

        when(promptOutputPanel.isNewConversation()).thenReturn(false);
        when(commandProcessor.processCommands(any(ChatMessageContext.class), eq(promptOutputPanel)))
                .thenReturn(Optional.of("Help information"));

        controller.handlePromptSubmission(context);

        // /help starts with "/help" so addUserPromptMessage should NOT be called
        verify(conversationPanel, never()).addUserPromptMessage(any());
    }

    @Test
    void testHandlePromptSubmission_NewConversation_ClearsWelcomeContent() {
        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt("first message")
                .build();

        when(promptOutputPanel.isNewConversation()).thenReturn(true);
        when(commandProcessor.processCommands(any(ChatMessageContext.class), eq(promptOutputPanel)))
                .thenReturn(Optional.of("first message"));

        controller.handlePromptSubmission(context);

        verify(conversationPanel).clearWithoutWelcome();
        verify(promptOutputPanel).markConversationAsStarted();
    }

    @Test
    void testHandlePromptSubmission_ExistingConversation_DoesNotClearWelcome() {
        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt("follow-up message")
                .build();

        when(promptOutputPanel.isNewConversation()).thenReturn(false);
        when(commandProcessor.processCommands(any(ChatMessageContext.class), eq(promptOutputPanel)))
                .thenReturn(Optional.of("follow-up message"));

        controller.handlePromptSubmission(context);

        verify(conversationPanel, never()).clearWithoutWelcome();
        verify(promptOutputPanel, never()).markConversationAsStarted();
    }

    @Test
    void testHandlePromptSubmission_NonNewConversation_ScrollsToBottom() {
        ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .userPrompt("message")
                .build();

        when(promptOutputPanel.isNewConversation()).thenReturn(false);
        when(commandProcessor.processCommands(any(ChatMessageContext.class), eq(promptOutputPanel)))
                .thenReturn(Optional.of("message"));

        controller.handlePromptSubmission(context);

        verify(promptOutputPanel).scrollToBottom();
    }

    @Test
    void testStartPromptExecution_MultipleCalls_IncreasesExecutionId() {
        // First start
        controller.startPromptExecution();
        assertThat(controller.isPromptRunning()).isTrue();

        controller.endPromptExecution();
        assertThat(controller.isPromptRunning()).isFalse();

        // Second start should succeed
        controller.startPromptExecution();
        assertThat(controller.isPromptRunning()).isTrue();

        // Verify disableSubmitBtn was called twice
        verify(actionButtonsPanel, times(2)).disableSubmitBtn();
    }
}
