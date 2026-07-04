package com.devoxx.genie.service.agent;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * TASK-244: pure-coordinator mode strips direct write/run tools from the orchestrating
 * conversation while keeping read/search tools and delegate_task.
 */
class CoordinatorToolFilterTest {

    private static ToolProvider providerWith(String... toolNames) {
        return request -> {
            ToolProviderResult.Builder builder = ToolProviderResult.builder();
            for (String name : toolNames) {
                builder.add(ToolSpecification.builder().name(name).build(), mock(ToolExecutor.class));
            }
            return builder.build();
        };
    }

    private static List<String> toolNames(ToolProviderResult result) {
        return result.aiServiceTools().stream()
                .map(tool -> tool.toolSpecification().name())
                .toList();
    }

    @Test
    void stripsWriteAndRunTools_keepsReadAndDelegate() {
        ToolProvider delegate = providerWith(
                "read_file", "list_files", "search_files", "write_file", "edit_file",
                "run_command", "run_tests", "delegate_task", "fetch_page");

        AgentToolProviderFactory.CoordinatorToolFilter filter =
                new AgentToolProviderFactory.CoordinatorToolFilter(delegate);

        List<String> names = toolNames(filter.provideTools(null));

        assertThat(names)
                .contains("read_file", "list_files", "search_files", "delegate_task", "fetch_page")
                .doesNotContain("write_file", "edit_file", "run_command", "run_tests");
    }

    @Test
    void passesThroughWhenNothingBlocked() {
        ToolProvider delegate = providerWith("read_file", "delegate_task");

        AgentToolProviderFactory.CoordinatorToolFilter filter =
                new AgentToolProviderFactory.CoordinatorToolFilter(delegate);

        assertThat(toolNames(filter.provideTools(null)))
                .containsExactly("read_file", "delegate_task");
    }
}
