package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.service.agent.tool.psi.PsiToolCatalog;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuiltInToolProviderTest {

    private static final Set<String> BASE_TOOLS = Set.of(
            "read_file", "write_file", "edit_file", "list_files",
            "search_files", "run_command", "fetch_page"
    );

    @Mock
    private Project project;
    @Mock
    private ToolProviderRequest request;
    @Mock
    private DevoxxGenieStateService stateService;

    private MockedStatic<DevoxxGenieStateService> stateServiceMock;

    @BeforeEach
    void setUp() {
        stateServiceMock = mockStatic(DevoxxGenieStateService.class);
        stateServiceMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        when(project.getBasePath()).thenReturn("/tmp/test-project");
    }

    @AfterEach
    void tearDown() {
        stateServiceMock.close();
    }

    private BuiltInToolProvider createProvider() {
        return new BuiltInToolProvider(project);
    }

    private Set<String> getToolNames(ToolProviderResult result) {
        return result.tools().keySet().stream()
                .map(ToolSpecification::name)
                .collect(Collectors.toSet());
    }

    // --- Default configuration (all optional features disabled) ---

    @Test
    void provideTools_defaultConfig_returnsSevenBaseTools() {
        BuiltInToolProvider provider = createProvider();
        ToolProviderResult result = provider.provideTools(request);

        assertThat(result.tools()).hasSize(7);
        assertThat(getToolNames(result)).containsExactlyInAnyOrderElementsOf(BASE_TOOLS);
    }

    @Test
    void provideTools_allToolsHaveDescriptions() {
        BuiltInToolProvider provider = createProvider();
        ToolProviderResult result = provider.provideTools(request);

        for (ToolSpecification spec : result.tools().keySet()) {
            assertThat(spec.description()).isNotNull().isNotBlank();
        }
    }

    @Test
    void provideTools_allToolsHaveParameters() {
        BuiltInToolProvider provider = createProvider();
        ToolProviderResult result = provider.provideTools(request);

        for (ToolSpecification spec : result.tools().keySet()) {
            assertThat(spec.parameters()).isNotNull();
        }
    }

    @Test
    void provideTools_allToolsHaveExecutors() {
        BuiltInToolProvider provider = createProvider();
        ToolProviderResult result = provider.provideTools(request);

        for (var executor : result.tools().values()) {
            assertThat(executor).isNotNull();
        }
    }

    // --- Test execution feature flag ---

    @Test
    void provideTools_testExecutionEnabled_includesRunTests() {
        when(stateService.getTestExecutionEnabled()).thenReturn(true);
        BuiltInToolProvider provider = createProvider();

        ToolProviderResult result = provider.provideTools(request);

        assertThat(result.tools()).hasSize(8);
        assertThat(getToolNames(result)).contains("run_tests");
    }

    @Test
    void provideTools_testExecutionDisabled_excludesRunTests() {
        when(stateService.getTestExecutionEnabled()).thenReturn(false);
        BuiltInToolProvider provider = createProvider();

        ToolProviderResult result = provider.provideTools(request);

        assertThat(getToolNames(result)).doesNotContain("run_tests");
    }

    // --- Parallel explore feature flag ---

    @Test
    void provideTools_parallelExploreEnabled_includesParallelExplore() {
        when(stateService.getParallelExploreEnabled()).thenReturn(true);
        BuiltInToolProvider provider = createProvider();

        ToolProviderResult result = provider.provideTools(request);

        assertThat(result.tools()).hasSize(8);
        assertThat(getToolNames(result)).contains("parallel_explore");
    }

    @Test
    void provideTools_parallelExploreDisabled_excludesParallelExplore() {
        when(stateService.getParallelExploreEnabled()).thenReturn(false);
        BuiltInToolProvider provider = createProvider();

        ToolProviderResult result = provider.provideTools(request);

        assertThat(getToolNames(result)).doesNotContain("parallel_explore");
    }

    @Test
    void getParallelExploreExecutor_enabled_returnsExecutor() {
        when(stateService.getParallelExploreEnabled()).thenReturn(true);
        BuiltInToolProvider provider = createProvider();

        assertThat(provider.getParallelExploreExecutor()).isNotNull();
        assertThat(provider.getParallelExploreExecutor()).isInstanceOf(ParallelExploreToolExecutor.class);
    }

    @Test
    void getParallelExploreExecutor_disabled_returnsNull() {
        when(stateService.getParallelExploreEnabled()).thenReturn(false);
        BuiltInToolProvider provider = createProvider();

        assertThat(provider.getParallelExploreExecutor()).isNull();
    }

    // --- Backlog (SDD) feature flag ---

    @Test
    void provideTools_specBrowserEnabled_includesBacklogTools() {
        when(stateService.getSpecBrowserEnabled()).thenReturn(true);
        BuiltInToolProvider provider = createProvider();

        ToolProviderResult result = provider.provideTools(request);

        Set<String> toolNames = getToolNames(result);
        // 7 base + 20 backlog (10 task + 5 document + 5 milestone)
        assertThat(result.tools()).hasSize(27);
        assertThat(toolNames).contains(
                "backlog_task_create", "backlog_task_list", "backlog_task_search",
                "backlog_task_view", "backlog_task_edit", "backlog_task_complete", "backlog_task_archive",
                "backlog_task_archive_done", "backlog_task_unarchive", "backlog_task_list_archived",
                "backlog_document_list", "backlog_document_view", "backlog_document_create",
                "backlog_document_update", "backlog_document_search",
                "backlog_milestone_list", "backlog_milestone_add", "backlog_milestone_rename",
                "backlog_milestone_remove", "backlog_milestone_archive"
        );
    }

    @Test
    void provideTools_specBrowserDisabled_excludesBacklogTools() {
        when(stateService.getSpecBrowserEnabled()).thenReturn(false);
        BuiltInToolProvider provider = createProvider();

        ToolProviderResult result = provider.provideTools(request);

        Set<String> toolNames = getToolNames(result);
        assertThat(toolNames).noneMatch(name -> name.startsWith("backlog_"));
    }

    // --- PSI tools feature flag ---

    @Test
    void provideTools_psiToolsEnabled_includesPsiTools() {
        when(stateService.getPsiToolsEnabled()).thenReturn(true);
        BuiltInToolProvider provider = createProvider();

        ToolProviderResult result = provider.provideTools(request);

        Set<String> toolNames = getToolNames(result);
        // 7 base + 9 PSI tools
        assertThat(result.tools()).hasSize(16);
        assertThat(toolNames).contains(
                "find_symbols", "document_symbols", "find_references",
                "find_definition", "find_implementations",
                "find_callees", "trace_call_chains", "calculate_complexity", "find_dead_code"
        );
    }

    @Test
    void provideTools_psiToolsDisabled_excludesPsiTools() {
        when(stateService.getPsiToolsEnabled()).thenReturn(false);
        BuiltInToolProvider provider = createProvider();

        ToolProviderResult result = provider.provideTools(request);

        Set<String> toolNames = getToolNames(result);
        assertThat(toolNames).doesNotContainAnyElementsOf(PsiToolCatalog.toolNames());
    }

    /**
     * Regression guard for fine-grained PSI tool control: the settings UI builds one
     * checkbox per entry in {@link PsiToolCatalog}, so every PSI tool actually registered
     * by the provider MUST appear in the catalog — otherwise a newly added PSI tool would
     * have no settings toggle. This fails the moment a PSI tool is registered without being
     * listed in the catalog (or vice-versa).
     */
    @Test
    void psiToolCatalog_listsExactlyTheRegisteredPsiTools() {
        when(stateService.getPsiToolsEnabled()).thenReturn(true);
        BuiltInToolProvider provider = createProvider();

        Set<String> registeredPsiTools = new java.util.HashSet<>(getToolNames(provider.provideTools(request)));
        registeredPsiTools.removeAll(BASE_TOOLS); // only PSI tools remain (no other features enabled)

        assertThat(PsiToolCatalog.toolNames())
                .containsExactlyInAnyOrderElementsOf(registeredPsiTools);
    }

    /**
     * Each PSI tool must be individually disableable via the shared disabledAgentTools list
     * (the mechanism the per-tool settings checkboxes write to). Disabling one PSI tool must
     * exclude only that tool and leave the rest registered.
     */
    @Test
    void provideTools_disablingSinglePsiTool_excludesOnlyThatTool() {
        when(stateService.getPsiToolsEnabled()).thenReturn(true);
        when(stateService.getDisabledAgentTools()).thenReturn(List.of("find_dead_code"));
        BuiltInToolProvider provider = createProvider();

        Set<String> toolNames = getToolNames(provider.provideTools(request));

        assertThat(toolNames).doesNotContain("find_dead_code");
        assertThat(toolNames).contains(
                "find_symbols", "document_symbols", "find_references", "find_definition",
                "find_implementations", "find_callees", "trace_call_chains", "calculate_complexity");
    }

    // --- All features enabled ---

    @Test
    void provideTools_allFeaturesEnabled_includesAllTools() {
        when(stateService.getTestExecutionEnabled()).thenReturn(true);
        when(stateService.getParallelExploreEnabled()).thenReturn(true);
        when(stateService.getSpecBrowserEnabled()).thenReturn(true);
        when(stateService.getPsiToolsEnabled()).thenReturn(true);
        BuiltInToolProvider provider = createProvider();

        ToolProviderResult result = provider.provideTools(request);

        // 7 base + 1 run_tests + 1 parallel_explore + 20 backlog + 9 PSI = 38
        assertThat(result.tools()).hasSize(38);
    }

    // --- Disabled tools filtering in provideTools() ---

    @Test
    void provideTools_withDisabledTools_filtersThemOut() {
        when(stateService.getDisabledAgentTools()).thenReturn(List.of("read_file", "write_file"));
        BuiltInToolProvider provider = createProvider();

        ToolProviderResult result = provider.provideTools(request);

        Set<String> toolNames = getToolNames(result);
        assertThat(result.tools()).hasSize(5);
        assertThat(toolNames).doesNotContain("read_file", "write_file");
        assertThat(toolNames).contains("edit_file", "list_files", "search_files", "run_command", "fetch_page");
    }

    @Test
    void provideTools_withNullDisabledTools_returnsAllTools() {
        when(stateService.getDisabledAgentTools()).thenReturn(null);
        BuiltInToolProvider provider = createProvider();

        ToolProviderResult result = provider.provideTools(request);

        assertThat(result.tools()).hasSize(7);
    }

    @Test
    void provideTools_withEmptyDisabledTools_returnsAllTools() {
        when(stateService.getDisabledAgentTools()).thenReturn(List.of());
        BuiltInToolProvider provider = createProvider();

        ToolProviderResult result = provider.provideTools(request);

        assertThat(result.tools()).hasSize(7);
    }

    @Test
    void provideTools_disableNonExistentTool_returnsAllTools() {
        when(stateService.getDisabledAgentTools()).thenReturn(List.of("nonexistent_tool"));
        BuiltInToolProvider provider = createProvider();

        ToolProviderResult result = provider.provideTools(request);

        assertThat(result.tools()).hasSize(7);
    }

    @Test
    void provideTools_disableAllTools_returnsEmpty() {
        when(stateService.getDisabledAgentTools()).thenReturn(List.of(
                "read_file", "write_file", "edit_file", "list_files",
                "search_files", "run_command", "fetch_page"
        ));
        BuiltInToolProvider provider = createProvider();

        ToolProviderResult result = provider.provideTools(request);

        assertThat(result.tools()).isEmpty();
    }
}
