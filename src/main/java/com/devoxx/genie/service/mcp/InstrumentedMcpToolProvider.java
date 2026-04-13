package com.devoxx.genie.service.mcp;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts actual MCP tool invocations per prompt (task-209, AC #24).
 *
 * <p>Sits in the MCP provider stack <strong>outside</strong> {@link FilteredMcpToolProvider}
 * (so disabled tools are never counted) and <strong>inside</strong>
 * {@link ApprovalRequiredToolProvider} (so denied-by-user calls don't count). The counter is
 * incremented inside the wrapped {@link ToolExecutor#execute} lambda — NOT inside
 * {@link #provideTools} — because the LLM framework may call {@code provideTools}
 * speculatively without actually executing anything.
 *
 * <p>The counter is owned by the caller (usually stashed on {@code ChatMessageContext}) so
 * the per-prompt count can be read after the chat finishes and bucketed into a
 * {@code feature_used} event. The provider itself never emits analytics events — it only
 * counts.
 */
@Slf4j
public class InstrumentedMcpToolProvider implements ToolProvider {

    private final ToolProvider delegate;
    private final AtomicInteger counter;

    public InstrumentedMcpToolProvider(@NotNull ToolProvider delegate, @NotNull AtomicInteger counter) {
        this.delegate = delegate;
        this.counter = counter;
    }

    @Override
    public ToolProviderResult provideTools(@NotNull ToolProviderRequest request) {
        ToolProviderResult delegateResult = delegate.provideTools(request);
        ToolProviderResult.Builder builder = ToolProviderResult.builder();

        for (Map.Entry<ToolSpecification, ToolExecutor> entry : delegateResult.tools().entrySet()) {
            ToolSpecification spec = entry.getKey();
            ToolExecutor originalExecutor = entry.getValue();

            ToolExecutor countingExecutor = (toolRequest, memoryId) -> {
                String result = originalExecutor.execute(toolRequest, memoryId);
                // Increment AFTER successful execution so failures don't inflate usage counts.
                counter.incrementAndGet();
                return result;
            };

            builder.add(spec, countingExecutor);
        }

        return builder.build();
    }
}
