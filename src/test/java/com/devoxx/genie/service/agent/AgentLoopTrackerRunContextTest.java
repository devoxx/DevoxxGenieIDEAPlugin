package com.devoxx.genie.service.agent;

import com.devoxx.genie.service.agent.loop.AgentRunContext;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AgentLoopTracker} with an {@link AgentRunContext}: read-only call de-duplication,
 * repeat blocking, cache invalidation by mutating tools, metrics and untrusted wrapping.
 */
class AgentLoopTrackerRunContextTest {

    private final AtomicInteger executions = new AtomicInteger();

    private ToolProviderResult tools(AgentRunContext context, int maxCalls) {
        ToolExecutor executor = (req, id) -> {
            executions.incrementAndGet();
            return "result of " + req.name() + " #" + executions.get();
        };
        ToolProvider provider = req -> ToolProviderResult.builder()
                .add(ToolSpecification.builder().name("read_file").build(), executor)
                .add(ToolSpecification.builder().name("edit_file").build(), executor)
                .add(ToolSpecification.builder().name("fetch_page").build(), executor)
                .build();
        AgentLoopTracker tracker = new AgentLoopTracker(provider, maxCalls);
        tracker.setRunContext(context);
        return tracker.provideTools(null);
    }

    private static ToolExecutionRequest call(String name, String args) {
        return ToolExecutionRequest.builder().id("x").name(name).arguments(args).build();
    }

    @Test
    void identicalReadOnlyCall_isServedFromCache() {
        AgentRunContext context = new AgentRunContext(true, false, false, 0);
        ToolProviderResult result = tools(context, 10);
        ToolExecutor read = result.toolExecutorByName("read_file");

        read.execute(call("read_file", "{\"path\":\"A\"}"), null);
        String second = read.execute(call("read_file", "{\"path\": \"A\"}"), null);

        // The original result is still verbatim a step above: a short reference suffices.
        assertThat(executions.get()).isEqualTo(1);
        assertThat(second).startsWith("[Identical to tool call #1").doesNotContain("result of read_file");
        assertThat(context.getMetrics().getCacheHits()).isEqualTo(1);
        assertThat(context.getMetrics().getExecutedToolCalls()).isEqualTo(1);
    }

    @Test
    void olderRepeat_getsTheFullCachedResult_becauseTheOriginalMayBeCompacted() {
        ToolProviderResult result = tools(new AgentRunContext(true, false, false, 0), 20);
        ToolExecutor read = result.toolExecutorByName("read_file");

        String first = read.execute(call("read_file", "{\"path\":\"A\"}"), null);
        for (int i = 0; i < 5; i++) {
            read.execute(call("read_file", "{\"path\":\"other" + i + "\"}"), null);
        }
        String repeat = read.execute(call("read_file", "{\"path\":\"A\"}"), null);

        assertThat(repeat).contains("Same result as tool call #1").endsWith(first);
        assertThat(executions.get()).isEqualTo(6);
    }

    @Test
    void writeBetweenReads_invalidatesCache() {
        ToolProviderResult result = tools(new AgentRunContext(true, false, false, 0), 10);

        result.toolExecutorByName("read_file").execute(call("read_file", "{\"path\":\"A\"}"), null);
        result.toolExecutorByName("edit_file").execute(call("edit_file", "{\"path\":\"A\"}"), null);
        String reread = result.toolExecutorByName("read_file").execute(call("read_file", "{\"path\":\"A\"}"), null);

        assertThat(executions.get()).isEqualTo(3);
        assertThat(reread).isEqualTo("result of read_file #3");
    }

    @Test
    void endlessRepeats_areBlocked_andStillCountTowardsTheLoopLimit() {
        AgentRunContext context = new AgentRunContext(true, false, false, 0);
        ToolProviderResult result = tools(context, 10);
        ToolExecutor read = result.toolExecutorByName("read_file");

        String last = null;
        for (int i = 0; i < 4; i++) {
            last = read.execute(call("read_file", "{\"path\":\"A\"}"), null);
        }

        assertThat(executions.get()).isEqualTo(1);
        assertThat(last).startsWith("Error:").contains("exact arguments");
        assertThat(context.getMetrics().getRepeatBlocks()).isEqualTo(1);
    }

    @Test
    void withoutDeduplication_everyCallExecutes() {
        ToolProviderResult result = tools(AgentRunContext.disabled(), 10);

        result.toolExecutorByName("read_file").execute(call("read_file", "{\"path\":\"A\"}"), null);
        result.toolExecutorByName("read_file").execute(call("read_file", "{\"path\":\"A\"}"), null);

        assertThat(executions.get()).isEqualTo(2);
    }

    @Test
    void fetchPageOutput_isWrappedAsUntrusted() {
        ToolProviderResult result = tools(AgentRunContext.disabled(), 10);

        String output = result.toolExecutorByName("fetch_page").execute(call("fetch_page", "{\"url\":\"https://x\"}"), null);

        assertThat(output).startsWith("<untrusted_tool_output source=\"fetch_page\">");
    }
}
