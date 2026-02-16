package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Wraps an MCP ToolProvider and filters out tools that have been disabled
 * by the user at the individual tool level in MCP server settings.
 */
@Slf4j
public class FilteredMcpToolProvider implements ToolProvider {

    private final ToolProvider delegate;
    private final Supplier<Set<String>> disabledToolsSupplier;

    public FilteredMcpToolProvider(@NotNull ToolProvider delegate) {
        this(delegate, FilteredMcpToolProvider::collectDisabledToolsFromSettings);
    }

    FilteredMcpToolProvider(@NotNull ToolProvider delegate,
                            @NotNull Supplier<Set<String>> disabledToolsSupplier) {
        this.delegate = delegate;
        this.disabledToolsSupplier = disabledToolsSupplier;
    }

    @Override
    public ToolProviderResult provideTools(@NotNull ToolProviderRequest request) {
        ToolProviderResult delegateResult = delegate.provideTools(request);

        // Collect all disabled tool names across all enabled MCP servers
        Set<String> allDisabledTools = disabledToolsSupplier.get();

        if (allDisabledTools.isEmpty()) {
            return delegateResult;
        }

        ToolProviderResult.Builder builder = ToolProviderResult.builder();

        for (Map.Entry<ToolSpecification, ToolExecutor> entry : delegateResult.tools().entrySet()) {
            ToolSpecification spec = entry.getKey();
            if (!allDisabledTools.contains(spec.name())) {
                builder.add(spec, entry.getValue());
            } else {
                MCPService.logDebug("Filtered out disabled MCP tool: " + spec.name());
            }
        }

        return builder.build();
    }

    /**
     * Collects all disabled tool names from all enabled MCP servers using application settings.
     */
    @NotNull
    static Set<String> collectDisabledToolsFromSettings() {
        Map<String, MCPServer> mcpServers = DevoxxGenieStateService.getInstance()
                .getMcpSettings()
                .getMcpServers();
        return collectDisabledTools(mcpServers);
    }

    /**
     * Collects all disabled tool names from all enabled MCP servers.
     */
    @NotNull
    static Set<String> collectDisabledTools(@NotNull Map<String, MCPServer> mcpServers) {
        Set<String> disabledTools = new HashSet<>();
        for (MCPServer server : mcpServers.values()) {
            if (server.isEnabled() && server.getDisabledTools() != null) {
                disabledTools.addAll(server.getDisabledTools());
            }
        }
        return disabledTools;
    }
}
