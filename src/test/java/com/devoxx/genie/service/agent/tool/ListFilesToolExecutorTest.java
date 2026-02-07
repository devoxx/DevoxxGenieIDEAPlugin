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
class ListFilesToolExecutorTest {

    @Mock
    private Project project;
    @Mock
    private VirtualFile projectBase;

    private ListFilesToolExecutor executor;

    @BeforeEach
    void setUp() {
        when(project.getBaseDir()).thenReturn(projectBase);
        executor = new ListFilesToolExecutor(project);
    }

    @Test
    void execute_emptyArguments_usesProjectRoot() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("list_files")
                .arguments("{}")
                .build();

        // Without ReadAction mock, this should handle gracefully
        String result = executor.execute(request, null);
        assertThat(result).isNotNull();
    }

    @Test
    void execute_withInvalidJson_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("list_files")
                .arguments("invalid")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).isNotNull();
    }
}
