package com.devoxx.genie.service.agent;

import com.devoxx.genie.service.agent.tool.BuiltInToolProvider;
import com.devoxx.genie.service.agent.tool.CompositeToolProvider;
import com.devoxx.genie.service.mcp.MCPExecutionService;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        if (!Boolean.TRUE.equals(settings.getAgentModeEnabled())) {
            return null;
        }

        List<ToolProvider> providers = new ArrayList<>();

        // Add built-in IDE tools (including parallel_explore if enabled)
        BuiltInToolProvider builtInToolProvider = new BuiltInToolProvider(project);
        providers.add(builtInToolProvider);

        // Add MCP tools if MCP is also enabled
        if (MCPService.isMCPEnabled()) {
            ToolProvider mcpProvider = getMcpToolProviderWithoutApproval(project);
            if (mcpProvider != null) {
                providers.add(mcpProvider);
            }
        }

        // Merge all providers
        ToolProvider compositeProvider = providers.size() == 1
                ? providers.get(0)
                : new CompositeToolProvider(providers);

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

        return tracker;
    }

    /**
     * Gets the raw MCP tool provider (without ApprovalRequiredToolProvider wrapper),
     * since AgentApprovalProvider handles approval uniformly for all tools.
     * Does not pre-check cached tool metadata — the actual MCP client connection
     * determines available tools at runtime.
     */
    @Nullable
    private static ToolProvider getMcpToolProviderWithoutApproval(@NotNull Project project) {
        try {
            ToolProvider provider = MCPExecutionService.getInstance().createRawMCPToolProvider();
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
}
