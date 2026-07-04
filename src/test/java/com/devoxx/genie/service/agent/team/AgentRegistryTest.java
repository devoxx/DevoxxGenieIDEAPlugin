package com.devoxx.genie.service.agent.team;

import com.devoxx.genie.model.agent.AgentDefinition;
import com.devoxx.genie.model.agent.AgentToolsetPreset;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentRegistryTest {

    @Mock
    private DevoxxGenieStateService stateService;

    /** Simulates the state service persistence: setAgentDefinitions stores, getter returns. */
    private List<AgentDefinition> stored;

    @BeforeEach
    void setUp() {
        stored = new ArrayList<>();
        when(stateService.getAgentDefinitions()).thenAnswer(inv -> stored);
        doAnswer(inv -> {
            stored = new ArrayList<>((List<AgentDefinition>) inv.getArgument(0));
            return null;
        }).when(stateService).setAgentDefinitions(anyList());
    }

    private MockedStatic<DevoxxGenieStateService> mockState() {
        MockedStatic<DevoxxGenieStateService> mocked = mockStatic(DevoxxGenieStateService.class);
        mocked.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
        return mocked;
    }

    @Test
    void getAll_seedsBuiltInsExactlyOnce() {
        try (MockedStatic<DevoxxGenieStateService> ignored = mockState()) {
            List<AgentDefinition> first = AgentRegistry.getInstance().getAll();

            assertThat(first).extracting(AgentDefinition::getName)
                    .containsExactly("architect", "implementer", "reviewer", "documentalist");
            assertThat(first).allMatch(AgentDefinition::isBuiltIn);
            assertThat(first).allMatch(AgentDefinition::isEnabled);

            // Second call must not re-seed (stored list unchanged, no duplicate entries)
            List<AgentDefinition> second = AgentRegistry.getInstance().getAll();
            assertThat(second).hasSize(4);
            verify(stateService, times(1)).setAgentDefinitions(anyList());
        }
    }

    @Test
    void getAll_returnsDefensiveCopies() {
        try (MockedStatic<DevoxxGenieStateService> ignored = mockState()) {
            List<AgentDefinition> all = AgentRegistry.getInstance().getAll();
            all.get(0).setDescription("mutated");
            all.get(0).getToolsetPresets().add("shell");

            List<AgentDefinition> fresh = AgentRegistry.getInstance().getAll();
            assertThat(fresh.get(0).getDescription()).isNotEqualTo("mutated");
            assertThat(fresh.get(0).getToolsetPresets()).doesNotContain("shell");
        }
    }

    @Test
    void byName_findsCaseInsensitively_andMissesUnknown() {
        try (MockedStatic<DevoxxGenieStateService> ignored = mockState()) {
            assertThat(AgentRegistry.getInstance().byName("Reviewer")).isPresent();
            assertThat(AgentRegistry.getInstance().byName("  implementer ")).isPresent();
            assertThat(AgentRegistry.getInstance().byName("nonexistent")).isEmpty();
            assertThat(AgentRegistry.getInstance().byName(null)).isEmpty();
            assertThat(AgentRegistry.getInstance().byName("")).isEmpty();
        }
    }

    @Test
    void getEnabled_excludesDisabledAgents() {
        try (MockedStatic<DevoxxGenieStateService> ignored = mockState()) {
            List<AgentDefinition> all = AgentRegistry.getInstance().getAll();
            all.stream().filter(d -> d.getName().equals("reviewer")).forEach(d -> d.setEnabled(false));
            stateService.setAgentDefinitions(all);

            assertThat(AgentRegistry.getInstance().getEnabled())
                    .extracting(AgentDefinition::getName)
                    .doesNotContain("reviewer")
                    .contains("architect", "implementer", "documentalist");
        }
    }

    @Test
    void buildCatalogPrompt_rendersEnabledAgentsWithModelLabels() {
        try (MockedStatic<DevoxxGenieStateService> ignored = mockState()) {
            List<AgentDefinition> all = AgentRegistry.getInstance().getAll();
            all.stream().filter(d -> d.getName().equals("reviewer")).forEach(d -> {
                d.setModelProvider("Ollama");
                d.setModelName("qwen3");
            });
            all.stream().filter(d -> d.getName().equals("documentalist")).forEach(d -> d.setEnabled(false));
            stateService.setAgentDefinitions(all);

            String catalog = AgentRegistry.getInstance().buildCatalogPrompt();

            assertThat(catalog)
                    .contains("delegate_task")
                    .contains("| reviewer |")
                    .contains("Ollama · qwen3")
                    .contains("conversation model")
                    .doesNotContain("documentalist");
        }
    }

    @Test
    void buildOrchestratorInstruction_containsMandateAndCatalog() {
        try (MockedStatic<DevoxxGenieStateService> ignored = mockState()) {
            String instruction = AgentRegistry.getInstance().buildOrchestratorInstruction();
            assertThat(instruction)
                    .contains("CORE MANDATE")
                    .contains("ONE-SHOT")
                    .contains("| implementer |");
        }
    }

    @Test
    void resetBuiltIn_restoresShippedPersona() {
        try (MockedStatic<DevoxxGenieStateService> ignored = mockState()) {
            List<AgentDefinition> all = AgentRegistry.getInstance().getAll();
            all.stream().filter(d -> d.getName().equals("reviewer")).forEach(d -> {
                d.setInstruction("edited persona");
                d.setModelProvider("OpenAI");
            });
            stateService.setAgentDefinitions(all);

            AgentRegistry.getInstance().resetBuiltIn("reviewer");

            AgentDefinition reviewer = AgentRegistry.getInstance().byName("reviewer").orElseThrow();
            assertThat(reviewer.getInstruction()).contains("senior code reviewer");
            assertThat(reviewer.getModelProvider()).isEmpty();
            assertThat(reviewer.isReadOnly()).isTrue();
        }
    }

    @Test
    void saveAll_persistsValidListAndRejectsInvalidOnes() {
        try (MockedStatic<DevoxxGenieStateService> ignored = mockState()) {
            AgentRegistry registry = AgentRegistry.getInstance();
            List<AgentDefinition> all = registry.getAll();

            // valid: add a custom agent
            all.add(AgentDefinition.builder()
                    .name("my-agent").instruction("Do things.").build());
            registry.saveAll(all);
            assertThat(registry.byName("my-agent")).isPresent();

            // invalid name
            List<AgentDefinition> badName = registry.getAll();
            badName.add(AgentDefinition.builder().name("Bad Name").instruction("x").build());
            assertThatThrownBy(() -> registry.saveAll(badName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid agent name");

            // duplicate name
            List<AgentDefinition> dup = registry.getAll();
            dup.add(AgentDefinition.builder().name("reviewer").instruction("x").build());
            assertThatThrownBy(() -> registry.saveAll(dup))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Duplicate");

            // empty persona
            List<AgentDefinition> empty = registry.getAll();
            empty.add(AgentDefinition.builder().name("blank-agent").instruction("  ").build());
            assertThatThrownBy(() -> registry.saveAll(empty))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty persona");

            // deleting a built-in
            List<AgentDefinition> withoutBuiltIn = registry.getAll().stream()
                    .filter(d -> !d.getName().equals("reviewer")).toList();
            assertThatThrownBy(() -> registry.saveAll(withoutBuiltIn))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be deleted");

            // failed saves must not have corrupted the stored list
            assertThat(registry.byName("reviewer")).isPresent();
            assertThat(registry.byName("blank-agent")).isEmpty();
        }
    }

    @Test
    void shippedDefault_returnsCopyForBuiltInsOnly() {
        try (MockedStatic<DevoxxGenieStateService> ignored = mockState()) {
            AgentRegistry registry = AgentRegistry.getInstance();
            assertThat(registry.shippedDefault("reviewer")).isPresent();
            assertThat(registry.shippedDefault("reviewer").orElseThrow().isReadOnly()).isTrue();
            assertThat(registry.shippedDefault("custom-thing")).isEmpty();
            assertThat(registry.shippedDefault(null)).isEmpty();
        }
    }

    @Test
    void isValidName_enforcesFlatLowercaseTokens() {
        assertThat(AgentRegistry.isValidName("reviewer")).isTrue();
        assertThat(AgentRegistry.isValidName("my-agent-2")).isTrue();
        assertThat(AgentRegistry.isValidName("A")).isFalse();
        assertThat(AgentRegistry.isValidName("Upper")).isFalse();
        assertThat(AgentRegistry.isValidName("has space")).isFalse();
        assertThat(AgentRegistry.isValidName("9starts-with-digit")).isFalse();
        assertThat(AgentRegistry.isValidName(null)).isFalse();
        assertThat(AgentRegistry.isValidName("x".repeat(40))).isFalse();
    }

    @Test
    void toolsetPresets_resolveAndClampReadOnly() {
        Set<String> rw = AgentToolsetPreset.resolveTools(List.of("filesystem", "shell"), false);
        assertThat(rw).contains("read_file", "write_file", "edit_file", "run_command", "run_tests");

        Set<String> ro = AgentToolsetPreset.resolveTools(List.of("filesystem", "shell"), true);
        assertThat(ro).contains("read_file", "list_files", "search_files")
                .doesNotContain("write_file", "edit_file", "run_command", "run_tests");

        assertThat(AgentToolsetPreset.resolveTools(List.of("unknown", "fetch"), false))
                .containsExactly("fetch_page", "web_search");
        assertThat(AgentToolsetPreset.resolveTools(null, false)).isEmpty();
    }
}
