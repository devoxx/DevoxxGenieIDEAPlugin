package com.devoxx.genie.service.mcp;

import com.devoxx.genie.service.agent.loop.AgentRunMetrics;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardedMcpToolProviderTest {

    private static ToolSpecification spec(String name, Map<String, Object> metadata) {
        ToolSpecification.Builder builder = ToolSpecification.builder()
                .name(name)
                .description("MCP tool")
                .parameters(JsonObjectSchema.builder().addStringProperty("id", "id").required("id").build());
        if (metadata != null) {
            builder.metadata(metadata);
        }
        return builder.build();
    }

    private static ToolExecutionRequest request(String name, String args) {
        return ToolExecutionRequest.builder().id("1").name(name).arguments(args).build();
    }

    private static ToolExecutor guarded(ToolSpecification spec, ToolExecutor executor, AgentRunMetrics metrics) {
        ToolProvider raw = req -> ToolProviderResult.builder().add(spec, executor).build();
        return new GuardedMcpToolProvider(raw, metrics, 0).provideTools(null).toolExecutorByName(spec.name());
    }

    /** Executor failing with a transport error the first {@code failures} times. */
    private static ToolExecutor flaky(AtomicInteger calls, int failures) {
        return (req, id) -> {
            if (calls.incrementAndGet() <= failures) {
                throw new UncheckedIOException(new IOException("connection reset"));
            }
            return "result for " + req.arguments();
        };
    }

    @Test
    void invalidArguments_areRejectedLocally_withoutCallingTheServer() {
        AtomicInteger calls = new AtomicInteger();
        AgentRunMetrics metrics = new AgentRunMetrics();
        ToolExecutor executor = guarded(spec("get_issue", null), flaky(calls, 0), metrics);

        String result = executor.execute(request("get_issue", "{}"), null);

        assertThat(result).startsWith("Error: invalid arguments for MCP tool 'get_issue'")
                .contains("missing required parameter 'id'");
        assertThat(calls.get()).isZero();
        assertThat(metrics.getValidationRejects()).isEqualTo(1);
    }

    @Test
    void validArguments_resultIsWrappedAsUntrusted() {
        ToolExecutor executor = guarded(spec("get_issue", null), flaky(new AtomicInteger(), 0), null);

        String result = executor.execute(request("get_issue", "{\"id\":\"42\"}"), null);

        assertThat(result).startsWith("<untrusted_tool_output source=\"mcp:get_issue\">")
                .contains("result for {\"id\":\"42\"}");
    }

    @Test
    void readOnlyTool_transientFailure_isRetriedOnce() {
        AtomicInteger calls = new AtomicInteger();
        AgentRunMetrics metrics = new AgentRunMetrics();
        ToolExecutor executor = guarded(spec("get_issue", Map.of("annotations", Map.of("readOnlyHint", true))),
                flaky(calls, 1), metrics);

        String result = executor.execute(request("get_issue", "{\"id\":\"42\"}"), null);

        assertThat(result).contains("result for");
        assertThat(calls.get()).isEqualTo(2);
        assertThat(metrics.getRetries()).isEqualTo(1);
    }

    @Test
    void readOnlyTool_secondFailure_propagates() {
        AtomicInteger calls = new AtomicInteger();
        ToolExecutor executor = guarded(spec("get_issue", Map.of("annotations", Map.of("idempotentHint", "true"))),
                flaky(calls, 2), null);

        assertThatThrownBy(() -> executor.execute(request("get_issue", "{\"id\":\"42\"}"), null))
                .isInstanceOf(UncheckedIOException.class);
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void toolWithoutReadOnlyHint_isNeverRetried() {
        AtomicInteger calls = new AtomicInteger();
        ToolExecutor executor = guarded(spec("create_issue", null), flaky(calls, 1), null);

        assertThatThrownBy(() -> executor.execute(request("create_issue", "{\"id\":\"42\"}"), null))
                .isInstanceOf(UncheckedIOException.class);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void nonTransientFailure_isNotRetried() {
        AtomicInteger calls = new AtomicInteger();
        ToolExecutor failing = (req, id) -> {
            calls.incrementAndGet();
            throw new IllegalStateException("server said no");
        };
        ToolExecutor executor = guarded(spec("get_issue", Map.of("annotations", Map.of("readOnlyHint", true))), failing, null);

        assertThatThrownBy(() -> executor.execute(request("get_issue", "{\"id\":\"1\"}"), null))
                .isInstanceOf(IllegalStateException.class);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void executeWithContext_validatesAndWraps_butLeavesServerErrorsAlone() {
        ToolExecutor delegate = new ToolExecutor() {
            @Override
            public String execute(ToolExecutionRequest request, Object memoryId) {
                return "unused";
            }

            @Override
            public ToolExecutionResult executeWithContext(ToolExecutionRequest request, InvocationContext context) {
                boolean fail = request.arguments().contains("fail");
                return ToolExecutionResult.builder().isError(fail).resultText(fail ? "server error" : "ok").build();
            }
        };
        ToolExecutor executor = guarded(spec("get_issue", null), delegate, null);

        ToolExecutionResult rejected = executor.executeWithContext(request("get_issue", "{}"), null);
        ToolExecutionResult ok = executor.executeWithContext(request("get_issue", "{\"id\":\"1\"}"), null);
        ToolExecutionResult serverError = executor.executeWithContext(request("get_issue", "{\"id\":\"fail\"}"), null);

        assertThat(rejected.isError()).isTrue();
        assertThat(rejected.resultText()).contains("missing required parameter 'id'");
        assertThat(ok.resultText()).startsWith("<untrusted_tool_output").contains("ok");
        assertThat(serverError.resultText()).isEqualTo("server error");
    }

    @Test
    void isRetryable_readsAnnotationsInSeveralShapes() {
        assertThat(GuardedMcpToolProvider.isRetryable(spec("a", Map.of("annotations", Map.of("readOnlyHint", true))))).isTrue();
        assertThat(GuardedMcpToolProvider.isRetryable(spec("b", Map.of("annotations", "{\"readOnlyHint\": true}")))).isTrue();
        assertThat(GuardedMcpToolProvider.isRetryable(spec("c", Map.of("annotations", Map.of("readOnlyHint", false))))).isFalse();
        assertThat(GuardedMcpToolProvider.isRetryable(spec("d", Map.of("annotations", Map.of("destructiveHint", true))))).isFalse();
        assertThat(GuardedMcpToolProvider.isRetryable(spec("e", null))).isFalse();
    }

    @Test
    void isTransient_detectsIoAndTimeoutsInCauseChain() {
        assertThat(GuardedMcpToolProvider.isTransient(new RuntimeException(new IOException("x")))).isTrue();
        assertThat(GuardedMcpToolProvider.isTransient(new RuntimeException(new java.util.concurrent.TimeoutException()))).isTrue();
        assertThat(GuardedMcpToolProvider.isTransient(new IllegalArgumentException("bad"))).isFalse();
    }
}
