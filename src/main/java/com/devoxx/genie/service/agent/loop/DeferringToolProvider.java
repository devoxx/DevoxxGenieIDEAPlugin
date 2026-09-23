package com.devoxx.genie.service.agent.loop;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Wraps a tool provider (the MCP provider in agent mode) and, when it exposes more tools than
 * the configured threshold, registers them as deferred and adds the {@code search_tools}
 * meta tool. All tools remain registered with langchain4j so they are executable; their
 * specifications are withheld from requests by {@link AgentRequestTransformer} until unlocked.
 *
 * <p>Calling a deferred tool directly by name (its name is listed in the {@code search_tools}
 * description) also unlocks it, so its definition is sent from then on.
 */
@Slf4j
public class DeferringToolProvider implements ToolProvider {

    private final ToolProvider delegate;
    private final AgentRunContext context;

    public DeferringToolProvider(@NotNull ToolProvider delegate, @NotNull AgentRunContext context) {
        this.delegate = delegate;
        this.context = context;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ToolProviderResult result = delegate.provideTools(request);
        List<AiServiceTool> tools = result.aiServiceTools();
        if (!context.isDeferMcpTools() || tools.size() <= context.getDeferMcpToolsThreshold()) {
            return result;
        }

        DeferredToolRegistry registry = context.getDeferredTools();
        registry.defer(tools.stream().map(AiServiceTool::toolSpecification).toList());
        log.info("Deferring {} MCP tool definitions behind {} (threshold {})",
                tools.size(), SearchToolsToolExecutor.TOOL_NAME, context.getDeferMcpToolsThreshold());

        ToolProviderResult.Builder builder = ToolProviderResult.builder();
        for (AiServiceTool tool : tools) {
            builder.add(tool.toBuilder().toolExecutor(unlockingExecutor(tool)).build());
        }
        builder.add(SearchToolsToolExecutor.specification(registry.hiddenToolNames()),
                new SearchToolsToolExecutor(registry, context.getMetrics()));
        return builder.build();
    }

    private @NotNull ToolExecutor unlockingExecutor(@NotNull AiServiceTool tool) {
        ToolSpecification spec = tool.toolSpecification();
        ToolExecutor original = tool.toolExecutor();
        DeferredToolRegistry registry = context.getDeferredTools();
        return new ToolExecutor() {
            @Override
            public String execute(ToolExecutionRequest toolRequest, Object memoryId) {
                unlock();
                return original.execute(toolRequest, memoryId);
            }

            @Override
            public ToolExecutionResult executeWithContext(ToolExecutionRequest toolRequest, InvocationContext invocationContext) {
                unlock();
                return original.executeWithContext(toolRequest, invocationContext);
            }

            private void unlock() {
                if (registry.unlock(spec.name())) {
                    context.getMetrics().recordToolsUnlocked(1);
                }
            }
        };
    }
}
