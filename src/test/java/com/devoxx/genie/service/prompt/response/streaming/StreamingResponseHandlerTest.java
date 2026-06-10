package com.devoxx.genie.service.prompt.response.streaming;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.compose.ConversationViewController;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StreamingResponseHandlerTest {

    @Mock private Project mockProject;
    @Mock private ChatMessageContext mockContext;
    @Mock private ConversationViewController mockViewController;
    @Mock private ChatMemoryManager mockChatMemoryManager;
    @Mock private ChatMemoryService mockChatMemoryService;
    @Mock private FileListManager mockFileListManager;
    @Mock private MessageBus mockMessageBus;

    private MockedStatic<ApplicationManager> applicationManagerMock;
    private MockedStatic<ChatMemoryManager> chatMemoryManagerMock;
    private MockedStatic<ChatMemoryService> chatMemoryServiceMock;
    private MockedStatic<FileListManager> fileListManagerMock;
    private MockedStatic<PromptErrorHandler> promptErrorHandlerMock;

    private AtomicReference<ChatResponse> completedResponse = new AtomicReference<>();
    private AtomicReference<Throwable> capturedError = new AtomicReference<>();
    private AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
    private AtomicBoolean onErrorCalled = new AtomicBoolean(false);

    /** Flush tasks captured from the handler instead of being run on a timer thread. */
    private final List<Runnable> pendingFlushes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        applicationManagerMock = mockStatic(ApplicationManager.class);
        chatMemoryManagerMock = mockStatic(ChatMemoryManager.class);
        chatMemoryServiceMock = mockStatic(ChatMemoryService.class);
        fileListManagerMock = mockStatic(FileListManager.class);
        promptErrorHandlerMock = mockStatic(PromptErrorHandler.class);

        Application mockApplication = mock(Application.class);
        applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(mockApplication);
        // Execute invokeLater inline
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mockApplication).invokeLater(any(Runnable.class));

        chatMemoryManagerMock.when(ChatMemoryManager::getInstance).thenReturn(mockChatMemoryManager);
        chatMemoryServiceMock.when(ChatMemoryService::getInstance).thenReturn(mockChatMemoryService);
        fileListManagerMock.when(FileListManager::getInstance).thenReturn(mockFileListManager);

        when(mockContext.getId()).thenReturn("test-context-id");
        when(mockContext.getProject()).thenReturn(mockProject);
        when(mockContext.getMemoryKey()).thenReturn("test-memory-key");
        when(mockFileListManager.isEmpty(any(Project.class))).thenReturn(true);
        when(mockFileListManager.isEmpty(any(Project.class), any())).thenReturn(true);

        // Mock message bus
        when(mockProject.getMessageBus()).thenReturn(mockMessageBus);
        var mockPublisher = mock(com.devoxx.genie.ui.listener.ConversationEventListener.class);
        when(mockMessageBus.syncPublisher(AppTopics.CONVERSATION_TOPIC)).thenReturn(mockPublisher);

        completedResponse.set(null);
        capturedError.set(null);
        onCompleteCalled.set(false);
        onErrorCalled.set(false);
        pendingFlushes.clear();
    }

    /** Runs all captured batched-flush tasks synchronously, as the timer would. */
    private void runPendingFlushes() {
        List<Runnable> tasks = new ArrayList<>(pendingFlushes);
        pendingFlushes.clear();
        tasks.forEach(Runnable::run);
    }

    @AfterEach
    void tearDown() {
        if (applicationManagerMock != null) applicationManagerMock.close();
        if (chatMemoryManagerMock != null) chatMemoryManagerMock.close();
        if (chatMemoryServiceMock != null) chatMemoryServiceMock.close();
        if (fileListManagerMock != null) fileListManagerMock.close();
        if (promptErrorHandlerMock != null) promptErrorHandlerMock.close();
    }

    private StreamingResponseHandler createHandler() {
        return createHandler(mockViewController);
    }

    private StreamingResponseHandler createHandlerWithNullWebView() {
        return createHandler(null);
    }

    private StreamingResponseHandler createHandler(ConversationViewController viewController) {
        Consumer<ChatResponse> onComplete = response -> {
            completedResponse.set(response);
            onCompleteCalled.set(true);
        };
        Consumer<Throwable> onError = error -> {
            capturedError.set(error);
            onErrorCalled.set(true);
        };
        return new StreamingResponseHandler(mockContext, viewController, onComplete, onError,
                (task, delayMillis) -> pendingFlushes.add(task),
                StreamingResponseHandler.FLUSH_INTERVAL_MS);
    }

    @Test
    void onPartialResponse_accumulatesTokens() {
        StreamingResponseHandler handler = createHandler();

        handler.onPartialResponse("Hello");
        handler.onPartialResponse(" World");
        runPendingFlushes();

        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(mockContext, atLeastOnce()).setAiMessage(captor.capture());
        assertThat(captor.getValue().text()).isEqualTo("Hello World");
        verify(mockViewController, atLeastOnce()).updateAiMessageContent(mockContext);
    }

    @Test
    void onPartialResponse_batchesUiUpdatesAtFixedCadence() {
        StreamingResponseHandler handler = createHandler();

        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            String token = "token" + i + " ";
            expected.append(token);
            handler.onPartialResponse(token);
        }

        // The first token is painted immediately; the remaining 99 must arm exactly
        // ONE pending flush instead of posting 99 more EDT updates.
        verify(mockViewController, times(1)).updateAiMessageContent(mockContext);
        assertThat(pendingFlushes).hasSize(1);

        runPendingFlushes();

        // One immediate paint + one batched flush — far fewer than 100 posts.
        verify(mockViewController, times(2)).updateAiMessageContent(mockContext);
        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(mockContext, atLeastOnce()).setAiMessage(captor.capture());
        assertThat(captor.getValue().text()).isEqualTo(expected.toString());
    }

    @Test
    void onPartialResponse_afterFlush_armsNewFlushForLaterTokens() {
        StreamingResponseHandler handler = createHandler();

        handler.onPartialResponse("first ");   // immediate paint
        handler.onPartialResponse("second ");  // arms flush #1
        runPendingFlushes();

        handler.onPartialResponse("third");    // must arm flush #2

        assertThat(pendingFlushes).hasSize(1);
        runPendingFlushes();

        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(mockContext, atLeastOnce()).setAiMessage(captor.capture());
        assertThat(captor.getValue().text()).isEqualTo("first second third");
    }

    @Test
    void onCompleteResponse_withUnflushedBufferedTokens_rendersFullAccumulatedText() {
        StreamingResponseHandler handler = createHandler();

        handler.onPartialResponse("Hello");   // immediate paint
        handler.onPartialResponse(" World");  // buffered, flush never runs

        AiMessage lastTurnOnly = AiMessage.from(" World");
        handler.onCompleteResponse(ChatResponse.builder().aiMessage(lastTurnOnly).build());

        // The final render must include the buffered tail even though no timer fired.
        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(mockContext, atLeastOnce()).setAiMessage(captor.capture());
        assertThat(captor.getValue().text()).isEqualTo("Hello World");
        assertThat(onCompleteCalled.get()).isTrue();
    }

    @Test
    void stop_flushesBufferedTokensToUi() {
        StreamingResponseHandler handler = createHandler();

        handler.onPartialResponse("Hello");   // immediate paint
        handler.onPartialResponse(" Wor");    // buffered
        handler.onPartialResponse("ld!");     // buffered

        handler.stop();

        // The user-visible text after stop must contain every received token.
        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(mockContext, atLeastOnce()).setAiMessage(captor.capture());
        assertThat(captor.getValue().text()).isEqualTo("Hello World!");
        verify(mockViewController, times(2)).updateAiMessageContent(mockContext);
    }

    @Test
    void scheduledFlush_afterStop_doesNotUpdateUi() {
        StreamingResponseHandler handler = createHandler();

        handler.onPartialResponse("Hello");   // immediate paint
        handler.onPartialResponse(" World");  // arms a flush

        handler.stop();                       // flushes the buffer itself
        clearInvocations(mockViewController);

        runPendingFlushes();                  // stale timer firing after stop

        verify(mockViewController, never()).updateAiMessageContent(any());
    }

    @Test
    void scheduledFlush_afterComplete_doesNotUpdateUi() {
        StreamingResponseHandler handler = createHandler();

        handler.onPartialResponse("Hello");   // immediate paint
        handler.onPartialResponse(" World");  // arms a flush

        handler.onCompleteResponse(ChatResponse.builder()
            .aiMessage(AiMessage.from("Hello World")).build());
        clearInvocations(mockViewController);

        runPendingFlushes();                  // stale timer firing after completion

        verify(mockViewController, never()).updateAiMessageContent(any());
    }

    @Test
    void batchedFlush_straddlingTurnBoundary_keepsSeparator() {
        StreamingResponseHandler handler = createHandler();

        handler.onPartialResponse("Let me check.");  // immediate paint (turn 1)
        handler.onIntermediateResponse(ChatResponse.builder()
            .aiMessage(AiMessage.from("Let me check.")).build());
        handler.onPartialResponse("Here it is.");    // buffered (turn 2)

        runPendingFlushes();

        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(mockContext, atLeastOnce()).setAiMessage(captor.capture());
        assertThat(captor.getValue().text()).isEqualTo("Let me check.\n\nHere it is.");
    }

    @Test
    void onPartialResponse_whenStopped_doesNothing() {
        StreamingResponseHandler handler = createHandler();
        handler.stop();

        handler.onPartialResponse("Should be ignored");

        verify(mockViewController, never()).updateAiMessageContent(any());
    }

    @Test
    void onPartialResponse_withNullWebViewController_updatesContextOnly() {
        StreamingResponseHandler handler = createHandlerWithNullWebView();

        handler.onPartialResponse("Hello");

        verify(mockContext).setAiMessage(any(AiMessage.class));
    }

    @Test
    void onCompleteResponse_setsExecutionTimeAndAiMessage() {
        StreamingResponseHandler handler = createHandler();
        AiMessage aiMessage = AiMessage.from("Complete response");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();

        handler.onCompleteResponse(response);

        verify(mockContext).setExecutionTimeMs(anyLong());
        verify(mockContext).setAiMessage(aiMessage);
        assertThat(onCompleteCalled.get()).isTrue();
        assertThat(completedResponse.get()).isEqualTo(response);
    }

    @Test
    void onCompleteResponse_afterIntermediateAgentText_preservesFullStreamedText() {
        StreamingResponseHandler handler = createHandler();

        // Agent mode: the LLM streams intermediate reasoning before a tool call (turn 1)...
        handler.onPartialResponse("Great question! Let me take a look at the code first.");
        // ...the tool executes, then the final answer is streamed (turn 2).
        handler.onPartialResponse("\n\nHere is the improved factory.");

        // langchain4j delivers onCompleteResponse with ONLY the final turn's AiMessage.
        AiMessage finalTurnOnly = AiMessage.from("Here is the improved factory.");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(finalTurnOnly)
            .build();

        handler.onCompleteResponse(response);

        // The chat panel reads context.aiMessage; it must keep the intermediate reasoning,
        // not just the final turn (otherwise "Great question!..." disappears from the chat).
        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(mockContext, atLeastOnce()).setAiMessage(captor.capture());
        assertThat(captor.getValue().text())
            .contains("Great question! Let me take a look at the code first.")
            .contains("Here is the improved factory.");
    }

    @Test
    void onIntermediateResponse_insertsSeparatorBetweenAgentTurns() {
        StreamingResponseHandler handler = createHandler();

        // Turn 1: intermediate reasoning streamed before a tool call.
        handler.onPartialResponse("Great question! Let me take a look.");
        // Turn boundary: langchain4j signals the intermediate (tool-calling) response.
        handler.onIntermediateResponse(ChatResponse.builder()
            .aiMessage(AiMessage.from("Great question! Let me take a look."))
            .build());
        // Turn 2: the final answer streamed (no separator in the raw tokens).
        handler.onPartialResponse("Here is the improved factory.");

        AiMessage finalTurnOnly = AiMessage.from("Here is the improved factory.");
        handler.onCompleteResponse(ChatResponse.builder().aiMessage(finalTurnOnly).build());

        ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
        verify(mockContext, atLeastOnce()).setAiMessage(captor.capture());
        // Turns must be visually separated by a blank line, not run together.
        assertThat(captor.getValue().text())
            .isEqualTo("Great question! Let me take a look.\n\nHere is the improved factory.");
    }

    @Test
    void onCompleteResponse_whenStopped_doesNothing() {
        StreamingResponseHandler handler = createHandler();
        handler.stop();

        AiMessage aiMessage = AiMessage.from("Should be ignored");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();

        handler.onCompleteResponse(response);

        assertThat(onCompleteCalled.get()).isFalse();
    }

    @Test
    void onCompleteResponse_publishesConversationTopic() {
        StreamingResponseHandler handler = createHandler();
        AiMessage aiMessage = AiMessage.from("Response");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();

        handler.onCompleteResponse(response);

        verify(mockProject.getMessageBus().syncPublisher(AppTopics.CONVERSATION_TOPIC))
            .onNewConversation(mockContext);
    }

    @Test
    void onCompleteResponse_addsAiResponseToMemory() {
        StreamingResponseHandler handler = createHandler();
        AiMessage aiMessage = AiMessage.from("Response");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();

        handler.onCompleteResponse(response);

        verify(mockChatMemoryManager).addAiResponse(mockContext);
    }

    @Test
    void onCompleteResponse_withPartialResponseFirst_updatesExistingMessage() {
        StreamingResponseHandler handler = createHandler();

        // Send partial first
        handler.onPartialResponse("Partial");

        // Then complete
        AiMessage aiMessage = AiMessage.from("Full response");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();
        handler.onCompleteResponse(response);

        // Should call updateAiMessageContent (not addChatMessage) since partials were received
        verify(mockViewController, atLeast(2)).updateAiMessageContent(mockContext);
    }

    @Test
    void onCompleteResponse_withoutPartialResponse_addsChatMessage() {
        StreamingResponseHandler handler = createHandler();

        AiMessage aiMessage = AiMessage.from("Complete response");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();
        handler.onCompleteResponse(response);

        verify(mockViewController).addChatMessage(mockContext);
    }

    @Test
    void onCompleteResponse_marksLogsAsCompleted() {
        StreamingResponseHandler handler = createHandler();
        AiMessage aiMessage = AiMessage.from("Response");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();

        handler.onCompleteResponse(response);

        verify(mockViewController).markMCPLogsAsCompleted("test-context-id");
    }

    @Test
    void onCompleteResponse_withNullWebViewController_stillCompletes() {
        StreamingResponseHandler handler = createHandlerWithNullWebView();
        AiMessage aiMessage = AiMessage.from("Response");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();

        handler.onCompleteResponse(response);

        assertThat(onCompleteCalled.get()).isTrue();
        verify(mockChatMemoryManager).addAiResponse(mockContext);
    }

    @Test
    void onCompleteResponse_withFileReferences_addsToWebView() {
        when(mockFileListManager.isEmpty(any(Project.class), any())).thenReturn(false);
        when(mockFileListManager.getFiles(any(Project.class), any())).thenReturn(new java.util.ArrayList<>());

        StreamingResponseHandler handler = createHandler();
        AiMessage aiMessage = AiMessage.from("Response");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();

        handler.onCompleteResponse(response);

        verify(mockViewController).addFileReferences(eq(mockContext), any());
    }

    @Test
    void onError_callsErrorCallback() {
        StreamingResponseHandler handler = createHandler();
        RuntimeException testError = new RuntimeException("Test error");

        handler.onError(testError);

        assertThat(onErrorCalled.get()).isTrue();
        assertThat(capturedError.get()).isNotNull();
    }

    @Test
    void onError_hidesLoadingIndicator() {
        StreamingResponseHandler handler = createHandler();

        handler.onError(new RuntimeException("Error"));

        verify(mockViewController).hideLoadingIndicator("test-context-id");
    }

    @Test
    void onError_deactivatesActivityHandlers() {
        StreamingResponseHandler handler = createHandler();

        handler.onError(new RuntimeException("Error"));

        verify(mockViewController).deactivateActivityHandlers();
    }

    @Test
    void stop_setsStoppedFlag() {
        StreamingResponseHandler handler = createHandler();

        handler.stop();

        // Subsequent calls should be ignored
        handler.onPartialResponse("Should be ignored");
        verify(mockViewController, never()).updateAiMessageContent(any());
    }

    @Test
    void stop_deactivatesActivityHandlers() {
        StreamingResponseHandler handler = createHandler();

        handler.stop();

        verify(mockViewController).deactivateActivityHandlers();
    }

    @Test
    void stop_hidesLoadingIndicator() {
        StreamingResponseHandler handler = createHandler();

        handler.stop();

        verify(mockViewController).hideLoadingIndicator("test-context-id");
    }

    @Test
    void stop_removesPartialResponseFromMemory() {
        StreamingResponseHandler handler = createHandler();
        when(mockContext.getAiMessage()).thenReturn(AiMessage.from("partial"));

        handler.stop();

        verify(mockChatMemoryService).removeLastMessageByKey("test-memory-key");
    }

    @Test
    void stop_withoutAiMessage_doesNotRemoveFromMemory() {
        StreamingResponseHandler handler = createHandler();
        when(mockContext.getAiMessage()).thenReturn(null);

        handler.stop();

        verify(mockChatMemoryService, never()).removeLastMessage(any());
    }

    @Test
    void stop_calledTwice_onlyExecutesOnce() {
        StreamingResponseHandler handler = createHandler();
        when(mockContext.getAiMessage()).thenReturn(AiMessage.from("partial"));

        handler.stop();
        handler.stop();

        // Should only be called once
        verify(mockViewController, times(1)).deactivateActivityHandlers();
    }

    @Test
    void stop_withNullWebViewController_doesNotThrow() {
        StreamingResponseHandler handler = createHandlerWithNullWebView();
        when(mockContext.getAiMessage()).thenReturn(AiMessage.from("partial"));

        handler.stop();

        verify(mockChatMemoryService).removeLastMessageByKey("test-memory-key");
    }

    // --- Terminal states (task-234) ---

    @Test
    void stop_setsStoppedTerminalState() {
        StreamingResponseHandler handler = createHandler();

        handler.stop();

        verify(mockViewController).setTerminalState(
                "test-context-id", com.devoxx.genie.ui.compose.model.TerminalState.STOPPED, null);
    }

    @Test
    void stop_setsStoppedStateAfterFinalPartialFlush_andBlocksFurtherPartials() {
        StreamingResponseHandler handler = createHandler();
        handler.onPartialResponse("partial ");  // immediate paint
        handler.onPartialResponse("answer");    // buffered

        handler.stop();

        // The buffered tail is flushed to the UI before the STOPPED marker lands,
        // so the partial text remains visible under the marker.
        var inOrder = inOrder(mockViewController);
        inOrder.verify(mockViewController, atLeastOnce()).updateAiMessageContent(mockContext);
        inOrder.verify(mockViewController).setTerminalState(
                "test-context-id", com.devoxx.genie.ui.compose.model.TerminalState.STOPPED, null);

        // Straggling tokens after stop never reach the view
        clearInvocations(mockViewController);
        handler.onPartialResponse("straggler");
        runPendingFlushes();
        verify(mockViewController, never()).updateAiMessageContent(any());
    }

    @Test
    void stop_calledTwice_setsTerminalStateOnlyOnce() {
        StreamingResponseHandler handler = createHandler();

        handler.stop();
        handler.stop();

        verify(mockViewController, times(1)).setTerminalState(
                "test-context-id", com.devoxx.genie.ui.compose.model.TerminalState.STOPPED, null);
    }

    @Test
    void onError_setsErrorTerminalStateWithUserFacingMessage() {
        promptErrorHandlerMock
                .when(() -> PromptErrorHandler.userFacingMessage(any(Throwable.class)))
                .thenReturn("Provider exploded");
        StreamingResponseHandler handler = createHandler();

        handler.onError(new RuntimeException("Provider exploded with a huge stack trace"));

        verify(mockViewController).setTerminalState(
                "test-context-id", com.devoxx.genie.ui.compose.model.TerminalState.ERROR, "Provider exploded");
    }

    @Test
    void onError_withNullWebViewController_doesNotThrow() {
        StreamingResponseHandler handler = createHandlerWithNullWebView();

        handler.onError(new RuntimeException("Error"));

        assertThat(onErrorCalled.get()).isTrue();
    }
}
