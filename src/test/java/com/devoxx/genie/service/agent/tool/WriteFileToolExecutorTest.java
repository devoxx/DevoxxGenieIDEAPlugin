package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
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
class WriteFileToolExecutorTest {

    @Mock
    private Project project;
    @Mock
    private VirtualFile projectBase;

    private WriteFileToolExecutor executor;

    @BeforeEach
    void setUp() {
        when(project.getBaseDir()).thenReturn(projectBase);
        executor = new WriteFileToolExecutor(project);
    }

    @Test
    void execute_missingPath_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("{\"content\": \"hello\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path");
    }

    @Test
    void execute_missingContent_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("{\"path\": \"test.txt\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("content");
    }

    @Test
    void execute_pathTraversal_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("{\"path\": \"../../../etc/passwd\", \"content\": \"pwned\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path traversal");
    }

    @Test
    void execute_pathWithDoubleDotsInMiddle_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("{\"path\": \"src/../../secret.txt\", \"content\": \"data\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path traversal");
    }
}
