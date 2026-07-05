package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.agent.AgentDefinition;
import com.devoxx.genie.model.agent.AgentResult;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DelegateTaskToolExecutorTest {

    @Mock
    private Project project;
    @Mock
    private DevoxxGenieStateService stateService;

    private List<AgentDefinition> stored;

    @BeforeEach
    void setUp() {
        stored = new ArrayList<>();
        when(stateService.getAgentDefinitions()).thenAnswer(inv -> stored);
        doAnswer(inv -> {
            stored = new ArrayList<>((List<AgentDefinition>) inv.getArgument(0));
            return null;
        }).when(stateService).setAgentDefinitions(anyList());
    }

    private static ToolExecutionRequest request(String arguments) {
        return ToolExecutionRequest.builder().name("delegate_task").arguments(arguments).build();
    }

    @Test
    void parseTasks_readsAgentTaskAndIntent() {
        List<DelegateTaskToolExecutor.DelegatedTask> tasks = DelegateTaskToolExecutor.parseTasks(
                """
                {"tasks": [
                  {"agent": "reviewer", "task": "Review Foo.java", "intent": "review foo"},
                  {"agent": "implementer", "task": "Fix the bug"}
                ]}
                """);

        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).agent()).isEqualTo("reviewer");
        assertThat(tasks.get(0).intent()).isEqualTo("review foo");
        assertThat(tasks.get(1).agent()).isEqualTo("implementer");
        assertThat(tasks.get(1).intent()).isNull();
    }

    @Test
    void parseTasks_skipsMalformedEntries() {
        List<DelegateTaskToolExecutor.DelegatedTask> tasks = DelegateTaskToolExecutor.parseTasks(
                """
                {"tasks": [
                  {"agent": "reviewer"},
                  {"task": "no agent"},
                  {"agent": "", "task": "blank agent"},
                  "not-an-object",
                  {"agent": " architect ", "task": "Design it"}
                ]}
                """);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).agent()).isEqualTo("architect");
    }

    @Test
    void parseTasks_toleratesGarbageInput() {
        assertThat(DelegateTaskToolExecutor.parseTasks(null)).isEmpty();
        assertThat(DelegateTaskToolExecutor.parseTasks("")).isEmpty();
        assertThat(DelegateTaskToolExecutor.parseTasks("not json")).isEmpty();
        assertThat(DelegateTaskToolExecutor.parseTasks("{\"tasks\": \"string\"}")).isEmpty();
        assertThat(DelegateTaskToolExecutor.parseTasks("{}")).isEmpty();
    }

    @Test
    void execute_emptyTasks_returnsError() {
        DelegateTaskToolExecutor executor = new DelegateTaskToolExecutor(project);
        String result = executor.execute(request("{\"tasks\": []}"), null);
        assertThat(result).startsWith("Error:").contains("non-empty");
    }

    @Test
    void execute_unknownAgent_failsFastWithAvailableNames() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

            DelegateTaskToolExecutor executor = new DelegateTaskToolExecutor(project);
            String result = executor.execute(request(
                    "{\"tasks\": [{\"agent\": \"nonexistent\", \"task\": \"do it\"}]}"), null);

            assertThat(result).startsWith("Error:")
                    .contains("nonexistent")
                    .contains("reviewer")     // available-agents list from seeded built-ins
                    .contains("implementer");
        }
    }

    @Test
    void execute_disabledAgent_failsFast() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

            // Seed then disable the reviewer
            DelegateTaskToolExecutor executor = new DelegateTaskToolExecutor(project);
            executor.execute(request("{\"tasks\": [{\"agent\": \"x\", \"task\": \"seed\"}]}"), null);
            stored.stream().filter(d -> d.getName().equals("reviewer")).forEach(d -> d.setEnabled(false));

            String result = executor.execute(request(
                    "{\"tasks\": [{\"agent\": \"reviewer\", \"task\": \"review\"}]}"), null);

            assertThat(result).startsWith("Error:").contains("reviewer");
        }
    }

    @Test
    void execute_orchestratorTarget_isRejected_noSelfDelegation() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

            DelegateTaskToolExecutor executor = new DelegateTaskToolExecutor(project);
            String result = executor.execute(request(
                    "{\"tasks\": [{\"agent\": \"orchestrator\", \"task\": \"coordinate yourself\"}]}"), null);

            assertThat(result).startsWith("Error:").contains("orchestrator");
        }
    }

    @Test
    void formatResults_rendersSummaryOnlyWithStatusMetadata() {
        List<AgentResult> results = List.of(
                AgentResult.ok("reviewer", "review foo", "Looks good. One nit in Foo.java.",
                        7, 12000, "Ollama", "qwen3"),
                AgentResult.timeout("implementer", "fix bug", 120),
                AgentResult.error("documentalist", null, "Agent 'documentalist' failed: no network",
                        0, 100, null, null));

        String report = DelegateTaskToolExecutor.formatResults(results);

        assertThat(report)
                .contains("# Delegation Results")
                .contains("reviewer (Ollama · qwen3)")
                .contains("review foo")
                .contains("Status: ok")
                .contains("7 tool calls")
                .contains("Looks good. One nit in Foo.java.")
                .contains("Status: timeout")
                .contains("timed out after 120s")
                .contains("Status: error")
                .contains("no network");
    }
}
