package com.devoxx.genie.service.agent;

import com.devoxx.genie.model.agent.AgentDefinition;
import com.devoxx.genie.service.agent.team.AgentRegistry;
import com.devoxx.genie.service.agent.team.TeamAgentToolProvider;
import com.devoxx.genie.service.agent.tool.BuiltInToolProvider;
import com.devoxx.genie.service.agent.tool.CompositeToolProvider;
import com.devoxx.genie.service.mcp.MCPExecutionService;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.service.skill.SkillRegistry;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.Skills;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory that assembles the agent tool provider chain:
 * BuiltInToolProvider + MCP → CompositeToolProvider → AgentApprovalProvider → AgentLoopTracker
 * Returns null when agent mode is disabled, allowing callers to fall back to MCP-only.
 */
@Slf4j
public class AgentToolProviderFactory {

    private AgentToolProviderFactory() {
    }

    /**
     * Creates the full tool provider chain for agent mode.
     *
     * @param project The current project
     * @return A fully wrapped ToolProvider, or null if agent mode is disabled
     */
    @Nullable
    public static ToolProvider createToolProvider(@NotNull Project project) {
        return createToolProvider(project, null);
    }

    /**
     * Same as {@link #createToolProvider(Project)} but threads an optional per-prompt MCP
     * call counter through to {@link MCPExecutionService#createRawMCPToolProvider(AtomicInteger)}
     * so MCP-inside-agent invocations are counted for task-209 analytics (AC #24).
     */
    @Nullable
    public static ToolProvider createToolProvider(@NotNull Project project,
                                                  @Nullable AtomicInteger mcpCallCounter) {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        if (!Boolean.TRUE.equals(settings.getAgentModeEnabled())) {
            return null;
        }

        // TASK-249: a specialist selected directly in the LLM dropdown ("Agent Team"
        // provider + e.g. "reviewer" as the model) chats with ITS scoped toolset —
        // exactly what a delegation would give it (approval-gated, budgeted, no
        // delegate_task) — instead of the orchestrator chain below.
        if (Boolean.TRUE.equals(settings.getAgentTeamEnabled())) {
            var directAgent = AgentRegistry.getInstance()
                    .selectedDirectAgent(project.getLocationHash());
            if (directAgent.isPresent()) {
                AgentDefinition definition = directAgent.get();
                ToolProvider teamTools = new TeamAgentToolProvider(project, definition);
                boolean autoApproveRO = Boolean.TRUE.equals(settings.getAgentAutoApproveReadOnly());
                ToolProvider approved = new AgentApprovalProvider(teamTools, project, autoApproveRO);
                int budget = definition.getMaxToolCalls() != null && definition.getMaxToolCalls() > 0
                        ? definition.getMaxToolCalls()
                        : (settings.getAgentMaxToolCalls() != null ? settings.getAgentMaxToolCalls() : 25);
                return new AgentLoopTracker(approved, budget, project, definition.getName());
            }
        }

        List<ToolProvider> providers = new ArrayList<>();

        // Add built-in IDE tools (including parallel_explore if enabled)
        BuiltInToolProvider builtInToolProvider = new BuiltInToolProvider(project);
        providers.add(builtInToolProvider);

        // Add MCP tools if MCP is also enabled
        if (MCPService.isMCPEnabled()) {
            ToolProvider mcpProvider = getMcpToolProviderWithoutApproval(project, mcpCallCounter);
            if (mcpProvider != null) {
                providers.add(mcpProvider);
            }
        }

        // Add langchain4j Skills tool provider (issue #1040). The provider exposes the
        // activate_skill (and optional read_skill_resource) management tools and any
        // skill-scoped tools once the LLM activates a skill.
        try {
            Skills skills = SkillRegistry.getInstance(project).buildSkills();
            if (skills != null) {
                providers.add(skills.toolProvider());
                log.info("Skills tool provider included in agent tool chain");
            }
        } catch (Exception e) {
            log.warn("Failed to add Skills tool provider to agent tool chain", e);
        }

        // Merge all providers
        ToolProvider compositeProvider = providers.size() == 1
                ? providers.get(0)
                : new CompositeToolProvider(providers);

        // Agent Team pure-coordinator mode: strip direct write/run tools from the
        // orchestrating conversation so hands-on work must go through delegate_task —
        // the structural version of the DockerAgents orchestrator's "never execute
        // tasks yourself" mandate. Applies ONLY to this (parent) chain; delegated
        // children build their own scoped providers via TeamAgentToolProvider.
        if (Boolean.TRUE.equals(settings.getAgentTeamEnabled())
                && Boolean.TRUE.equals(settings.getAgentTeamPureCoordinator())) {
            compositeProvider = new CoordinatorToolFilter(compositeProvider);
        }

        // Wrap with approval
        boolean autoApproveReadOnly = Boolean.TRUE.equals(settings.getAgentAutoApproveReadOnly());
        ToolProvider approvedProvider = new AgentApprovalProvider(
                compositeProvider, project, autoApproveReadOnly);

        // Wrap with loop tracker
        int maxToolCalls = settings.getAgentMaxToolCalls() != null
                ? settings.getAgentMaxToolCalls()
                : 25;

        AgentLoopTracker tracker = new AgentLoopTracker(approvedProvider, maxToolCalls, project);

        // Register the parallel explore executor as a cancellable child so user cancellation
        // propagates to any running sub-agents
        if (builtInToolProvider.getParallelExploreExecutor() != null) {
            tracker.registerChild(builtInToolProvider.getParallelExploreExecutor());
        }

        // Same for delegate_task: Stop must cancel all in-flight delegated team agents
        if (builtInToolProvider.getDelegateTaskExecutor() != null) {
            tracker.registerChild(builtInToolProvider.getDelegateTaskExecutor());
        }

        return tracker;
    }

