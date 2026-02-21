package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.spec.AcpToolConfig;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.service.acp.protocol.AcpClient;
import com.devoxx.genie.service.cli.CliConsoleManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AcpPromptStrategyTest {

    @Mock private Project mockProject;
    @Mock private ChatMemoryManager mockChatMemoryManager;
    @Mock private ThreadPoolManager mockThreadPoolManager;
    @Mock private MessageCreationService mockMessageCreationService;
    @Mock private ChatMessageContext mockContext;
    @Mock private PromptOutputPanel mockPanel;
    @Mock private PromptTask<PromptResult> mockResultTask;
    @Mock private ExecutorService mockExecutor;
    @Mock private LanguageModel mockLanguageModel;
    @Mock private DevoxxGenieStateService mockStateService;
    @Mock private CliConsoleManager mockConsoleManager;
    @Mock private FileListManager mockFileListManager;

    private MockedStatic<ApplicationManager> applicationManagerMock;
    private MockedStatic<ChatMemoryManager> chatMemoryManagerMock;
    private MockedStatic<ThreadPoolManager> threadPoolManagerMock;
    private MockedStatic<MessageCreationService> messageCreationServiceMock;
    private MockedStatic<DevoxxGenieStateService> stateServiceMock;
    private MockedStatic<CliConsoleManager> consoleManagerMock;
    private MockedStatic<FileListManager> fileListManagerMock;

    private AcpPromptStrategy strategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        applicationManagerMock = mockStatic(ApplicationManager.class);
        chatMemoryManagerMock = mockStatic(ChatMemoryManager.class);
        threadPoolManagerMock = mockStatic(ThreadPoolManager.class);
        messageCreationServiceMock = mockStatic(MessageCreationService.class);
        stateServiceMock = mockStatic(DevoxxGenieStateService.class);
        consoleManagerMock = mockStatic(CliConsoleManager.class);
        fileListManagerMock = mockStatic(FileListManager.class);

        Application mockApplication = mock(Application.class);
        applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(mockApplication);
        when(mockApplication.getService(ChatMemoryManager.class)).thenReturn(mockChatMemoryManager);
        when(mockApplication.getService(ThreadPoolManager.class)).thenReturn(mockThreadPoolManager);
        when(mockApplication.getService(MessageCreationService.class)).thenReturn(mockMessageCreationService);

        chatMemoryManagerMock.when(ChatMemoryManager::getInstance).thenReturn(mockChatMemoryManager);
        threadPoolManagerMock.when(ThreadPoolManager::getInstance).thenReturn(mockThreadPoolManager);
        messageCreationServiceMock.when(MessageCreationService::getInstance).thenReturn(mockMessageCreationService);
        stateServiceMock.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);
        consoleManagerMock.when(() -> CliConsoleManager.getInstance(any(Project.class))).thenReturn(mockConsoleManager);
        fileListManagerMock.when(FileListManager::getInstance).thenReturn(mockFileListManager);

        when(mockThreadPoolManager.getPromptExecutionPool()).thenReturn(mockExecutor);
        when(mockContext.getProject()).thenReturn(mockProject);
        when(mockContext.getUserPrompt()).thenReturn("test prompt");
        when(mockContext.getCreatedOn()).thenReturn(java.time.LocalDateTime.now());
        when(mockContext.getId()).thenReturn("test-id");
        when(mockLanguageModel.getProvider()).thenReturn(ModelProvider.ACPRunners);
        when(mockLanguageModel.getModelName()).thenReturn("Claude");
        when(mockContext.getLanguageModel()).thenReturn(mockLanguageModel);
        when(mockFileListManager.isEmpty(any(Project.class))).thenReturn(true);

        // Set up ChatMemoryService mock
        try {
            java.lang.reflect.Field chatMemoryServiceField = ChatMemoryManager.class.getDeclaredField("chatMemoryService");
            chatMemoryServiceField.setAccessible(true);
            ChatMemoryService mockChatMemoryService = mock(ChatMemoryService.class);
            when(mockChatMemoryService.isEmpty(any(Project.class))).thenReturn(false);
            chatMemoryServiceField.set(mockChatMemoryManager, mockChatMemoryService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        strategy = new AcpPromptStrategy(mockProject);
    }

    @AfterEach
    void tearDown() {
        if (applicationManagerMock != null) applicationManagerMock.close();
        if (chatMemoryManagerMock != null) chatMemoryManagerMock.close();
        if (threadPoolManagerMock != null) threadPoolManagerMock.close();
        if (messageCreationServiceMock != null) messageCreationServiceMock.close();
        if (stateServiceMock != null) stateServiceMock.close();
        if (consoleManagerMock != null) consoleManagerMock.close();
        if (fileListManagerMock != null) fileListManagerMock.close();
    }

    @Test
    void getStrategyName_returnsAcpRunner() {
        assertThat(strategy.getStrategyName()).isEqualTo("ACP Runner");
    }

    @Test
    void executeStrategySpecific_withToolNotFound_completesWithFailure() {
        when(mockStateService.getAcpTools()).thenReturn(Collections.emptyList());

        strategy.executeStrategySpecific(mockContext, mockPanel, mockResultTask);

        verify(mockResultTask).complete(argThat(result -> {
            assertThat(result.getError()).isNotNull();
            assertThat(result.getError().getMessage()).contains("ACP tool not found");
            return true;
        }));
    }

    @Test
    void executeStrategySpecific_withDisabledTool_completesWithFailure() {
        AcpToolConfig disabledTool = AcpToolConfig.builder()
            .name("Claude")
            .enabled(false)
            .build();
        when(mockStateService.getAcpTools()).thenReturn(List.of(disabledTool));

        strategy.executeStrategySpecific(mockContext, mockPanel, mockResultTask);

        verify(mockResultTask).complete(argThat(result -> {
            assertThat(result.getError()).isNotNull();
            assertThat(result.getError().getMessage()).contains("ACP tool not found");
            return true;
        }));
    }

    @Test
    void executeStrategySpecific_withWrongToolName_completesWithFailure() {
        AcpToolConfig wrongTool = AcpToolConfig.builder()
            .name("WrongName")
            .enabled(true)
            .build();
        when(mockStateService.getAcpTools()).thenReturn(List.of(wrongTool));

        strategy.executeStrategySpecific(mockContext, mockPanel, mockResultTask);

        verify(mockResultTask).complete(argThat(result -> {
            assertThat(result.getError()).isNotNull();
            return true;
        }));
    }

    @Test
    void cancel_withoutActiveClient_doesNotThrow() {
        // Verify that cancel() completes without throwing an exception
        assertDoesNotThrow(() -> strategy.cancel());
    }

    @Test
    void cancel_withActiveClient_closesClient() throws Exception {
        AcpClient mockClient = mock(AcpClient.class);

        // Use invokeLater inline
        Application mockApp = mock(Application.class);
        applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(mockApp);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mockApp).invokeLater(any(Runnable.class));

        // Set the activeClient AtomicReference via reflection
        java.lang.reflect.Field clientField = AcpPromptStrategy.class.getDeclaredField("activeClient");
        clientField.setAccessible(true);
        ((java.util.concurrent.atomic.AtomicReference<AcpClient>) clientField.get(strategy)).set(mockClient);

        strategy.cancel();

        verify(mockClient).close();
    }
}
