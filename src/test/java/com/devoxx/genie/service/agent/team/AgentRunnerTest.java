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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentRunnerTest {

    @Mock
    private Project project;
    @Mock
    private DevoxxGenieStateService stateService;

    private static AgentDefinition definition(String provider) {
        return AgentDefinition.builder()
                .name("reviewer")
                .instruction("You review code.")
                .modelProvider(provider)
                .toolsetPresets(List.of("filesystem-ro"))
                .readOnly(true)
                .build();
    }

    @Test
    void execute_cancelledBeforeStart_returnsCancelledResult() {
        AgentRunner runner = new AgentRunner(project, definition(""), "intent", new AtomicBoolean(true));

        AgentResult result = runner.execute("task");

        assertThat(result.status()).isEqualTo(AgentResult.Status.CANCELLED);
        assertThat(result.agent()).isEqualTo("reviewer");
        assertThat(result.intent()).isEqualTo("intent");
        assertThat(result.summary()).isNotBlank();
    }

    @Test
    void execute_noProviderResolvable_returnsReadableError() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(project.getLocationHash()).thenReturn("hash");
            when(stateService.getSelectedProvider("hash")).thenReturn(null);

            AgentRunner runner = new AgentRunner(project, definition(""), null, new AtomicBoolean(false));
            AgentResult result = runner.execute("task");

            assertThat(result.status()).isEqualTo(AgentResult.Status.ERROR);
            assertThat(result.summary())
                    .contains("Could not create a chat model")
                    .contains("reviewer")
                    .contains("Settings");
        }
    }

    @Test
    void execute_unknownProvider_returnsReadableError() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

            AgentRunner runner = new AgentRunner(project, definition("NoSuchProvider"), null, new AtomicBoolean(false));
            AgentResult result = runner.execute("task");

            assertThat(result.status()).isEqualTo(AgentResult.Status.ERROR);
            assertThat(result.summary()).contains("Could not create a chat model");
        }
    }

    @Test
    void cancel_flagsSharedStateSoSubsequentExecuteShortCircuits() {
        AtomicBoolean shared = new AtomicBoolean(false);
        AgentRunner runner = new AgentRunner(project, definition(""), null, shared);

        runner.cancel();

        assertThat(shared).isTrue();
        assertThat(runner.execute("task").status()).isEqualTo(AgentResult.Status.CANCELLED);
    }

    @Test
    void agentResult_labelIncludesProviderAndModel() {
        assertThat(AgentResult.ok("reviewer", null, "s", 1, 1, "Ollama", "qwen3").label())
                .isEqualTo("reviewer (Ollama · qwen3)");
        assertThat(AgentResult.ok("reviewer", null, "s", 1, 1, "Ollama", null).label())
                .isEqualTo("reviewer (Ollama)");
        assertThat(AgentResult.ok("reviewer", null, "s", 1, 1, null, null).label())
                .isEqualTo("reviewer");
    }
}
