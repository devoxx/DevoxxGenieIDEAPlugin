package com.devoxx.genie.model.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A named agent in the Agent Team: persona (system instruction), tool allowlist and an
 * optional per-agent LLM provider/model binding. The provider/model indirection is what
 * enables hybrid teams — e.g. a local Ollama reviewer next to a cloud implementer.
 * <p>
 * When {@code modelProvider} is empty the agent inherits the conversation's active
 * provider/model. Built-in agents ({@code builtIn=true}) can be edited and reset but
 * not deleted.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AgentDefinition {

    @Builder.Default
    private String name = "";
    @Builder.Default
    private String description = "";
    /** Persona / system prompt body injected for this agent's runs. */
    @Builder.Default
    private String instruction = "";
    /** Empty = inherit the conversation's active provider. */
    @Builder.Default
    private String modelProvider = "";
    /** Empty = inherit / first available model of the provider. */
    @Builder.Default
    private String modelName = "";
    /**
     * Toolset preset names ({@link AgentToolsetPreset}) this agent may use. Resolved to
     * concrete tool names at run time and clamped to the parent conversation's toolset.
     */
    @Builder.Default
    private List<String> toolsetPresets = new ArrayList<>();
    /** Convenience flag: strips write/run tools at tool-provider build time. */
    private boolean readOnly;
    /** Null = SUB_AGENT_MAX_TOOL_CALLS default. */
    private Integer maxToolCalls;
    /** Null = SUB_AGENT_TIMEOUT_SECONDS default. */
    private Integer timeoutSeconds;
    /** Null = global temperature setting. */
    private Double temperature;
    /** Built-ins are seeded by {@code AgentRegistry}, reset-able but not deletable. */
    private boolean builtIn;
    @Builder.Default
    private boolean enabled = true;
    /**
     * Where delegated tasks for this agent execute ({@link AgentExecutionTarget} name).
     * Stored as a String for XML-serialization stability; empty/unknown = IN_PROCESS.
     * Applies to delegations only — direct chat (Agent Team dropdown) is always in-process.
     */
    @Builder.Default
    private String executionTarget = "";

    /** Null-safe typed accessor for {@link #executionTarget}. */
    public AgentExecutionTarget effectiveExecutionTarget() {
        return AgentExecutionTarget.fromString(executionTarget);
    }

    /** Defensive copy used by settings UI and registry reset. */
    public AgentDefinition copy() {
        return this.toBuilder()
                .toolsetPresets(new ArrayList<>(toolsetPresets != null ? toolsetPresets : new ArrayList<>()))
                .build();
    }
}
