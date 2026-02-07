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
class SearchFilesToolExecutorTest {

    @Mock
    private Project project;
    @Mock
    private VirtualFile projectBase;

    private SearchFilesToolExecutor executor;

    @BeforeEach
    void setUp() {
        when(project.getBaseDir()).thenReturn(projectBase);
        executor = new SearchFilesToolExecutor(project);
    }

    @Test
    void execute_missingPattern_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("search_files")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("pattern");
    }

    @Test
    void execute_invalidRegex_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("search_files")
                .arguments("{\"pattern\": \"[invalid\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("Invalid regex");
    }

    @Test
    void execute_emptyPattern_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("search_files")
                .arguments("{\"pattern\": \"\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("pattern");
    }
}
