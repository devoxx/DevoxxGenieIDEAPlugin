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
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
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

import java.util.List;
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
        agentToolProviderMock.when(() -> AgentToolProviderFactory.createToolProvider(any(), any())).thenReturn(null);
        contextUtilMock.when(() -> ChatMessageContextUtil.hasMultimodalContent(any())).thenReturn(false);

        when(mockContext.getProject()).thenReturn(mockProject);
        when(mockContext.getChatModel()).thenReturn(mockChatModel);
        when(mockContext.getUserMessage()).thenReturn(UserMessage.from("test question"));
        when(mockContext.getMemoryKey()).thenReturn("test-project-hash");
        when(mockContext.getTimeout()).thenReturn(60);
        when(mockProject.getLocationHash()).thenReturn("test-project-hash");
        when(mockChatMemoryManager.getChatMemory("test-project-hash")).thenReturn(mockChatMemory);
        when(mockStateService.getAgentModeEnabled()).thenReturn(false);
        when(mockStateService.getSystemPrompt()).thenReturn("You are a helpful assistant");
        when(mockFileListManager.isEmpty(any(Project.class))).thenReturn(true);
        when(mockFileListManager.isEmpty(any(Project.class), any())).thenReturn(true);
        when(mockLanguageModel.getProvider()).thenReturn(ModelProvider.OpenAI);
        when(mockContext.getLanguageModel()).thenReturn(mockLanguageModel);

        // Execute tasks synchronously
        when(mockThreadPoolManager.getPromptExecutionPool()).thenReturn(mockExecutor);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
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
    void executeQuery_withoutToolsUsesChatModelDirectlySoThinkingIsPreserved() throws Exception {
        AiMessage aiMessage = AiMessage.builder()
            .thinking("I should reason first.")
            .text("The answer is 42.")
            .build();
        ChatResponse modelResponse = ChatResponse.builder()
            .aiMessage(aiMessage)
            .build();
        when(mockChatMemory.messages()).thenReturn(List.<ChatMessage>of(UserMessage.from("test question")));
        when(mockChatModel.chat(anyList())).thenReturn(modelResponse);

        ChatResponse response = service.executeQuery(mockContext).get();

        assertThat(response.aiMessage().thinking()).isEqualTo("I should reason first.");
        assertThat(response.aiMessage().text()).isEqualTo("The answer is 42.");
        verify(mockChatMemory).add(mockContext.getUserMessage());
        verify(mockChatModel).chat(anyList());
        verify(mockChatMemory).add(aiMessage);
    }

    @Test
    void executeQuery_withToolProviderPreservesThinking() throws Exception {
        // Agent/MCP mode routes through AiServices; the response must still carry
        // the model's thinking so the UI can render the thinking bubble.
        dev.langchain4j.service.tool.ToolProvider mockToolProvider =
            mock(dev.langchain4j.service.tool.ToolProvider.class);
        when(mockToolProvider.provideTools(any()))
            .thenReturn(dev.langchain4j.service.tool.ToolProviderResult.builder().build());
        agentToolProviderMock.when(() -> AgentToolProviderFactory.createToolProvider(any(), any()))
            .thenReturn(mockToolProvider);
        chatMemoryManagerMock.when(() -> ChatMemoryManager.buildAugmentedSystemPrompt(any()))
            .thenReturn("You are a helpful assistant");

        AiMessage aiMessage = AiMessage.builder()
            .thinking("I should reason first.")
            .text("The answer is 42.")
            .build();
        when(mockChatModel.chat(any(dev.langchain4j.model.chat.request.ChatRequest.class)))
            .thenReturn(ChatResponse.builder().aiMessage(aiMessage).build());
        when(mockChatMemory.messages()).thenReturn(List.<ChatMessage>of(UserMessage.from("test question")));

        ChatResponse response = service.executeQuery(mockContext).get();

        assertThat(response.aiMessage().thinking()).isEqualTo("I should reason first.");
        assertThat(response.aiMessage().text()).isEqualTo("The answer is 42.");
    }

    @Test
    void cancelExecutingQuery_withNoActiveQuery_doesNotThrow() {
        service.cancelExecutingQuery();
        // Should not throw
    }

    @Test
    void cancelExecutingQuery_withActiveQuery_cancelsIt() {
        // Create an incomplete future and put it in the queryFutures map via reflection
        CompletableFuture<ChatResponse> incompleteFuture = new CompletableFuture<>();
        try {
            var field = NonStreamingPromptExecutionService.class.getDeclaredField("queryFutures");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var map = (java.util.Map<String, CompletableFuture<ChatResponse>>) field.get(service);
            map.put("test-tab", incompleteFuture);
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
            var field = NonStreamingPromptExecutionService.class.getDeclaredField("queryFutures");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var map = (java.util.Map<String, CompletableFuture<ChatResponse>>) field.get(service);
            map.put("test-tab", incompleteFuture);
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

    // ---------- Hang/timeout reproduction (issue: MCP-enabled prompts had no wall-clock cap) ----------

    /** Simple prompt: caller's per-request timeout wins. */
    @Test
    void resolveTimeoutSeconds_simplePrompt_returnsRequestedTimeout() {
        assertThat(NonStreamingPromptExecutionService.resolveTimeoutSeconds(45, false, 999))
                .isEqualTo(45L);
    }

    /** Simple prompt with null/non-positive request falls back to the simple-prompt default (60s). */
    @Test
    void resolveTimeoutSeconds_simplePrompt_nullRequested_returnsDefault60() {
        assertThat(NonStreamingPromptExecutionService.resolveTimeoutSeconds(null, false, 999))
                .isEqualTo(60L);
        assertThat(NonStreamingPromptExecutionService.resolveTimeoutSeconds(0, false, 999))
                .isEqualTo(60L);
    }

    /** Agent/MCP: agent cap is used regardless of the (short) per-request timeout. */
    @Test
    void resolveTimeoutSeconds_agentOrMcp_returnsAgentCap() {
        assertThat(NonStreamingPromptExecutionService.resolveTimeoutSeconds(45, true, 240))
                .isEqualTo(240L);
    }

    /** Agent/MCP with null/non-positive cap falls back to AGENT_MAX_EXECUTION_SECONDS (300). */
    @Test
    void resolveTimeoutSeconds_agentOrMcp_nullCap_returnsDefault300() {
        assertThat(NonStreamingPromptExecutionService.resolveTimeoutSeconds(45, true, null))
                .isEqualTo(300L);
        assertThat(NonStreamingPromptExecutionService.resolveTimeoutSeconds(45, true, 0))
                .isEqualTo(300L);
    }

    /**
     * Demonstrates the fix at the JDK-API level: applying the resolved timeout via {@code orTimeout}
     * makes a hanging future terminate. The bug was that MCP/agent flows skipped {@code orTimeout}
     * entirely; the production fix unconditionally calls {@code orTimeout(resolveTimeoutSeconds(...), SECONDS)}.
     */
    @Test
    void orTimeout_appliedToMcpAgentFlow_terminatesHangingFuture() throws Exception {
        long capSeconds = NonStreamingPromptExecutionService.resolveTimeoutSeconds(
                /* requested */ 60, /* isAgentOrMcp */ true, /* cap */ 1);
        assertThat(capSeconds).isEqualTo(1L);

        // Dedicated executor so we can shutdownNow() and interrupt the sleeping worker;
        // CompletableFuture#cancel doesn't propagate interrupts to its supplier.
        java.util.concurrent.ExecutorService exec = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "test-hang-worker");
            t.setDaemon(true);
            return t;
        });
        try {
            CompletableFuture<String> hanging = CompletableFuture.supplyAsync(() -> {
                try { Thread.sleep(30_000); }
                catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                return "should never see this";
            }, exec);

            long start = System.nanoTime();
            CompletableFuture<String> guarded = hanging.orTimeout(capSeconds, java.util.concurrent.TimeUnit.SECONDS);
            Throwable err = null;
            try { guarded.get(5, java.util.concurrent.TimeUnit.SECONDS); }
            catch (java.util.concurrent.ExecutionException e) { err = e.getCause(); }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertThat(err).isInstanceOf(java.util.concurrent.TimeoutException.class);
            // Should fire close to the cap, well before the 30s hang would complete.
            assertThat(elapsedMs).isLessThan(3_000L);
        } finally {
            exec.shutdownNow();
            exec.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    @Test
    void cancelExecutingQuery_withCompletedFuture_doesNotCancel() {
        CompletableFuture<ChatResponse> completedFuture = CompletableFuture.completedFuture(null);
        try {
            var field = NonStreamingPromptExecutionService.class.getDeclaredField("queryFutures");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var map = (java.util.Map<String, CompletableFuture<ChatResponse>>) field.get(service);
            map.put("test-tab", completedFuture);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        service.cancelExecutingQuery();

        // Already done, no cancellation should happen
        assertThat(completedFuture.isCancelled()).isFalse();
    }
}
