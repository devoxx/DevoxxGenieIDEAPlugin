package com.devoxx.genie.service.agent.tool.psi;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
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
class FindDefinitionToolExecutorTest {

    @Mock
    private Project project;

    private FindDefinitionToolExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new FindDefinitionToolExecutor(project);
    }

    // --- Input validation ---

    @Test
    void execute_missingFile_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("find_definition")
                .arguments("{\"line\": 5}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("file");
    }

    @Test
    void execute_blankFile_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("find_definition")
                .arguments("{\"file\": \"   \", \"line\": 5}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("file");
    }

    @Test
    void execute_missingLine_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("find_definition")
                .arguments("{\"file\": \"Foo.java\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("line");
    }

    @Test
    void execute_lineZero_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("find_definition")
                .arguments("{\"file\": \"Foo.java\", \"line\": 0}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("line");
    }

    @Test
    void execute_negativeLineNumber_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("find_definition")
                .arguments("{\"file\": \"Foo.java\", \"line\": -3}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("line");
    }

    @Test
    void execute_invalidJson_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("find_definition")
                .arguments("not json")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error");
    }

    // --- ReadAction exception handling ---

    @SuppressWarnings("unchecked")
    @Test
    void execute_readActionThrows_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("find_definition")
                .arguments("{\"file\": \"Foo.java\", \"line\": 5}")
                .build();

        try (MockedStatic<ReadAction> readActionMock = mockStatic(ReadAction.class)) {
            readActionMock.when(() -> ReadAction.compute(any(ThrowableComputable.class)))
                    .thenThrow(new RuntimeException("PSI lock failed"));

            String result = executor.execute(request, null);
            assertThat(result).contains("Error").contains("Failed to find definition");
        }
    }
}
