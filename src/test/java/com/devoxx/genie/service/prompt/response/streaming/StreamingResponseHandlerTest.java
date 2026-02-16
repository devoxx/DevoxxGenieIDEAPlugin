package com.devoxx.genie.service.prompt.response.streaming;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.webview.ConversationWebViewController;
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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
    @Mock private ConversationWebViewController mockWebViewController;
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
        when(mockFileListManager.isEmpty(any(Project.class))).thenReturn(true);

        // Mock message bus
        when(mockProject.getMessageBus()).thenReturn(mockMessageBus);
        var mockPublisher = mock(com.devoxx.genie.ui.listener.ConversationEventListener.class);
        when(mockMessageBus.syncPublisher(AppTopics.CONVERSATION_TOPIC)).thenReturn(mockPublisher);

        completedResponse.set(null);
        capturedError.set(null);
        onCompleteCalled.set(false);
        onErrorCalled.set(false);
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
        return createHandler(mockWebViewController);
    }

    private StreamingResponseHandler createHandlerWithNullWebView() {
        return createHandler(null);
    }

    private StreamingResponseHandler createHandler(ConversationWebViewController webViewController) {
        Consumer<ChatResponse> onComplete = response -> {
            completedResponse.set(response);
            onCompleteCalled.set(true);
        };
        Consumer<Throwable> onError = error -> {
            capturedError.set(error);
            onErrorCalled.set(true);
        };
        return new StreamingResponseHandler(mockContext, webViewController, onComplete, onError);
    }

    @Test
    void onPartialResponse_accumulatesTokens() {
        StreamingResponseHandler handler = createHandler();

        handler.onPartialResponse("Hello");
        handler.onPartialResponse(" World");

        verify(mockContext, atLeast(2)).setAiMessage(any(AiMessage.class));
        verify(mockWebViewController, atLeast(2)).updateAiMessageContent(mockContext);
    }

    @Test
    void onPartialResponse_whenStopped_doesNothing() {
        StreamingResponseHandler handler = createHandler();
        handler.stop();

        handler.onPartialResponse("Should be ignored");

        verify(mockWebViewController, never()).updateAiMessageContent(any());
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
        verify(mockWebViewController, atLeast(2)).updateAiMessageContent(mockContext);
    }

    @Test
    void onCompleteResponse_withoutPartialResponse_addsChatMessage() {
        StreamingResponseHandler handler = createHandler();

        AiMessage aiMessage = AiMessage.from("Complete response");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();
        handler.onCompleteResponse(response);

        verify(mockWebViewController).addChatMessage(mockContext);
    }

    @Test
    void onCompleteResponse_marksLogsAsCompleted() {
        StreamingResponseHandler handler = createHandler();
        AiMessage aiMessage = AiMessage.from("Response");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();

        handler.onCompleteResponse(response);

        verify(mockWebViewController).markMCPLogsAsCompleted("test-context-id");
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
        when(mockFileListManager.isEmpty(mockProject)).thenReturn(false);
        when(mockFileListManager.getFiles(mockProject)).thenReturn(new java.util.ArrayList<>());

        StreamingResponseHandler handler = createHandler();
        AiMessage aiMessage = AiMessage.from("Response");
        ChatResponse response = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();

        handler.onCompleteResponse(response);

        verify(mockWebViewController).addFileReferences(eq(mockContext), any());
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

        verify(mockWebViewController).hideLoadingIndicator("test-context-id");
    }

    @Test
    void onError_deactivatesActivityHandlers() {
        StreamingResponseHandler handler = createHandler();

        handler.onError(new RuntimeException("Error"));

        verify(mockWebViewController).deactivateActivityHandlers();
    }

    @Test
    void stop_setsStoppedFlag() {
        StreamingResponseHandler handler = createHandler();

        handler.stop();

        // Subsequent calls should be ignored
        handler.onPartialResponse("Should be ignored");
        verify(mockWebViewController, never()).updateAiMessageContent(any());
    }

    @Test
    void stop_deactivatesActivityHandlers() {
        StreamingResponseHandler handler = createHandler();

        handler.stop();

        verify(mockWebViewController).deactivateActivityHandlers();
    }

    @Test
    void stop_hidesLoadingIndicator() {
        StreamingResponseHandler handler = createHandler();

        handler.stop();

        verify(mockWebViewController).hideLoadingIndicator("test-context-id");
    }

    @Test
    void stop_removesPartialResponseFromMemory() {
        StreamingResponseHandler handler = createHandler();
        when(mockContext.getAiMessage()).thenReturn(AiMessage.from("partial"));

        handler.stop();

        verify(mockChatMemoryService).removeLastMessage(mockProject);
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
        verify(mockWebViewController, times(1)).deactivateActivityHandlers();
    }

    @Test
    void stop_withNullWebViewController_doesNotThrow() {
        StreamingResponseHandler handler = createHandlerWithNullWebView();
        when(mockContext.getAiMessage()).thenReturn(AiMessage.from("partial"));

        handler.stop();

        verify(mockChatMemoryService).removeLastMessage(mockProject);
    }
}
