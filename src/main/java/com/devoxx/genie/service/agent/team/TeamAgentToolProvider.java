package com.devoxx.genie.service.agent.team;

import com.devoxx.genie.model.agent.AgentDefinition;
import com.devoxx.genie.model.agent.AgentToolsetPreset;
import com.devoxx.genie.service.agent.tool.BuiltInToolProvider;
import com.intellij.openapi.project.Project;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Per-agent tool provider: exposes only the tools resolved from the agent's toolset
 * presets, intersected with whatever {@link BuiltInToolProvider} actually provides for
 * this project (globally disabled tools and feature-gated tools are therefore clamped
 * automatically — a team agent can never see a tool the parent conversation couldn't).
 * Delegation tools ({@code delegate_task}, {@code parallel_explore}) are structurally
 * excluded so delegation depth is always 1.
 */
public class TeamAgentToolProvider implements ToolProvider {

    /** Never available to delegated children, regardless of presets. */
    private static final Set<String> ORCHESTRATION_TOOLS = Set.of("delegate_task", "parallel_explore");

    private final BuiltInToolProvider builtInToolProvider;
    private final Set<String> allowedTools;

    public TeamAgentToolProvider(@NotNull Project project, @NotNull AgentDefinition definition) {
        this.builtInToolProvider = new BuiltInToolProvider(project);
        this.allowedTools = AgentToolsetPreset.resolveTools(
                definition.getToolsetPresets(), definition.isReadOnly());
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ToolProviderResult all = builtInToolProvider.provideTools(request);
        ToolProviderResult.Builder builder = ToolProviderResult.builder();
        for (AiServiceTool tool : all.aiServiceTools()) {
            String name = tool.toolSpecification().name();
            if (allowedTools.contains(name) && !ORCHESTRATION_TOOLS.contains(name)) {
                builder.add(tool);
            }
        }
        return builder.build();
    }

    public Set<String> getAllowedTools() {
        return allowedTools;
    }
}
