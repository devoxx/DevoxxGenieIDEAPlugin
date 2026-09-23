package com.devoxx.genie.service.mcp;

import com.devoxx.genie.service.agent.loop.AgentRunMetrics;
import com.devoxx.genie.service.agent.loop.UntrustedContent;
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
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Hardens MCP tool execution:
 * <ul>
 *   <li><b>Local argument validation</b> — arguments are checked against the tool's input
 *       schema ({@link McpArgumentValidator}); invalid calls are answered with a precise
 *       error without contacting the server.</li>
 *   <li><b>One retry of transient failures</b> — when a call fails with a transport-level
 *       exception (I/O error, timeout) and the server declares the tool read-only or
 *       idempotent ({@code readOnlyHint} / {@code idempotentHint} annotations), only that
 *       call is retried once. Calls that may have side effects are never retried, and a
 *       server-reported error result is never retried.</li>
 *   <li><b>Untrusted output</b> — successful text results are wrapped with
 *       {@link UntrustedContent} so the model treats them as data, not instructions.</li>
 * </ul>
 */
@Slf4j
public class GuardedMcpToolProvider implements ToolProvider {

    static final long RETRY_BACKOFF_MILLIS = 500;

    private static final Pattern READ_ONLY_HINT =
            Pattern.compile("\"?(readOnlyHint|idempotentHint)\"?\\s*[:=]\\s*\"?true", Pattern.CASE_INSENSITIVE);

    private final ToolProvider delegate;
    private final @Nullable AgentRunMetrics metrics;
    private final long retryBackoffMillis;

    public GuardedMcpToolProvider(@NotNull ToolProvider delegate, @Nullable AgentRunMetrics metrics) {
        this(delegate, metrics, RETRY_BACKOFF_MILLIS);
    }

    GuardedMcpToolProvider(@NotNull ToolProvider delegate, @Nullable AgentRunMetrics metrics, long retryBackoffMillis) {
        this.delegate = delegate;
        this.metrics = metrics;
        this.retryBackoffMillis = retryBackoffMillis;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ToolProviderResult result = delegate.provideTools(request);
        ToolProviderResult.Builder builder = ToolProviderResult.builder();
        for (AiServiceTool tool : result.aiServiceTools()) {
            builder.add(tool.toBuilder().toolExecutor(guard(tool.toolSpecification(), tool.toolExecutor())).build());
        }
        return builder.build();
    }

    private @NotNull ToolExecutor guard(@NotNull ToolSpecification spec, @NotNull ToolExecutor original) {
        boolean retryable = isRetryable(spec);
        return new ToolExecutor() {
            @Override
            public String execute(ToolExecutionRequest toolRequest, Object memoryId) {
                String rejection = validate(spec, toolRequest);
                if (rejection != null) {
                    return rejection;
                }
                String text = withRetry(spec.name(), retryable, () -> original.execute(toolRequest, memoryId));
                return UntrustedContent.wrapIfEnabled("mcp:" + spec.name(), text);
            }

            @Override
            public ToolExecutionResult executeWithContext(ToolExecutionRequest toolRequest, InvocationContext context) {
                String rejection = validate(spec, toolRequest);
                if (rejection != null) {
                    return ToolExecutionResult.builder().isError(true).resultText(rejection).build();
                }
                ToolExecutionResult result = withRetry(spec.name(), retryable,
                        () -> original.executeWithContext(toolRequest, context));
                return wrapResult(spec.name(), result);
            }
        };
    }

    private @Nullable String validate(@NotNull ToolSpecification spec, @NotNull ToolExecutionRequest toolRequest) {
        List<String> problems = McpArgumentValidator.validate(toolRequest.arguments(), spec.parameters());
        if (problems.isEmpty()) {
            return null;
        }
        if (metrics != null) {
            metrics.recordValidationReject();
        }
        log.debug("Rejected MCP call {} locally: {}", spec.name(), problems);
        return McpArgumentValidator.errorMessage(spec.name(), problems, spec.parameters());
    }

    private <T> T withRetry(@NotNull String toolName, boolean retryable, @NotNull Supplier<T> call) {
        try {
            return call.get();
        } catch (RuntimeException e) {
            if (!retryable || !isTransient(e)) {
                throw e;
            }
            log.info("Transient failure calling read-only MCP tool {} ({}); retrying once", toolName, e.getMessage());
            if (metrics != null) {
                metrics.recordRetry();
            }
            try {
                Thread.sleep(retryBackoffMillis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw e;
            }
            return call.get();
        }
    }

    private static @Nullable ToolExecutionResult wrapResult(@NotNull String toolName, @Nullable ToolExecutionResult result) {
        if (result == null || result.isError()) {
            return result;
        }
        String text;
        try {
            text = result.resultText();
        } catch (RuntimeException e) {
            // Non-text content (e.g. images): pass through unchanged.
            return result;
        }
        String wrapped = UntrustedContent.wrapIfEnabled("mcp:" + toolName, text);
        if (wrapped == null || wrapped.equals(text)) {
            return result;
        }
        return ToolExecutionResult.builder()
                .isError(false)
                .result(result.result())
                .resultText(wrapped)
                .attributes(result.attributes())
                .build();
    }

    /** True when the server annotates the tool as read-only or idempotent. */
    static boolean isRetryable(@NotNull ToolSpecification spec) {
        Map<String, Object> metadata = spec.metadata();
        if (metadata == null) {
            return false;
        }
        Object annotations = metadata.get("annotations");
        if (annotations == null) {
            return false;
        }
        if (annotations instanceof Map<?, ?> map) {
            return isTrue(map.get("readOnlyHint")) || isTrue(map.get("idempotentHint"));
        }
        // Jackson/Gson trees and other representations: fall back to their JSON text.
        return READ_ONLY_HINT.matcher(annotations.toString()).find();
    }

    private static boolean isTrue(@Nullable Object value) {
        return value != null && "true".equalsIgnoreCase(value.toString().trim());
    }

    /** Transport-level failures worth one retry: I/O errors and timeouts anywhere in the cause chain. */
    static boolean isTransient(@NotNull Throwable error) {
        Throwable t = error;
        for (int depth = 0; t != null && depth < 10; depth++, t = t.getCause()) {
            if (t instanceof IOException || t instanceof UncheckedIOException || t instanceof TimeoutException
                    || t.getClass().getSimpleName().contains("Timeout")) {
                return true;
            }
        }
        return false;
    }
}
