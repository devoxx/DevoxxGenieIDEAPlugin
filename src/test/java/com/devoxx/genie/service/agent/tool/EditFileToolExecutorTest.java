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
class EditFileToolExecutorTest {

    @Mock
    private Project project;
    @Mock
    private VirtualFile projectBase;

    private EditFileToolExecutor executor;

    @BeforeEach
    void setUp() {
        when(project.getBaseDir()).thenReturn(projectBase);
        executor = new EditFileToolExecutor(project);
    }

    @Test
    void execute_missingPath_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"old_string\": \"foo\", \"new_string\": \"bar\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path");
    }

    @Test
    void execute_missingOldString_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"test.txt\", \"new_string\": \"bar\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("old_string");
    }

    @Test
    void execute_missingNewString_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"test.txt\", \"old_string\": \"foo\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("new_string");
    }

    @Test
    void execute_emptyOldString_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"test.txt\", \"old_string\": \"\", \"new_string\": \"bar\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("old_string").contains("empty");
    }

    @Test
    void execute_identicalStrings_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"test.txt\", \"old_string\": \"same\", \"new_string\": \"same\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("identical");
    }

    @Test
    void execute_pathTraversal_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"../../../etc/passwd\", \"old_string\": \"foo\", \"new_string\": \"bar\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path traversal");
    }

    @Test
    void execute_pathWithDoubleDotsInMiddle_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"src/../../secret.txt\", \"old_string\": \"foo\", \"new_string\": \"bar\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path traversal");
    }
}
