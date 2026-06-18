package com.devoxx.genie.service.mcp;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.invocation.InvocationContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link InstrumentedMcpToolProvider} (task-209, AC #24).
 *
 * <p>Verifies the counter is incremented inside the wrapped {@code execute()} path and
 * <strong>not</strong> inside {@code provideTools} — so speculative {@code provideTools}
 * calls by the LLM framework never inflate the count. Also verifies failed executions do
 * not increment the counter.
 */
class InstrumentedMcpToolProviderTest {

    @Test
    void provideToolsDoesNotIncrementCounter() {
        AtomicInteger counter = new AtomicInteger(0);
        ToolProvider delegate = fakeProvider((req, mem) -> "ok");

        InstrumentedMcpToolProvider instrumented = new InstrumentedMcpToolProvider(delegate, counter);
        // Speculative provideTools call: the LLM may ask for the tool list without executing anything.
        instrumented.provideTools(new ToolProviderRequest("test", UserMessage.from("hi")));

        assertThat(counter.get()).isZero();
    }

    @Test
    void eachExecuteIncrementsCounter() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        ToolProvider delegate = fakeProvider((req, mem) -> "tool-result");

        InstrumentedMcpToolProvider instrumented = new InstrumentedMcpToolProvider(delegate, counter);
        ToolProviderResult result = instrumented.provideTools(new ToolProviderRequest("test", UserMessage.from("hi")));
        ToolExecutor executor = result.tools().values().iterator().next();

        executor.execute(dummyRequest(), null);
        executor.execute(dummyRequest(), null);
        executor.execute(dummyRequest(), null);

        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    void failedExecutionDoesNotIncrementCounter() {
        AtomicInteger counter = new AtomicInteger(0);
        ToolProvider delegate = fakeProvider((req, mem) -> {
            throw new RuntimeException("MCP server unreachable");
        });

        InstrumentedMcpToolProvider instrumented = new InstrumentedMcpToolProvider(delegate, counter);
        ToolProviderResult result = instrumented.provideTools(new ToolProviderRequest("test", UserMessage.from("hi")));
        ToolExecutor executor = result.tools().values().iterator().next();

        assertThatThrownBy(() -> executor.execute(dummyRequest(), null))
                .isInstanceOf(RuntimeException.class);

        assertThat(counter.get()).isZero();
    }

    @Test
    void delegateWithZeroToolsLeavesCounterUnchanged() {
        AtomicInteger counter = new AtomicInteger(0);
        ToolProvider empty = request -> ToolProviderResult.builder().build();

        InstrumentedMcpToolProvider instrumented = new InstrumentedMcpToolProvider(empty, counter);
        ToolProviderResult result = instrumented.provideTools(new ToolProviderRequest("test", UserMessage.from("hi")));

        assertThat(result.tools()).isEmpty();
        assertThat(counter.get()).isZero();
    }

    @Test
    void executeWithContextIncrementsCounter() {
        AtomicInteger counter = new AtomicInteger(0);
        ToolExecutor original = new ToolExecutor() {
            @Override
            public String execute(ToolExecutionRequest req, Object mem) {
                return "legacy";
            }

            @Override
            public ToolExecutionResult executeWithContext(ToolExecutionRequest req, InvocationContext ctx) {
                return ToolExecutionResult.builder().resultText("ctx-result").build();
            }
        };
        ToolProvider delegate = fakeProvider(original);

        InstrumentedMcpToolProvider instrumented = new InstrumentedMcpToolProvider(delegate, counter);
        ToolExecutor executor = instrumented
                .provideTools(new ToolProviderRequest("test", UserMessage.from("hi")))
                .tools().values().iterator().next();

        ToolExecutionResult result = executor.executeWithContext(dummyRequest(), null);

        assertThat(result.resultText()).isEqualTo("ctx-result");
        assertThat(counter.get()).isEqualTo(1);
    }

    /**
     * Mirrors the langchain4j-skills contract: legacy execute() throws, only
     * executeWithContext works. The counting wrapper must route through it.
     */
    @Test
    void executeWithContextWorksForExecuteThrowingTool() {
        AtomicInteger counter = new AtomicInteger(0);
        ToolExecutor skillStyle = new ToolExecutor() {
            @Override
            public String execute(ToolExecutionRequest req, Object mem) {
                throw new IllegalStateException("executeWithContext must be called instead");
            }

            @Override
            public ToolExecutionResult executeWithContext(ToolExecutionRequest req, InvocationContext ctx) {
                return ToolExecutionResult.builder().resultText("skill-ok").build();
            }
        };

        InstrumentedMcpToolProvider instrumented = new InstrumentedMcpToolProvider(fakeProvider(skillStyle), counter);
        ToolExecutor executor = instrumented
                .provideTools(new ToolProviderRequest("test", UserMessage.from("hi")))
                .tools().values().iterator().next();

        ToolExecutionResult result = executor.executeWithContext(dummyRequest(), null);

        assertThat(result.resultText()).isEqualTo("skill-ok");
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void failedExecuteWithContextDoesNotIncrementCounter() {
        AtomicInteger counter = new AtomicInteger(0);
        ToolExecutor failing = new ToolExecutor() {
            @Override
            public String execute(ToolExecutionRequest req, Object mem) {
                return "unused";
            }

            @Override
            public ToolExecutionResult executeWithContext(ToolExecutionRequest req, InvocationContext ctx) {
                throw new RuntimeException("MCP server unreachable");
            }
        };

        InstrumentedMcpToolProvider instrumented = new InstrumentedMcpToolProvider(fakeProvider(failing), counter);
        ToolExecutor executor = instrumented
                .provideTools(new ToolProviderRequest("test", UserMessage.from("hi")))
                .tools().values().iterator().next();

        assertThatThrownBy(() -> executor.executeWithContext(dummyRequest(), null))
                .isInstanceOf(RuntimeException.class);

        assertThat(counter.get()).isZero();
    }

    private static ToolProvider fakeProvider(ToolExecutor executor) {
        ToolSpecification spec = ToolSpecification.builder().name("fake_tool").description("fake").build();
        return request -> ToolProviderResult.builder().add(spec, executor).build();
    }

    private static ToolExecutionRequest dummyRequest() {
        return ToolExecutionRequest.builder().id("1").name("fake_tool").arguments("{}").build();
    }
}
