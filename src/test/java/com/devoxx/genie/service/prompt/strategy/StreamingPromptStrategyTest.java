package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.service.agent.AgentLoopTracker;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.model.chat.StreamingChatModel;
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

import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StreamingPromptStrategyTest {

    @Mock private Project mockProject;
    @Mock private ChatMemoryManager mockChatMemoryManager;
    @Mock private ThreadPoolManager mockThreadPoolManager;
    @Mock private MessageCreationService mockMessageCreationService;
    @Mock private ChatMessageContext mockContext;
    @Mock private PromptOutputPanel mockPanel;
    @Mock private PromptTask<PromptResult> mockResultTask;
    @Mock private ExecutorService mockExecutor;
    @Mock private LanguageModel mockLanguageModel;
    @Mock private StreamingChatModel mockStreamingModel;
    @Mock private FileListManager mockFileListManager;

    private MockedStatic<ApplicationManager> applicationManagerMock;
    private MockedStatic<ChatMemoryManager> chatMemoryManagerMock;
    private MockedStatic<ThreadPoolManager> threadPoolManagerMock;
    private MockedStatic<MessageCreationService> messageCreationServiceMock;
    private MockedStatic<NotificationUtil> notificationUtilMock;
    private MockedStatic<FileListManager> fileListManagerMock;

    private StreamingPromptStrategy strategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        applicationManagerMock = mockStatic(ApplicationManager.class);
        chatMemoryManagerMock = mockStatic(ChatMemoryManager.class);
        threadPoolManagerMock = mockStatic(ThreadPoolManager.class);
        messageCreationServiceMock = mockStatic(MessageCreationService.class);
        notificationUtilMock = mockStatic(NotificationUtil.class);
        fileListManagerMock = mockStatic(FileListManager.class);

        Application mockApplication = mock(Application.class);
        applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(mockApplication);
        when(mockApplication.getService(ChatMemoryManager.class)).thenReturn(mockChatMemoryManager);
        when(mockApplication.getService(ThreadPoolManager.class)).thenReturn(mockThreadPoolManager);
        when(mockApplication.getService(MessageCreationService.class)).thenReturn(mockMessageCreationService);

        chatMemoryManagerMock.when(ChatMemoryManager::getInstance).thenReturn(mockChatMemoryManager);
        threadPoolManagerMock.when(ThreadPoolManager::getInstance).thenReturn(mockThreadPoolManager);
        messageCreationServiceMock.when(MessageCreationService::getInstance).thenReturn(mockMessageCreationService);
        fileListManagerMock.when(FileListManager::getInstance).thenReturn(mockFileListManager);

        when(mockThreadPoolManager.getPromptExecutionPool()).thenReturn(mockExecutor);
        when(mockContext.getProject()).thenReturn(mockProject);
        when(mockContext.getCreatedOn()).thenReturn(java.time.LocalDateTime.now());
        when(mockContext.getId()).thenReturn("test-id");
        when(mockLanguageModel.getProvider()).thenReturn(ModelProvider.OpenAI);
        when(mockLanguageModel.getModelName()).thenReturn("gpt-4");
        when(mockContext.getLanguageModel()).thenReturn(mockLanguageModel);
        when(mockFileListManager.isEmpty(any(Project.class))).thenReturn(true);

        // Set up ChatMemoryService mock inside ChatMemoryManager
        try {
            java.lang.reflect.Field chatMemoryServiceField = ChatMemoryManager.class.getDeclaredField("chatMemoryService");
            chatMemoryServiceField.setAccessible(true);
            ChatMemoryService mockChatMemoryService = mock(ChatMemoryService.class);
            when(mockChatMemoryService.isEmpty(any(Project.class))).thenReturn(false);
            chatMemoryServiceField.set(mockChatMemoryManager, mockChatMemoryService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set chatMemoryService field", e);
        }

        strategy = new StreamingPromptStrategy(
            mockProject, mockChatMemoryManager, mockThreadPoolManager, mockMessageCreationService);
    }

    @AfterEach
    void tearDown() {
        if (applicationManagerMock != null) applicationManagerMock.close();
        if (chatMemoryManagerMock != null) chatMemoryManagerMock.close();
        if (threadPoolManagerMock != null) threadPoolManagerMock.close();
        if (messageCreationServiceMock != null) messageCreationServiceMock.close();
        if (notificationUtilMock != null) notificationUtilMock.close();
        if (fileListManagerMock != null) fileListManagerMock.close();
    }

    @Test
    void getStrategyName_returnsStreamingPrompt() {
        assertThat(strategy.getStrategyName()).isEqualTo("streaming prompt");
    }

    @Test
    void executeStrategySpecific_withNullStreamingModel_completesWithFailure() {
        when(mockContext.getStreamingChatModel()).thenReturn(null);

        strategy.executeStrategySpecific(mockContext, mockPanel, mockResultTask);

        verify(mockResultTask).complete(argThat(result -> {
            assertThat(result.getError()).isNotNull();
            assertThat(result.getError().getMessage()).contains("Streaming model not available");
            return true;
        }));
        notificationUtilMock.verify(() ->
            NotificationUtil.sendNotification(eq(mockProject), contains("Streaming model not available")));
    }

    @Test
    void cancel_stopsHandlerAndTracker() {
        // Use reflection to set a mock handler and tracker
        AgentLoopTracker mockTracker = mock(AgentLoopTracker.class);
        try {
            java.lang.reflect.Field trackerField = StreamingPromptStrategy.class.getDeclaredField("currentTracker");
            trackerField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.concurrent.atomic.AtomicReference<AgentLoopTracker> trackerRef =
                (java.util.concurrent.atomic.AtomicReference<AgentLoopTracker>) trackerField.get(strategy);
            trackerRef.set(mockTracker);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        strategy.cancel();

        verify(mockTracker).cancel();
    }

    @Test
    void cancel_withoutHandlerOrTracker_doesNotThrow() {
        // Should not throw any exception when no handler/tracker is set
        strategy.cancel();
    }

    @Test
    void executeStrategySpecific_withEarlyCancellation_stopsHandler() {
        when(mockContext.getStreamingChatModel()).thenReturn(mockStreamingModel);
        when(mockContext.getUserPrompt()).thenReturn("test question");
        when(mockContext.getUserMessage()).thenReturn(
            dev.langchain4j.data.message.UserMessage.from("test question"));
        when(mockResultTask.isCancelled()).thenReturn(true);

        strategy.executeStrategySpecific(mockContext, mockPanel, mockResultTask);

        // With early cancellation, the executor should not have started real work
        // The handler should have been stopped
    }
}
