package com.devoxx.genie.service.agent;

import com.devoxx.genie.model.mcp.MCPServer;
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
import java.util.Map;

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

        // Add built-in IDE tools
        providers.add(new BuiltInToolProvider(project));

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

        return new AgentLoopTracker(approvedProvider, maxToolCalls, project);
    }

    /**
     * Gets the MCP tool provider without the existing ApprovalRequiredToolProvider wrapper,
     * since AgentApprovalProvider handles approval uniformly for all tools.
     */
    @Nullable
    private static ToolProvider getMcpToolProviderWithoutApproval(@NotNull Project project) {
        try {
            Map<String, MCPServer> mcpServers = DevoxxGenieStateService.getInstance()
                    .getMcpSettings().getMcpServers();

            if (mcpServers.isEmpty()) return null;

            int totalActiveMCPTools = mcpServers.values().stream()
                    .filter(MCPServer::isEnabled)
                    .mapToInt(server -> server.getAvailableTools().size())
                    .sum();

            if (totalActiveMCPTools == 0) return null;

            // Get the MCP provider - it comes with ApprovalRequiredToolProvider wrapper,
            // but we still use it since our AgentApprovalProvider handles the approval
            // uniformly. The MCP approval will be auto-approved when headless or not required.
            return MCPExecutionService.getInstance().createMCPToolProvider(project);
        } catch (Exception e) {
            log.warn("Failed to create MCP tool provider for agent mode", e);
            return null;
        }
    }
}
