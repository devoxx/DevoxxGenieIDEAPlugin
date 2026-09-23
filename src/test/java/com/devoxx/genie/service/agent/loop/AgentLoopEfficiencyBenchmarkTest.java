package com.devoxx.genie.service.agent.loop;

import com.devoxx.genie.service.agent.AgentLoopTracker;
import com.devoxx.genie.service.agent.tool.CompositeToolProvider;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic, credential-free benchmark of the agent loop efficiency features, run through
 * the real langchain4j AiServices tool loop with a scripted model.
 *
 * <p>The scripted "research" task mirrors a typical agent run: a broad search, several large
 * file reads, a repeated read and a repeated search (models do this), then one call to an MCP
 * tool out of a catalog of 30. It runs twice — every feature off (baseline) and the defaults
 * (compaction, de-duplication, deferred MCP tools) — and compares what the model is sent and
 * how many tools actually execute. The printed table is the "benchmark"; the assertions keep
 * the improvements from regressing.
 */
class AgentLoopEfficiencyBenchmarkTest {

    private static final int FILE_CHARS = 8_000;
    private static final int MCP_TOOL_COUNT = 30;
    private static final String MCP_TARGET = "jira_get_issue";

    interface Assistant {
        String chat(String userMessage);
    }

    /** Result of one scripted run. */
    record Run(long charsSentToModel, int roundTrips, int toolExecutions, String answer, AgentRunMetrics metrics) {
    }

    /**
     * Scripted model: works through a fixed plan of tool calls. When the MCP tool it needs is
     * not among the advertised tools but {@code search_tools} is, it searches first — as a real
     * model would.
     */
    private static final class ScriptedResearchModel implements ChatModel {
        private final Deque<ToolExecutionRequest> plan = new ArrayDeque<>();
        private long charsReceived;
        private int requests;
        private int ids;

        ScriptedResearchModel() {
            plan.add(call("search_files", "{\"pattern\":\"OrderService\"}"));
            plan.add(call("read_file", "{\"path\":\"src/OrderService.java\"}"));
            plan.add(call("read_file", "{\"path\":\"src/OrderRepository.java\"}"));
            plan.add(call("read_file", "{\"path\":\"src/Order.java\"}"));
            plan.add(call("read_file", "{\"path\":\"src/OrderService.java\"}"));   // repeated read
            plan.add(call("search_files", "{\"pattern\":\"OrderService\"}"));      // repeated search
            plan.add(call("read_file", "{\"path\":\"src/OrderController.java\"}"));
            plan.add(call(MCP_TARGET, "{\"key\":\"SHOP-42\"}"));
        }

