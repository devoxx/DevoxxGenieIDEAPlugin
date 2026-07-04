package com.devoxx.genie.service.agent.team;

import com.devoxx.genie.model.agent.AgentDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TASK-247: Genie YAML import/export — DockerAgents agents/*.yml interop.
 */
class GenieAgentSpecMapperTest {

    /** Mirrors the shape of DockerAgents/agents/reviewer.yml. */
    private static final String REVIEWER_YAML = """
            models:
              default:
                provider: ollama
                model: qwen3.6:35b-a3b-coding-mxfp8

            agents:
              root:
                model: default
                description: >
                  Code review and PR quality specialist. Reviews code, summarizes
                  PRs, checks CI status, and provides quality feedback.
                instruction: |
                  You are a senior code reviewer. Review code quality and report findings.

                  ## Context discipline
                  - You are a leaf node.
                toolsets:
                  - type: shell
                  - type: filesystem
                    readonly: true
            """;

    @Test
    void fromYaml_importsDockerAgentsReviewerSpec() {
        GenieAgentSpecMapper.ImportResult result = GenieAgentSpecMapper.fromYaml("reviewer", REVIEWER_YAML);
        AgentDefinition def = result.definition();

        assertThat(def.getName()).isEqualTo("reviewer");
        assertThat(def.getModelProvider()).isEqualTo("Ollama");
        assertThat(def.getModelName()).isEqualTo("qwen3.6:35b-a3b-coding-mxfp8");
        assertThat(def.getDescription()).contains("Code review and PR quality specialist");
        assertThat(def.getInstruction()).contains("senior code reviewer").contains("leaf node");
        assertThat(def.getToolsetPresets()).containsExactly("shell", "filesystem-ro");
        // shell grants run_command — the spec is not fully read-only
        assertThat(def.isReadOnly()).isFalse();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void fromYaml_readOnlyWhenNothingWritableGranted() {
        String yaml = """
                agents:
                  root:
                    instruction: Research things.
                    toolsets:
                      - type: fetch
                      - type: filesystem
                        readonly: true
                """;
        GenieAgentSpecMapper.ImportResult result = GenieAgentSpecMapper.fromYaml("documentalist", yaml);

        assertThat(result.definition().isReadOnly()).isTrue();
        assertThat(result.definition().getToolsetPresets()).containsExactly("fetch", "filesystem-ro");
        // no models block = inherit conversation model, no warning
        assertThat(result.definition().getModelProvider()).isEmpty();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void fromYaml_unknownProviderAndToolsets_degradeToWarnings() {
        String yaml = """
                models:
                  default:
                    provider: codex
                    model: gpt-5-codex
                agents:
                  root:
                    instruction: Implement things.
                    toolsets:
                      - type: shell
                      - type: todo
                      - type: mcp
                      - type: quantum
                """;
        GenieAgentSpecMapper.ImportResult result = GenieAgentSpecMapper.fromYaml("implementer", yaml);

        assertThat(result.definition().getModelProvider()).isEmpty(); // fell back to inherit
        assertThat(result.definition().getToolsetPresets()).containsExactly("shell");
        assertThat(result.warnings())
                .anySatisfy(w -> assertThat(w).contains("codex"))
                .anySatisfy(w -> assertThat(w).contains("todo"))
                .anySatisfy(w -> assertThat(w).contains("mcp"))
                .anySatisfy(w -> assertThat(w).contains("quantum"));
    }

    @Test
    void fromYaml_rejectsNonSpecDocuments() {
        assertThatThrownBy(() -> GenieAgentSpecMapper.fromYaml("x", "just a string"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GenieAgentSpecMapper.fromYaml("x", "foo: bar"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("agents.root");
        assertThatThrownBy(() -> GenieAgentSpecMapper.fromYaml("x", "agents:\n  root:\n    description: no instruction"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instruction");
        assertThatThrownBy(() -> GenieAgentSpecMapper.fromYaml("x", ":\n  - ["))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roundTrip_preservesShippedPersonas() {
        for (AgentDefinition shipped : List.of(
                shippedWith("reviewer"), shippedWith("architect"),
                shippedWith("implementer"), shippedWith("documentalist"))) {

            String yaml = GenieAgentSpecMapper.toYaml(shipped);
            GenieAgentSpecMapper.ImportResult reimported = GenieAgentSpecMapper.fromYaml(shipped.getName(), yaml);
            AgentDefinition def = reimported.definition();

            assertThat(def.getName()).isEqualTo(shipped.getName());
            assertThat(def.getInstruction().strip()).isEqualTo(shipped.getInstruction().strip());
            assertThat(def.getToolsetPresets()).isEqualTo(shipped.getToolsetPresets());
            assertThat(def.isReadOnly()).isEqualTo(shipped.isReadOnly());
            assertThat(def.getModelProvider()).isEqualTo(shipped.getModelProvider());
            assertThat(reimported.warnings()).isEmpty();
        }
    }

    @Test
    void roundTrip_preservesProviderBinding() {
        AgentDefinition bound = shippedWith("reviewer");
        bound.setModelProvider("Ollama");
        bound.setModelName("qwen3:8b");

        String yaml = GenieAgentSpecMapper.toYaml(bound);
        assertThat(yaml).contains("provider: ollama").contains("model: qwen3:8b");

        AgentDefinition reimported = GenieAgentSpecMapper.fromYaml("reviewer", yaml).definition();
        assertThat(reimported.getModelProvider()).isEqualTo("Ollama");
        assertThat(reimported.getModelName()).isEqualTo("qwen3:8b");
    }

    private static AgentDefinition shippedWith(String name) {
        return BuiltInAgents.defaults().stream()
                .filter(d -> d.getName().equals(name))
                .findFirst()
                .orElseThrow()
                .copy();
    }
}
