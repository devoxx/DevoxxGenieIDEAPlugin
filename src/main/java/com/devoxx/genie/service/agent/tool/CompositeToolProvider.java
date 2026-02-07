package com.devoxx.genie.service.agent.tool;

import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Merges tools from multiple ToolProviders into a single ToolProviderResult.
 */
public class CompositeToolProvider implements ToolProvider {

    private final List<ToolProvider> delegates;

    public CompositeToolProvider(@NotNull List<ToolProvider> delegates) {
        this.delegates = delegates;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ToolProviderResult.Builder builder = ToolProviderResult.builder();

        for (ToolProvider delegate : delegates) {
            ToolProviderResult result = delegate.provideTools(request);
            builder.addAll(result.tools());
        }

        return builder.build();
    }
}
