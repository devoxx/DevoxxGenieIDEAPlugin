package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReadFileToolExecutorTest {

    @Mock
    private Project project;
    @Mock
    private VirtualFile projectBase;
    @Mock
    private VirtualFile targetFile;

    private ReadFileToolExecutor executor;

    @BeforeEach
    void setUp() {
        when(project.getBaseDir()).thenReturn(projectBase);
        executor = new ReadFileToolExecutor(project);
    }

    @Test
    void execute_missingPath_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("read_file")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path");
    }

    @Test
    void execute_emptyPath_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("read_file")
                .arguments("{\"path\": \"\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path");
    }

    @Test
    void execute_invalidJson_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("read_file")
                .arguments("not json")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error");
    }
}
