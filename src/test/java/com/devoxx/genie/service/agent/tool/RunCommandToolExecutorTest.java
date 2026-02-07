package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RunCommandToolExecutorTest {

    @Mock
    private Project project;

    private RunCommandToolExecutor executor;

    @BeforeEach
    void setUp() {
        when(project.getBasePath()).thenReturn(System.getProperty("java.io.tmpdir"));
        executor = new RunCommandToolExecutor(project);
    }

    @Test
    void execute_missingCommand_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("command");
    }

    @Test
    void execute_emptyCommand_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("command");
    }

    @Test
    void execute_echoCommand_returnsOutput() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo hello\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("hello");
    }

    @Test
    void execute_nonZeroExitCode_returnsExitCode() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"exit 1\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Exit code: 1");
    }
}
