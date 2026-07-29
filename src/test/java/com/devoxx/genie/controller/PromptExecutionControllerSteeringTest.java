package com.devoxx.genie.controller;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.PromptExecutionService;
import com.devoxx.genie.service.prompt.command.PromptCommandProcessor;
import com.devoxx.genie.service.prompt.steering.SteeringMessageQueue;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.listener.PromptSubmissionListener;
import com.devoxx.genie.ui.panel.ActionButtonsPanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.panel.conversation.ConversationPanel;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Issue #1241: submitting a prompt while an agent task is running steers the
 * running task (queued for next-round-trip injection) instead of stopping it.
 * Stopping remains available via an empty-input submit and the stop flow.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromptExecutionControllerSteeringTest {

    private static final String LOCATION_HASH = "steeringTestHash";
    private static final String TAB_ID = "tab1";
    private static final String MEMORY_KEY = LOCATION_HASH + "-" + TAB_ID;

    @Mock private Project project;
    @Mock private PromptInputArea promptInputArea;
    @Mock private PromptOutputPanel promptOutputPanel;
    @Mock private ActionButtonsPanel actionButtonsPanel;
    @Mock private PromptExecutionService promptExecutionService;
    @Mock private PromptCommandProcessor commandProcessor;
    @Mock private ConversationPanel conversationPanel;
    @Mock private MessageBus messageBus;
    @Mock private PromptSubmissionListener promptSubmissionListener;

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
        when(promptOutputPanel.isNewConversation()).thenReturn(false);
        when(project.getLocationHash()).thenReturn(LOCATION_HASH);
        when(project.getMessageBus()).thenReturn(messageBus);
        when(messageBus.syncPublisher(AppTopics.PROMPT_SUBMISSION_TOPIC)).thenReturn(promptSubmissionListener);

        controller = new PromptExecutionController(project, promptInputArea, promptOutputPanel, actionButtonsPanel);
    }

    @AfterEach
    void tearDown() {
        SteeringMessageQueue.getInstance().drainAndDeactivate(MEMORY_KEY);
        executionServiceMockedStatic.close();
        commandProcessorMockedStatic.close();
    }

    private ChatMessageContext startRunningPrompt() {
        ChatMessageContext runningContext = ChatMessageContext.builder()
                .project(project)
                .userPrompt("implement the endpoints")
                .tabId(TAB_ID)
                .build();
        when(commandProcessor.processCommands(any(ChatMessageContext.class), eq(promptOutputPanel)))
                .thenReturn(Optional.of("implement the endpoints"));
        controller.handlePromptSubmission(runningContext);
        assertThat(controller.isPromptRunning()).isTrue();
        // The prompt strategy activates steering when it wires the injector
        SteeringMessageQueue.getInstance().activate(MEMORY_KEY);
        return runningContext;
    }

    private ChatMessageContext steeringContext(String text) {
        return ChatMessageContext.builder()
                .project(project)
                .userPrompt(text)
                .tabId(TAB_ID)
                .build();
    }

    @Test
    void submitWhileRunningWithActiveSteering_queuesMessageInsteadOfStopping() {
        startRunningPrompt();

        boolean result = controller.handlePromptSubmission(steeringContext("use snake_case for the API json"));

        assertThat(result).isTrue();
        assertThat(controller.isPromptRunning()).isTrue();
        verify(promptExecutionService, never()).stopExecution(any());
        verify(promptExecutionService, never()).stopExecution(any(), any());
        assertThat(SteeringMessageQueue.getInstance().drain(MEMORY_KEY))
                .containsExactly("use snake_case for the API json");
    }

    @Test
    void submitWhileRunningWithActiveSteering_showsUserBubbleAndClearsInput() {
        startRunningPrompt();

        controller.handlePromptSubmission(steeringContext("use snake_case for the API json"));

        verify(conversationPanel).addSteeringMessage("use snake_case for the API json");
        verify(promptInputArea).clear();
    }

    @Test
    void submitWhileRunningWithoutActiveSteering_stopsAsBefore() {
        startRunningPrompt();
        SteeringMessageQueue.getInstance().deactivate(MEMORY_KEY);

        boolean result = controller.handlePromptSubmission(steeringContext("some text"));

        assertThat(result).isTrue();
        assertThat(controller.isPromptRunning()).isFalse();
        verify(promptExecutionService).stopExecution(project, TAB_ID);
    }

    @Test
    void submitBlankWhileRunning_stopsExecution() {
        startRunningPrompt();

        controller.handlePromptSubmission(steeringContext("   "));

        assertThat(controller.isPromptRunning()).isFalse();
        verify(promptExecutionService).stopExecution(project, TAB_ID);
    }

    @Test
    void unconsumedSteeringMessages_areResubmittedAsNewPromptWhenRunEnds() {
        startRunningPrompt();
        ArgumentCaptor<Runnable> completion = ArgumentCaptor.forClass(Runnable.class);
        verify(promptExecutionService).executePrompt(any(), eq(promptOutputPanel), completion.capture());

        controller.handlePromptSubmission(steeringContext("late correction"));

        // Run finishes before the loop consumed the steering message
        completion.getValue().run();

        verify(promptSubmissionListener).onPromptSubmitted(project, "late correction", TAB_ID);
        assertThat(SteeringMessageQueue.getInstance().hasPending(MEMORY_KEY)).isFalse();
        assertThat(SteeringMessageQueue.getInstance().isActive(MEMORY_KEY)).isFalse();
    }

    @Test
    void unconsumedSteeringMessages_areDiscardedWhenUserStopsTheRun() {
        startRunningPrompt();
        controller.handlePromptSubmission(steeringContext("correction"));

        controller.stopPromptExecution();

        verify(promptSubmissionListener, never()).onPromptSubmitted(any(), any(), any());
        assertThat(SteeringMessageQueue.getInstance().hasPending(MEMORY_KEY)).isFalse();
        assertThat(SteeringMessageQueue.getInstance().isActive(MEMORY_KEY)).isFalse();
    }

    @Test
    void steerRunningPromptWithRawText_queuesMessageAndShowsBubble() {
        // The Enter key submits via the message-bus route, which carries only the raw
        // prompt text — steering must work without a ChatMessageContext.
        startRunningPrompt();

        boolean steered = controller.steerRunningPrompt("use snake_case for the API json");

        assertThat(steered).isTrue();
        assertThat(controller.isPromptRunning()).isTrue();
        assertThat(SteeringMessageQueue.getInstance().drain(MEMORY_KEY))
                .containsExactly("use snake_case for the API json");
        verify(conversationPanel).addSteeringMessage("use snake_case for the API json");
        verify(promptInputArea).clear();
    }

    @Test
    void steerRunningPromptWithRawText_returnsFalseWhenSteeringNotActive() {
        startRunningPrompt();
        SteeringMessageQueue.getInstance().deactivate(MEMORY_KEY);

        assertThat(controller.steerRunningPrompt("some text")).isFalse();
    }

    @Test
    void steerRunningPromptWithRawText_returnsFalseWhenNotRunning() {
        assertThat(controller.steerRunningPrompt("some text")).isFalse();
    }

    @Test
    void normalRunEndWithoutSteering_publishesNothing() {
        startRunningPrompt();
        ArgumentCaptor<Runnable> completion = ArgumentCaptor.forClass(Runnable.class);
        verify(promptExecutionService).executePrompt(any(), eq(promptOutputPanel), completion.capture());

        completion.getValue().run();

        verify(promptSubmissionListener, never()).onPromptSubmitted(any(), any(), any());
    }
}
