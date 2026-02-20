package com.devoxx.genie.service.prompt.response.nonstreaming;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.agent.AgentToolProviderFactory;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonStreamingPromptExecutionServiceTest {

    @Mock private Project mockProject;
    @Mock private ChatMemoryManager mockChatMemoryManager;
    @Mock private ThreadPoolManager mockThreadPoolManager;
    @Mock private ChatMessageContext mockContext;
    @Mock private ChatModel mockChatModel;
    @Mock private ChatMemory mockChatMemory;
    @Mock private ExecutorService mockExecutor;
    @Mock private DevoxxGenieStateService mockStateService;
    @Mock private FileListManager mockFileListManager;
    @Mock private LanguageModel mockLanguageModel;

    private MockedStatic<ApplicationManager> applicationManagerMock;
    private MockedStatic<ChatMemoryManager> chatMemoryManagerMock;
    private MockedStatic<ThreadPoolManager> threadPoolManagerMock;
    private MockedStatic<DevoxxGenieStateService> stateServiceMock;
    private MockedStatic<FileListManager> fileListManagerMock;
    private MockedStatic<MCPService> mcpServiceMock;
    private MockedStatic<AgentToolProviderFactory> agentToolProviderMock;
    private MockedStatic<PromptErrorHandler> promptErrorHandlerMock;
    private MockedStatic<ChatMessageContextUtil> contextUtilMock;

    private NonStreamingPromptExecutionService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        applicationManagerMock = mockStatic(ApplicationManager.class);
        chatMemoryManagerMock = mockStatic(ChatMemoryManager.class);
        threadPoolManagerMock = mockStatic(ThreadPoolManager.class);
        stateServiceMock = mockStatic(DevoxxGenieStateService.class);
        fileListManagerMock = mockStatic(FileListManager.class);
        mcpServiceMock = mockStatic(MCPService.class);
        agentToolProviderMock = mockStatic(AgentToolProviderFactory.class);
        promptErrorHandlerMock = mockStatic(PromptErrorHandler.class);
        contextUtilMock = mockStatic(ChatMessageContextUtil.class);

        Application mockApplication = mock(Application.class);
        applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(mockApplication);
        when(mockApplication.getService(NonStreamingPromptExecutionService.class)).thenReturn(null);

        chatMemoryManagerMock.when(ChatMemoryManager::getInstance).thenReturn(mockChatMemoryManager);
        threadPoolManagerMock.when(ThreadPoolManager::getInstance).thenReturn(mockThreadPoolManager);
        stateServiceMock.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);
        fileListManagerMock.when(FileListManager::getInstance).thenReturn(mockFileListManager);
        mcpServiceMock.when(MCPService::isMCPEnabled).thenReturn(false);
        agentToolProviderMock.when(() -> AgentToolProviderFactory.createToolProvider(any())).thenReturn(null);
        contextUtilMock.when(() -> ChatMessageContextUtil.hasMultimodalContent(any())).thenReturn(false);

        when(mockContext.getProject()).thenReturn(mockProject);
        when(mockContext.getChatModel()).thenReturn(mockChatModel);
        when(mockContext.getUserMessage()).thenReturn(UserMessage.from("test question"));
        when(mockContext.getTimeout()).thenReturn(60);
        when(mockProject.getLocationHash()).thenReturn("test-project-hash");
        when(mockChatMemoryManager.getChatMemory("test-project-hash")).thenReturn(mockChatMemory);
        when(mockStateService.getAgentModeEnabled()).thenReturn(false);
        when(mockStateService.getSystemPrompt()).thenReturn("You are a helpful assistant");
        when(mockFileListManager.isEmpty(any(Project.class))).thenReturn(true);
        when(mockLanguageModel.getProvider()).thenReturn(ModelProvider.OpenAI);
        when(mockContext.getLanguageModel()).thenReturn(mockLanguageModel);

        // Execute tasks synchronously
        when(mockThreadPoolManager.getPromptExecutionPool()).thenReturn(mockExecutor);
        doAnswer(invocation -> {
            // Run inline
            CompletableFuture.runAsync(() -> {
                ((Runnable) invocation.getArgument(0)).run();
            }).join();
            return null;
        }).when(mockExecutor).execute(any(Runnable.class));

        // Use direct executor for supplyAsync
        doAnswer(invocation -> {
            return CompletableFuture.supplyAsync(
                invocation.getArgument(0),
                Runnable::run  // Direct executor
            );
        }).when(mockExecutor).submit(any(java.util.concurrent.Callable.class));

        service = new NonStreamingPromptExecutionService();
    }

    @AfterEach
    void tearDown() {
        if (applicationManagerMock != null) applicationManagerMock.close();
        if (chatMemoryManagerMock != null) chatMemoryManagerMock.close();
        if (threadPoolManagerMock != null) threadPoolManagerMock.close();
        if (stateServiceMock != null) stateServiceMock.close();
        if (fileListManagerMock != null) fileListManagerMock.close();
        if (mcpServiceMock != null) mcpServiceMock.close();
        if (agentToolProviderMock != null) agentToolProviderMock.close();
        if (promptErrorHandlerMock != null) promptErrorHandlerMock.close();
        if (contextUtilMock != null) contextUtilMock.close();
    }

    @Test
    void isRunning_initiallyFalse() {
        assertThat(service.isRunning()).isFalse();
    }

    @Test
    void cancelExecutingQuery_withNoActiveQuery_doesNotThrow() {
        service.cancelExecutingQuery();
        // Should not throw
    }

    @Test
    void cancelExecutingQuery_withActiveQuery_cancelsIt() {
        // Create an incomplete future and set it via reflection
        CompletableFuture<ChatResponse> incompleteFuture = new CompletableFuture<>();
        try {
            var field = NonStreamingPromptExecutionService.class.getDeclaredField("currentQueryFuture");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var ref = (java.util.concurrent.atomic.AtomicReference<CompletableFuture<ChatResponse>>) field.get(service);
            ref.set(incompleteFuture);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        service.cancelExecutingQuery();

        assertThat(incompleteFuture.isCancelled()).isTrue();
    }

    @Test
    void cancelExecutingQuery_withMCPEnabled_clearsMCPCache() {
        mcpServiceMock.when(MCPService::isMCPEnabled).thenReturn(true);

        // Create an incomplete future
        CompletableFuture<ChatResponse> incompleteFuture = new CompletableFuture<>();
        try {
            var field = NonStreamingPromptExecutionService.class.getDeclaredField("currentQueryFuture");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var ref = (java.util.concurrent.atomic.AtomicReference<CompletableFuture<ChatResponse>>) field.get(service);
            ref.set(incompleteFuture);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Need MCPExecutionService mock
        try (var mcpExecMock = mockStatic(com.devoxx.genie.service.mcp.MCPExecutionService.class)) {
            var mockMcpExec = mock(com.devoxx.genie.service.mcp.MCPExecutionService.class);
            mcpExecMock.when(com.devoxx.genie.service.mcp.MCPExecutionService::getInstance).thenReturn(mockMcpExec);

            service.cancelExecutingQuery();

            verify(mockMcpExec).clearClientCache();
        }
    }

    @Test
    void cancelExecutingQuery_withCompletedFuture_doesNotCancel() {
        CompletableFuture<ChatResponse> completedFuture = CompletableFuture.completedFuture(null);
        try {
            var field = NonStreamingPromptExecutionService.class.getDeclaredField("currentQueryFuture");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var ref = (java.util.concurrent.atomic.AtomicReference<CompletableFuture<ChatResponse>>) field.get(service);
            ref.set(completedFuture);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        service.cancelExecutingQuery();

        // Already done, no cancellation should happen
        assertThat(completedFuture.isCancelled()).isFalse();
    }
}
