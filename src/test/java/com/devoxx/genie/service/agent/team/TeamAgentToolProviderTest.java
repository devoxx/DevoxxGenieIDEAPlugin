package com.devoxx.genie.service.agent.team;

import com.devoxx.genie.model.agent.AgentDefinition;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * TASK-242 AC#3: a team agent's tools are the preset resolution intersected with what
 * BuiltInToolProvider actually provides (global disables and feature gates clamp
 * automatically), and delegation tools are structurally excluded.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamAgentToolProviderTest {

    @Mock
    private Project project;
    @Mock
    private DevoxxGenieStateService stateService;

    private static List<String> names(ToolProviderResult result) {
        return result.aiServiceTools().stream()
                .map(tool -> tool.toolSpecification().name())
                .toList();
    }

    @Test
    void readOnlyAgent_getsOnlyReadTools() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

            AgentDefinition reviewer = AgentDefinition.builder()
                    .name("reviewer")
                    .toolsetPresets(List.of("filesystem", "shell")) // presets grant writes...
                    .readOnly(true)                                  // ...readOnly clamps them
                    .build();

            TeamAgentToolProvider provider = new TeamAgentToolProvider(project, reviewer);
            List<String> tools = names(provider.provideTools(null));

            assertThat(tools).contains("read_file", "list_files", "search_files")
                    .doesNotContain("write_file", "edit_file", "run_command", "run_tests",
                            "delegate_task", "parallel_explore");
        }
    }

    @Test
    void globallyDisabledTools_areClampedForChildren() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getDisabledAgentTools()).thenReturn(List.of("search_files"));

            AgentDefinition agent = AgentDefinition.builder()
                    .name("architect")
                    .toolsetPresets(List.of("filesystem-ro"))
                    .build();

            List<String> tools = names(new TeamAgentToolProvider(project, agent).provideTools(null));

            assertThat(tools).contains("read_file", "list_files").doesNotContain("search_files");
        }
    }

    @Test
    void fetchPreset_excludesUnregisteredWebSearch() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            // web_search is feature-gated off (mock returns null) — the preset asks for it,
            // BuiltInToolProvider doesn't provide it, so the intersection drops it.

            AgentDefinition documentalist = AgentDefinition.builder()
                    .name("documentalist")
                    .toolsetPresets(List.of("fetch"))
                    .build();

            List<String> tools = names(new TeamAgentToolProvider(project, documentalist).provideTools(null));

            assertThat(tools).contains("fetch_page")
                    .doesNotContain("web_search", "read_file", "write_file");
        }
    }
}
