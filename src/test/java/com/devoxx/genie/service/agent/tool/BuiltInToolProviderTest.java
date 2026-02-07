package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuiltInToolProviderTest {

    @Mock
    private Project project;
    @Mock
    private VirtualFile projectBase;
    @Mock
    private ToolProviderRequest request;

    private BuiltInToolProvider provider;

    @BeforeEach
    void setUp() {
        when(project.getBaseDir()).thenReturn(projectBase);
        when(project.getBasePath()).thenReturn("/tmp/test-project");
        provider = new BuiltInToolProvider(project);
    }

    @Test
    void provideTools_returnsSixTools() {
        ToolProviderResult result = provider.provideTools(request);

        assertThat(result.tools()).hasSize(6);
    }

    @Test
    void provideTools_containsExpectedToolNames() {
        ToolProviderResult result = provider.provideTools(request);

        Set<String> toolNames = result.tools().keySet().stream()
                .map(ToolSpecification::name)
                .collect(Collectors.toSet());

        assertThat(toolNames).containsExactlyInAnyOrder(
                "read_file", "write_file", "edit_file", "list_files", "search_files", "run_command"
        );
    }

    @Test
    void provideTools_allToolsHaveDescriptions() {
        ToolProviderResult result = provider.provideTools(request);

        for (ToolSpecification spec : result.tools().keySet()) {
            assertThat(spec.description()).isNotNull().isNotBlank();
        }
    }

    @Test
    void provideTools_allToolsHaveParameters() {
        ToolProviderResult result = provider.provideTools(request);

        for (ToolSpecification spec : result.tools().keySet()) {
            assertThat(spec.parameters()).isNotNull();
        }
    }

    @Test
    void provideTools_allToolsHaveExecutors() {
        ToolProviderResult result = provider.provideTools(request);

        for (var executor : result.tools().values()) {
            assertThat(executor).isNotNull();
        }
    }
}
