package com.devoxx.genie.service.agent.tool;

import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges tools from multiple ToolProviders into a single ToolProviderResult.
 *
 * <p>When two providers expose a tool with the same name (e.g. the built-in
 * {@code read_file} tool and a {@code read_file} tool from the JetBrains MCP server),
 * langchain4j's {@code ToolProviderResult.Builder} would otherwise throw
 * {@code IllegalConfigurationException: Duplicated definition for tool: <name>}.
 * We deduplicate by tool name so later delegates override earlier ones: the
 * provider order in {@code AgentToolProviderFactory} is
 * {@code [built-in, MCP, skills]}, which means an explicitly enabled MCP tool takes
 * precedence over the built-in tool of the same name.
 */
@Slf4j
public class CompositeToolProvider implements ToolProvider {

    private final List<ToolProvider> delegates;

    public CompositeToolProvider(@NotNull List<ToolProvider> delegates) {
        this.delegates = delegates;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        // Keyed by tool name; LinkedHashMap.put keeps the first-seen ordering while
        // letting a later delegate replace (win over) an earlier one with the same name.
        Map<String, AiServiceTool> toolsByName = new LinkedHashMap<>();

        for (ToolProvider delegate : delegates) {
            ToolProviderResult result = delegate.provideTools(request);
            for (AiServiceTool tool : result.aiServiceTools()) {
                AiServiceTool previous = toolsByName.put(tool.name(), tool);
                if (previous != null) {
                    log.info("Tool name '{}' provided by multiple providers; the later provider wins (MCP overrides built-in)",
                            tool.name());
                }
            }
        }

        ToolProviderResult.Builder builder = ToolProviderResult.builder();
        toolsByName.values().forEach(builder::add);
        return builder.build();
    }
}
