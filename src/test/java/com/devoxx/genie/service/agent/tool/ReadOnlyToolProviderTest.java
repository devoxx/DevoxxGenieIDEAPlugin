package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReadOnlyToolProviderTest {

    @Mock
    private Project project;

    @Mock
    private ToolProviderRequest providerRequest;

    @Test
    void provideTools_returnsOnlyReadOnlyTools() {
        ReadOnlyToolProvider provider = new ReadOnlyToolProvider(project);
        ToolProviderResult result = provider.provideTools(providerRequest);

        Set<String> toolNames = result.tools().keySet().stream()
                .map(ToolSpecification::name)
                .collect(Collectors.toSet());

        assertThat(toolNames).containsExactlyInAnyOrder("read_file", "list_files", "search_files");
    }

    @Test
    void provideTools_doesNotIncludeWriteTools() {
        ReadOnlyToolProvider provider = new ReadOnlyToolProvider(project);
        ToolProviderResult result = provider.provideTools(providerRequest);

        Set<String> toolNames = result.tools().keySet().stream()
                .map(ToolSpecification::name)
                .collect(Collectors.toSet());

        assertThat(toolNames).doesNotContain("write_file", "edit_file", "run_command", "parallel_explore");
    }

    @Test
    void provideTools_returnsThreeTools() {
        ReadOnlyToolProvider provider = new ReadOnlyToolProvider(project);
        ToolProviderResult result = provider.provideTools(providerRequest);

        assertThat(result.tools()).hasSize(3);
    }
}