        private ToolExecutionRequest call(String name, String args) {
            return ToolExecutionRequest.builder().id("call-" + (++ids)).name(name).arguments(args).build();
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            requests++;
            charsReceived += AgentRequestTransformer.estimateChars(request);

            if (plan.isEmpty()) {
                List<ChatMessage> messages = request.messages();
                String last = ((ToolExecutionResultMessage) messages.get(messages.size() - 1)).text();
                return ChatResponse.builder().aiMessage(AiMessage.from("Answer based on: " + last)).build();
            }
            ToolExecutionRequest next = plan.peek();
            boolean advertised = request.toolSpecifications().stream().anyMatch(s -> s.name().equals(next.name()));
            boolean canSearch = request.toolSpecifications().stream()
                    .anyMatch(s -> s.name().equals(SearchToolsToolExecutor.TOOL_NAME));
            if (!advertised && canSearch) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(call(SearchToolsToolExecutor.TOOL_NAME, "{\"query\":\"jira issue\"}")))
                        .build();
            }
            return ChatResponse.builder().aiMessage(AiMessage.from(plan.poll())).build();
        }
    }

    private static ToolSpecification spec(String name, String description) {
        return ToolSpecification.builder()
                .name(name)
                .description(description)
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("key", "Identifier of the item, e.g. PROJECT-123")
                        .addStringProperty("fields", "Comma separated list of fields to return")
                        .addIntegerProperty("limit", "Maximum number of results")
                        .required("key")
                        .build())
                .build();
    }

    private static ToolProvider builtInTools(AtomicInteger executions) {
        ToolExecutor read = (req, id) -> {
            executions.incrementAndGet();
            return "// " + req.arguments() + "\n" + "c".repeat(FILE_CHARS);
        };
        ToolExecutor search = (req, id) -> {
            executions.incrementAndGet();
            return "src/OrderService.java:12: class OrderService\n".repeat(60);
        };
        return req -> ToolProviderResult.builder()
                .add(ToolSpecification.builder().name("read_file").description("Read a file")
                        .parameters(JsonObjectSchema.builder().addStringProperty("path", "path").required("path").build())
                        .build(), read)
                .add(ToolSpecification.builder().name("search_files").description("Search files")
                        .parameters(JsonObjectSchema.builder().addStringProperty("pattern", "regex").required("pattern").build())
                        .build(), search)
                .build();
    }

    private static ToolProvider mcpTools(AtomicInteger executions) {
        ToolExecutor mcp = (req, id) -> {
            executions.incrementAndGet();
            return "jira ok: " + req.name();
        };
        String padding = " This tool talks to a remote service and returns structured JSON; see the server"
                + " documentation for the exact response format, pagination rules and rate limits.";
        return req -> {
            ToolProviderResult.Builder builder = ToolProviderResult.builder();
            builder.add(spec(MCP_TARGET, "Get a Jira issue by key." + padding), mcp);
            for (int i = 1; i < MCP_TOOL_COUNT; i++) {
                builder.add(spec("service_" + i + "_operation", "Operation " + i + " of an unrelated service." + padding), mcp);
            }
            return builder.build();
        };
    }

    private static Run run(AgentRunContext context) {
        AtomicInteger executions = new AtomicInteger();
        ScriptedResearchModel model = new ScriptedResearchModel();

        ToolProvider composite = new CompositeToolProvider(List.of(
                builtInTools(executions),
                new DeferringToolProvider(mcpTools(executions), context)));
        AgentLoopTracker tracker = new AgentLoopTracker(composite, 50);
        tracker.setRunContext(context);

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .toolProvider(tracker)
                .maxToolCallingRoundTrips(tracker.getMaxToolCallingRoundTrips())
                .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(200))
                .chatRequestTransformer(context.requestTransformer())
                .build();

        String answer = assistant.chat("How are orders persisted, and what does SHOP-42 ask for?");
        return new Run(model.charsReceived, model.requests, executions.get(), answer, context.getMetrics());
    }

    @Test
    void efficiencyFeatures_reduceWhatIsSentAndWhatIsExecuted() {
        Run baseline = run(AgentRunContext.disabled());
        Run optimized = run(new AgentRunContext(true, true, true, 20));

        System.out.printf("%n%-12s %12s %11s %15s%n", "variant", "chars sent", "round trips", "tool executions");
        System.out.printf("%-12s %12d %11d %15d%n", "baseline", baseline.charsSentToModel(), baseline.roundTrips(), baseline.toolExecutions());
        System.out.printf("%-12s %12d %11d %15d%n", "optimized", optimized.charsSentToModel(), optimized.roundTrips(), optimized.toolExecutions());
        System.out.println(optimized.metrics().summary());

        // Both variants complete the task and reach the MCP tool.
        assertThat(baseline.answer()).contains("jira ok: " + MCP_TARGET);
        assertThat(optimized.answer()).contains("jira ok: " + MCP_TARGET);

        // Repeated read and search are served from the cache: 2 fewer real executions.
        assertThat(baseline.toolExecutions()).isEqualTo(8);
        assertThat(optimized.toolExecutions()).isEqualTo(6);
        assertThat(optimized.metrics().getCacheHits()).isEqualTo(2);

        // One extra round trip for search_tools, but far less sent overall.
        assertThat(optimized.roundTrips()).isEqualTo(baseline.roundTrips() + 1);
        assertThat(optimized.charsSentToModel()).isLessThan(baseline.charsSentToModel() / 2);
        assertThat(optimized.metrics().getCompactedChars()).isPositive();
        assertThat(optimized.metrics().getHiddenToolSpecs()).isPositive();
        assertThat(optimized.metrics().getToolsUnlocked()).isGreaterThanOrEqualTo(1);

        // The transformer measured exactly what the model received.
        assertThat(optimized.metrics().getRequestChars()).isEqualTo(optimized.charsSentToModel());
        assertThat(optimized.metrics().getRoundTrips()).isEqualTo(optimized.roundTrips());
    }
}
