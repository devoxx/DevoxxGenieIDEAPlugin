package com.devoxx.genie.service.agent.loop;

import com.devoxx.genie.service.agent.tool.ToolArgumentParser;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The {@code search_tools} meta tool: finds deferred tools by keyword or exact name and
 * unlocks them, so their full definitions are sent with the following requests.
 */
public class SearchToolsToolExecutor implements ToolExecutor {

    public static final String TOOL_NAME = "search_tools";

    /** Names listed in the tool description before it switches to "… and N more". */
    private static final int MAX_NAMES_IN_DESCRIPTION = 60;

    private final DeferredToolRegistry registry;
    private final AgentRunMetrics metrics;

    public SearchToolsToolExecutor(@NotNull DeferredToolRegistry registry, @NotNull AgentRunMetrics metrics) {
        this.registry = registry;
        this.metrics = metrics;
    }

    /** Specification advertising the hidden tool names so the model knows what exists. */
    public static @NotNull ToolSpecification specification(@NotNull List<String> hiddenToolNames) {
        StringBuilder names = new StringBuilder();
        int shown = Math.min(hiddenToolNames.size(), MAX_NAMES_IN_DESCRIPTION);
        names.append(String.join(", ", hiddenToolNames.subList(0, shown)));
        if (hiddenToolNames.size() > shown) {
            names.append(", … and ").append(hiddenToolNames.size() - shown).append(" more");
        }
        return ToolSpecification.builder()
                .name(TOOL_NAME)
                .description("Load additional tools. " + hiddenToolNames.size() + " MCP tools are available but "
                        + "their definitions are not loaded, to save context: " + names + ". "
                        + "Call this with keywords describing what you need (or exact tool names, comma separated) "
                        + "and the best matching tools become callable from your next step on.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("query", "Keywords for the capability you need, or exact tool names separated by commas")
                        .addIntegerProperty("max_results", "Maximum number of tools to load (default "
                                + DeferredToolRegistry.DEFAULT_SEARCH_RESULTS + ", max "
                                + DeferredToolRegistry.MAX_SEARCH_RESULTS + ")")
                        .required("query")
                        .build())
                .build();
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        String query = ToolArgumentParser.getString(request.arguments(), "query");
        if (query == null || query.isBlank()) {
            return "Error: 'query' parameter is required.";
        }
        int max = ToolArgumentParser.getInt(request.arguments(), "max_results", DeferredToolRegistry.DEFAULT_SEARCH_RESULTS);
        List<DeferredToolRegistry.Match> matches = registry.search(query, max);
        if (matches.isEmpty()) {
            return "No tools match '" + query + "'. Available tools not yet loaded: "
                    + String.join(", ", registry.hiddenToolNames())
                    + ". Try other keywords or exact names.";
        }

        int newlyUnlocked = 0;
        StringBuilder sb = new StringBuilder("These tools are now loaded and can be called from your next step:\n");
        for (DeferredToolRegistry.Match match : matches) {
            ToolSpecification spec = match.spec();
            if (registry.unlock(spec.name())) newlyUnlocked++;
            sb.append("- ").append(spec.name()).append(JsonSchemaSupport.signature(spec.parameters()));
            if (spec.description() != null && !spec.description().isBlank()) {
                sb.append(": ").append(spec.description().strip());
            }
            sb.append('\n');
        }
        metrics.recordToolsUnlocked(newlyUnlocked);
        return sb.toString().stripTrailing();
    }
}
