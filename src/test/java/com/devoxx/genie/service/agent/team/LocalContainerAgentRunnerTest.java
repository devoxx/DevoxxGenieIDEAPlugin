package com.devoxx.genie.service.agent.team;

import com.devoxx.genie.model.agent.AgentDefinition;
import com.devoxx.genie.model.agent.AgentResult;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * TASK-251: unit-testable pieces of the local container runner. The docker spawn itself
 * needs a daemon and is covered by manual smoke; these tests pin the env/URL mapping and
 * the guaranteed-readable-result contract on spawn-free failure paths.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocalContainerAgentRunnerTest {

    @Mock
    private Project project;
    @Mock
    private DevoxxGenieStateService stateService;

    private static AgentDefinition ollamaReviewer() {
        return AgentDefinition.builder()
                .name("reviewer")
                .instruction("You review code.")
                .modelProvider("Ollama")
                .modelName("qwen3:8b")
                .toolsetPresets(List.of("filesystem-ro"))
                .readOnly(true)
                .build();
    }

    @Test
    void hostAccessibleUrl_rewritesLoopbackAndTrimsTrailingSlash() {
        assertThat(LocalContainerAgentRunner.hostAccessibleUrl("http://localhost:11434/", "fb"))
                .isEqualTo("http://host.docker.internal:11434");
        assertThat(LocalContainerAgentRunner.hostAccessibleUrl("http://127.0.0.1:1234", "fb"))
                .isEqualTo("http://host.docker.internal:1234");
        assertThat(LocalContainerAgentRunner.hostAccessibleUrl("http://192.168.1.5:8080", "fb"))
                .isEqualTo("http://192.168.1.5:8080");
        assertThat(LocalContainerAgentRunner.hostAccessibleUrl(null, "fallback")).isEqualTo("fallback");
        assertThat(LocalContainerAgentRunner.hostAccessibleUrl("  ", "fallback")).isEqualTo("fallback");
    }

    @Test
    void buildEnv_carriesIdentityTaskAndLocalProviderHost() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:11434/");

            LocalContainerAgentRunner runner = new LocalContainerAgentRunner(
                    project, ollamaReviewer(), "review foo", new AtomicBoolean(false));
            List<String> env = runner.buildEnv("reviewer", "Review Foo.java", "reviewer-abc123", 120);

            assertThat(env).contains(
                    "AGENT_NAME=reviewer",
                    "TASK_PROMPT=Review Foo.java",
                    "SESSION_ID=reviewer-abc123",
                    "NEEDS_REPO=0",
                    "MAX_SESSION_SECONDS=120",
                    "OLLAMA_HOST=http://host.docker.internal:11434");
        }
    }

    @Test
    void execute_cancelledBeforeStart_returnsCancelled() {
        LocalContainerAgentRunner runner = new LocalContainerAgentRunner(
                project, ollamaReviewer(), null, new AtomicBoolean(true));
        assertThat(runner.execute("task").status()).isEqualTo(AgentResult.Status.CANCELLED);
    }

    @Test
    void execute_projectWithoutBasePath_returnsReadableError() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(project.getBasePath()).thenReturn(null);

            AgentResult result = new LocalContainerAgentRunner(
                    project, ollamaReviewer(), null, new AtomicBoolean(false)).execute("task");

            assertThat(result.status()).isEqualTo(AgentResult.Status.ERROR);
            assertThat(result.summary()).contains("base path");
            assertThat(result.provider()).isEqualTo("container");
        }
    }

    @Test
    void gitStatusPorcelain_returnsNullOutsideGitRepo() {
        // /tmp is not a git repo — must degrade to null, never throw
        assertThat(LocalContainerAgentRunner.gitStatusPorcelain("/tmp")).isNull();
    }
}
