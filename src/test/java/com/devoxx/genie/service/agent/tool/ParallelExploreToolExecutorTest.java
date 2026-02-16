package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.service.agent.SubAgentRunner;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParallelExploreToolExecutorTest {

    @Mock
    private Project project;
    @Mock
    private DevoxxGenieStateService stateService;
    @Mock
    private ThreadPoolManager threadPoolManager;
    @Mock
    private Application application;
    @Mock
    private MessageBus messageBus;

    private MockedStatic<DevoxxGenieStateService> stateServiceMock;
    private MockedStatic<ThreadPoolManager> threadPoolManagerMock;
    private MockedStatic<ApplicationManager> applicationManagerMock;

    private ParallelExploreToolExecutor executor;

    @BeforeEach
    void setUp() {
        stateServiceMock = mockStatic(DevoxxGenieStateService.class);
        stateServiceMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        threadPoolManagerMock = mockStatic(ThreadPoolManager.class);
        threadPoolManagerMock.when(ThreadPoolManager::getInstance).thenReturn(threadPoolManager);

        applicationManagerMock = mockStatic(ApplicationManager.class);
        applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(application);
        when(application.getMessageBus()).thenReturn(messageBus);

        // Default settings
        when(stateService.getSubAgentParallelism()).thenReturn(3);
        when(stateService.getSubAgentTimeoutSeconds()).thenReturn(120);
        when(stateService.getAgentDebugLogsEnabled()).thenReturn(false);

        when(project.isDisposed()).thenReturn(false);

        executor = new ParallelExploreToolExecutor(project);
    }

    @AfterEach
    void tearDown() {
        stateServiceMock.close();
        threadPoolManagerMock.close();
        applicationManagerMock.close();
    }

    // --- Argument validation tests ---

    @Test
    void execute_emptyQueries_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("parallel_explore")
                .arguments("{\"queries\": []}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("queries");
    }

    @Test
    void execute_missingQueriesKey_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("parallel_explore")
                .arguments("{\"other\": \"value\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("queries");
    }

    @Test
    void execute_invalidJson_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("parallel_explore")
                .arguments("not json")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("queries");
    }

    @Test
    void execute_nullQueries_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("parallel_explore")
                .arguments("{\"queries\": null}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("queries");
    }

    // --- Successful execution tests ---

    @Test
    void execute_singleQuery_launchesOneSubAgent() {
        ExecutorService directExecutor = Executors.newSingleThreadExecutor();
        when(threadPoolManager.getSubAgentPool()).thenReturn(directExecutor);

        try (MockedConstruction<SubAgentRunner> mocked = mockConstruction(SubAgentRunner.class,
                (runner, context) -> when(runner.execute(any())).thenReturn("Found relevant code in Main.java"))) {

            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("parallel_explore")
                    .arguments("{\"queries\": [\"find main entry point\"]}")
                    .build();

            String result = executor.execute(request, null);

            assertThat(result).contains("Parallel Exploration Results");
            assertThat(result).contains("Sub-Agent #1");
            assertThat(result).contains("find main entry point");
            assertThat(result).contains("Found relevant code in Main.java");
            assertThat(mocked.constructed()).hasSize(1);
        } finally {
            directExecutor.shutdownNow();
        }
    }

    @Test
    void execute_multipleQueries_launchesMultipleSubAgents() {
        ExecutorService directExecutor = Executors.newFixedThreadPool(3);
        when(threadPoolManager.getSubAgentPool()).thenReturn(directExecutor);

        AtomicInteger constructionCount = new AtomicInteger(0);
        try (MockedConstruction<SubAgentRunner> mocked = mockConstruction(SubAgentRunner.class,
                (runner, context) -> {
                    int index = constructionCount.getAndIncrement();
                    when(runner.execute(any())).thenReturn("Result from agent " + (index + 1));
                })) {

            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("parallel_explore")
                    .arguments("{\"queries\": [\"query A\", \"query B\", \"query C\"]}")
                    .build();

            String result = executor.execute(request, null);

            assertThat(result).contains("Sub-Agent #1").contains("query A");
            assertThat(result).contains("Sub-Agent #2").contains("query B");
            assertThat(result).contains("Sub-Agent #3").contains("query C");
            assertThat(mocked.constructed()).hasSize(3);
        } finally {
            directExecutor.shutdownNow();
        }
    }

    // --- Parallelism cap tests ---

    @Test
    void execute_queriesExceedMaxParallelism_capsToMax() {
        when(stateService.getSubAgentParallelism()).thenReturn(2);

        ExecutorService directExecutor = Executors.newFixedThreadPool(2);
        when(threadPoolManager.getSubAgentPool()).thenReturn(directExecutor);

        try (MockedConstruction<SubAgentRunner> mocked = mockConstruction(SubAgentRunner.class,
                (runner, context) -> when(runner.execute(any())).thenReturn("Result"))) {

            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("parallel_explore")
                    .arguments("{\"queries\": [\"q1\", \"q2\", \"q3\", \"q4\", \"q5\"]}")
                    .build();

            String result = executor.execute(request, null);

            // Only 2 sub-agents should be created (capped by maxParallelism=2)
            assertThat(mocked.constructed()).hasSize(2);
            assertThat(result).contains("Sub-Agent #1").contains("q1");
            assertThat(result).contains("Sub-Agent #2").contains("q2");
            assertThat(result).doesNotContain("Sub-Agent #3");
        } finally {
            directExecutor.shutdownNow();
        }
    }

    @Test
    void execute_nullParallelismSetting_usesDefault() {
        when(stateService.getSubAgentParallelism()).thenReturn(null);

        ExecutorService directExecutor = Executors.newFixedThreadPool(3);
        when(threadPoolManager.getSubAgentPool()).thenReturn(directExecutor);

        try (MockedConstruction<SubAgentRunner> mocked = mockConstruction(SubAgentRunner.class,
                (runner, context) -> when(runner.execute(any())).thenReturn("Result"))) {

            // Default parallelism is 3, send 5 queries
            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("parallel_explore")
                    .arguments("{\"queries\": [\"q1\", \"q2\", \"q3\", \"q4\", \"q5\"]}")
                    .build();

            String result = executor.execute(request, null);

            // Should be capped at default (3)
            assertThat(mocked.constructed()).hasSize(3);
        } finally {
            directExecutor.shutdownNow();
        }
    }

    // --- Timeout handling tests ---

    @Test
    void execute_subAgentTimesOut_returnsTimeoutMessage() {
        when(stateService.getSubAgentTimeoutSeconds()).thenReturn(1);

        ExecutorService directExecutor = Executors.newSingleThreadExecutor();
        when(threadPoolManager.getSubAgentPool()).thenReturn(directExecutor);

        try (MockedConstruction<SubAgentRunner> ignored = mockConstruction(SubAgentRunner.class,
                (runner, context) -> when(runner.execute(any())).thenAnswer(inv -> {
                    // Simulate a long-running sub-agent
                    Thread.sleep(5000);
                    return "Should not reach here";
                }))) {

            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("parallel_explore")
                    .arguments("{\"queries\": [\"slow query\"]}")
                    .build();

            String result = executor.execute(request, null);

            assertThat(result).contains("Sub-Agent #1");
            assertThat(result).contains("timed out");
        } finally {
            directExecutor.shutdownNow();
        }
    }

    @Test
    void execute_nullTimeoutSetting_usesDefault() {
        when(stateService.getSubAgentTimeoutSeconds()).thenReturn(null);

        ExecutorService directExecutor = Executors.newSingleThreadExecutor();
        when(threadPoolManager.getSubAgentPool()).thenReturn(directExecutor);

        try (MockedConstruction<SubAgentRunner> mocked = mockConstruction(SubAgentRunner.class,
                (runner, context) -> when(runner.execute(any())).thenReturn("Result"))) {

            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("parallel_explore")
                    .arguments("{\"queries\": [\"query\"]}")
                    .build();

            String result = executor.execute(request, null);

            // Should succeed with default timeout (120s)
            assertThat(result).contains("Result");
        } finally {
            directExecutor.shutdownNow();
        }
    }

    // --- Error handling tests ---

    @Test
    void execute_subAgentThrowsException_returnsErrorMessage() {
        ExecutorService directExecutor = Executors.newSingleThreadExecutor();
        when(threadPoolManager.getSubAgentPool()).thenReturn(directExecutor);

        try (MockedConstruction<SubAgentRunner> ignored = mockConstruction(SubAgentRunner.class,
                (runner, context) -> when(runner.execute(any()))
                        .thenThrow(new RuntimeException("Model connection failed")))) {

            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("parallel_explore")
                    .arguments("{\"queries\": [\"failing query\"]}")
                    .build();

            String result = executor.execute(request, null);

            assertThat(result).contains("Sub-Agent #1");
            assertThat(result).contains("error");
            assertThat(result).contains("Model connection failed");
        } finally {
            directExecutor.shutdownNow();
        }
    }

    @Test
    void execute_mixedSuccessAndFailure_returnsAllResults() {
        when(stateService.getSubAgentParallelism()).thenReturn(3);

        ExecutorService directExecutor = Executors.newFixedThreadPool(3);
        when(threadPoolManager.getSubAgentPool()).thenReturn(directExecutor);

        AtomicInteger mixedCount = new AtomicInteger(0);
        try (MockedConstruction<SubAgentRunner> mocked = mockConstruction(SubAgentRunner.class,
                (runner, context) -> {
                    int index = mixedCount.getAndIncrement();
                    if (index == 1) {
                        when(runner.execute(any()))
                                .thenThrow(new RuntimeException("Agent 2 failed"));
                    } else {
                        when(runner.execute(any())).thenReturn("Success from agent " + (index + 1));
                    }
                })) {

            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("parallel_explore")
                    .arguments("{\"queries\": [\"q1\", \"q2\", \"q3\"]}")
                    .build();

            String result = executor.execute(request, null);

            assertThat(result).contains("Sub-Agent #1").contains("Success from agent 1");
            assertThat(result).contains("Sub-Agent #2").contains("error").contains("Agent 2 failed");
            assertThat(result).contains("Sub-Agent #3").contains("Success from agent 3");
        } finally {
            directExecutor.shutdownNow();
        }
    }

    // --- Cancellation tests ---

    @Test
    void cancel_setsFlag_andCancelsActiveRunners() {
        ExecutorService directExecutor = Executors.newSingleThreadExecutor();
        when(threadPoolManager.getSubAgentPool()).thenReturn(directExecutor);

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch blocked = new CountDownLatch(1);

        try (MockedConstruction<SubAgentRunner> mocked = mockConstruction(SubAgentRunner.class,
                (runner, context) -> when(runner.execute(any())).thenAnswer(inv -> {
                    started.countDown();
                    blocked.await(10, TimeUnit.SECONDS);
                    return "done";
                }))) {

            // Run execute in a separate thread so we can cancel during execution
            Future<?> executeFuture = Executors.newSingleThreadExecutor().submit(() -> {
                ToolExecutionRequest request = ToolExecutionRequest.builder()
                        .name("parallel_explore")
                        .arguments("{\"queries\": [\"long running query\"]}")
                        .build();
                executor.execute(request, null);
            });

            try {
                started.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Cancel
            executor.cancel();

            // Verify cancel was called on the runner
            if (!mocked.constructed().isEmpty()) {
                verify(mocked.constructed().get(0)).cancel();
            }

            blocked.countDown();
            executeFuture.cancel(true);
        } finally {
            directExecutor.shutdownNow();
        }
    }

    // --- Result formatting tests ---

    @Test
    void execute_formatsResultsWithHeaders() {
        ExecutorService directExecutor = Executors.newSingleThreadExecutor();
        when(threadPoolManager.getSubAgentPool()).thenReturn(directExecutor);

        try (MockedConstruction<SubAgentRunner> ignored = mockConstruction(SubAgentRunner.class,
                (runner, context) -> when(runner.execute(any())).thenReturn("Some findings here"))) {

            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("parallel_explore")
                    .arguments("{\"queries\": [\"find error handling\"]}")
                    .build();

            String result = executor.execute(request, null);

            assertThat(result).startsWith("# Parallel Exploration Results");
            assertThat(result).contains("## Sub-Agent #1: find error handling");
            assertThat(result).contains("Some findings here");
            assertThat(result).contains("---");
        } finally {
            directExecutor.shutdownNow();
        }
    }

    // --- Event publishing tests ---

    @Test
    void execute_withDebugLogsEnabled_publishesEvents() {
        when(stateService.getAgentDebugLogsEnabled()).thenReturn(true);
        when(project.getLocationHash()).thenReturn("test-hash");

        // Mock the message bus publisher
        var agentLogger = mock(com.devoxx.genie.service.agent.AgentLoggingMessage.class);
        when(messageBus.syncPublisher(any())).thenReturn(agentLogger);

        ExecutorService directExecutor = Executors.newSingleThreadExecutor();
        when(threadPoolManager.getSubAgentPool()).thenReturn(directExecutor);

        try (MockedConstruction<SubAgentRunner> ignored = mockConstruction(SubAgentRunner.class,
                (runner, context) -> when(runner.execute(any())).thenReturn("Result"))) {

            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("parallel_explore")
                    .arguments("{\"queries\": [\"test query\"]}")
                    .build();

            executor.execute(request, null);

            // Should have published events (started + sub-agent started + sub-agent completed)
            verify(agentLogger, atLeastOnce()).onAgentLoggingMessage(any());
        } finally {
            directExecutor.shutdownNow();
        }
    }

    @Test
    void execute_withDebugLogsDisabled_doesNotPublish() {
        when(stateService.getAgentDebugLogsEnabled()).thenReturn(false);

        ExecutorService directExecutor = Executors.newSingleThreadExecutor();
        when(threadPoolManager.getSubAgentPool()).thenReturn(directExecutor);

        try (MockedConstruction<SubAgentRunner> ignored = mockConstruction(SubAgentRunner.class,
                (runner, context) -> when(runner.execute(any())).thenReturn("Result"))) {

            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("parallel_explore")
                    .arguments("{\"queries\": [\"test query\"]}")
                    .build();

            executor.execute(request, null);

            // Should not attempt to publish
            verify(messageBus, never()).syncPublisher(any());
        } finally {
            directExecutor.shutdownNow();
        }
    }

    @Test
    void execute_projectDisposed_doesNotPublish() {
        when(project.isDisposed()).thenReturn(true);

        ExecutorService directExecutor = Executors.newSingleThreadExecutor();
        when(threadPoolManager.getSubAgentPool()).thenReturn(directExecutor);

        try (MockedConstruction<SubAgentRunner> ignored = mockConstruction(SubAgentRunner.class,
                (runner, context) -> when(runner.execute(any())).thenReturn("Result"))) {

            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("parallel_explore")
                    .arguments("{\"queries\": [\"test query\"]}")
                    .build();

            executor.execute(request, null);

            // Should not attempt to publish when project is disposed
            verify(messageBus, never()).syncPublisher(any());
        } finally {
            directExecutor.shutdownNow();
        }
    }

    // --- ToolArgumentParser tests (kept from original) ---

    @Test
    void getStringArray_validJsonArray_returnsList() {
        String json = "{\"queries\": [\"find error handling\", \"find logging patterns\", \"find test utilities\"]}";
        List<String> result = ToolArgumentParser.getStringArray(json, "queries");

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo("find error handling");
        assertThat(result.get(1)).isEqualTo("find logging patterns");
        assertThat(result.get(2)).isEqualTo("find test utilities");
    }

    @Test
    void getStringArray_emptyArray_returnsEmptyList() {
        String json = "{\"queries\": []}";
        List<String> result = ToolArgumentParser.getStringArray(json, "queries");
        assertThat(result).isEmpty();
    }

    @Test
    void getStringArray_missingKey_returnsEmptyList() {
        String json = "{\"other\": \"value\"}";
        List<String> result = ToolArgumentParser.getStringArray(json, "queries");
        assertThat(result).isEmpty();
    }

    @Test
    void getStringArray_nullValue_returnsEmptyList() {
        String json = "{\"queries\": null}";
        List<String> result = ToolArgumentParser.getStringArray(json, "queries");
        assertThat(result).isEmpty();
    }

    @Test
    void getStringArray_invalidJson_returnsEmptyList() {
        List<String> result = ToolArgumentParser.getStringArray("not json", "queries");
        assertThat(result).isEmpty();
    }

    @Test
    void getStringArray_arrayWithNulls_skipsNulls() {
        String json = "{\"queries\": [\"first\", null, \"third\"]}";
        List<String> result = ToolArgumentParser.getStringArray(json, "queries");
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("first", "third");
    }

    @Test
    void getStringArray_singleElement_returnsSingletonList() {
        String json = "{\"queries\": [\"single query\"]}";
        List<String> result = ToolArgumentParser.getStringArray(json, "queries");
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("single query");
    }

    @Test
    void getStringArray_notAnArray_returnsEmptyList() {
        String json = "{\"queries\": \"not an array\"}";
        List<String> result = ToolArgumentParser.getStringArray(json, "queries");
        assertThat(result).isEmpty();
    }
}