    /**
     * Gets the raw MCP tool provider (without ApprovalRequiredToolProvider wrapper),
     * since AgentApprovalProvider handles approval uniformly for all tools.
     * Does not pre-check cached tool metadata — the actual MCP client connection
     * determines available tools at runtime.
     */
    @Nullable
    private static ToolProvider getMcpToolProviderWithoutApproval(@NotNull Project project,
                                                                  @Nullable AtomicInteger mcpCallCounter) {
        try {
            ToolProvider provider = MCPExecutionService.getInstance()
                    .createRawMCPToolProvider(mcpCallCounter);
            if (provider != null) {
                log.info("MCP tool provider included in agent tool chain");
            } else {
                log.debug("MCP tool provider not available (no enabled servers or clients)");
            }
            return provider;
        } catch (Exception e) {
            log.warn("Failed to create MCP tool provider for agent mode", e);
            return null;
        }
    }

    /**
     * Removes direct write/run tools from the orchestrating conversation's tool chain
     * when Agent Team pure-coordinator mode is on. The orchestrator keeps read/search
     * tools (cheap triage) and delegate_task; modifications must flow through delegated
     * specialists, whose own writes remain approval-gated.
     */
    static final class CoordinatorToolFilter implements ToolProvider {

        static final java.util.Set<String> COORDINATOR_BLOCKED_TOOLS =
                java.util.Set.of("write_file", "edit_file", "run_command", "run_tests");

        private final ToolProvider delegate;

        CoordinatorToolFilter(@NotNull ToolProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public dev.langchain4j.service.tool.ToolProviderResult provideTools(
                dev.langchain4j.service.tool.ToolProviderRequest request) {
            dev.langchain4j.service.tool.ToolProviderResult all = delegate.provideTools(request);
            dev.langchain4j.service.tool.ToolProviderResult.Builder builder =
                    dev.langchain4j.service.tool.ToolProviderResult.builder();
            for (dev.langchain4j.service.tool.AiServiceTool tool : all.aiServiceTools()) {
                if (!COORDINATOR_BLOCKED_TOOLS.contains(tool.toolSpecification().name())) {
                    builder.add(tool);
                }
            }
            return builder.build();
        }
    }
}
