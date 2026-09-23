package com.devoxx.genie.service.agent.loop;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DeferredToolRegistry}, {@link SearchToolsToolExecutor} and
 * {@link DeferringToolProvider}.
 */
class DeferredToolsTest {

    private static ToolSpecification spec(String name, String description) {
        return ToolSpecification.builder()
                .name(name)
                .description(description)
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("id", "identifier")
                        .addIntegerProperty("limit", "max results")
                        .required("id")
                        .build())
                .build();
    }

    private static ToolExecutionRequest request(String name, String args) {
        return ToolExecutionRequest.builder().id("1").name(name).arguments(args).build();
    }

    private static DeferredToolRegistry registryWith(ToolSpecification... specs) {
        DeferredToolRegistry registry = new DeferredToolRegistry();
        registry.defer(List.of(specs));
        return registry;
    }

    @Test
    void visible_hidesLockedDeferredToolsOnly() {
        DeferredToolRegistry registry = registryWith(spec("jira_get_issue", "Get a Jira issue"));
        ToolSpecification builtIn = spec("read_file", "Read a file");
        ToolSpecification deferred = spec("jira_get_issue", "Get a Jira issue");

        assertThat(registry.visible(List.of(builtIn, deferred))).containsExactly(builtIn);

        registry.unlock("jira_get_issue");
        assertThat(registry.visible(List.of(builtIn, deferred))).containsExactly(builtIn, deferred);
    }

    @Test
    void search_ranksByNameThenDescription() {
        DeferredToolRegistry registry = registryWith(
                spec("jira_get_issue", "Get a Jira issue by key"),
                spec("github_list_issues", "List GitHub issues"),
                spec("slack_post_message", "Post a message to a Slack channel"));

        List<DeferredToolRegistry.Match> matches = registry.search("jira issue", 5);

        assertThat(matches).extracting(m -> m.spec().name())
                .first().isEqualTo("jira_get_issue");
        assertThat(matches).extracting(m -> m.spec().name()).doesNotContain("slack_post_message");
    }

    @Test
    void search_exactNamesWin_withSelectPrefix() {
        DeferredToolRegistry registry = registryWith(
                spec("jira_get_issue", "Get a Jira issue"),
                spec("slack_post_message", "Post a message"));

        List<DeferredToolRegistry.Match> matches = registry.search("select:slack_post_message", 5);

        assertThat(matches).extracting(m -> m.spec().name()).containsExactly("slack_post_message");
    }

    @Test
    void tokens_splitCamelCaseAndSnakeCase() {
        assertThat(DeferredToolRegistry.tokens("getIssueById snake_case"))
                .contains("get", "issue", "by", "id", "snake", "case");
    }

    @Test
    void searchTools_unlocksMatchesAndDescribesThem() {
        DeferredToolRegistry registry = registryWith(spec("jira_get_issue", "Get a Jira issue"));
        AgentRunMetrics metrics = new AgentRunMetrics();
        SearchToolsToolExecutor executor = new SearchToolsToolExecutor(registry, metrics);

        String result = executor.execute(request(SearchToolsToolExecutor.TOOL_NAME, "{\"query\":\"jira\"}"), null);

        assertThat(result).contains("jira_get_issue(id: string, limit?: integer): Get a Jira issue");
        assertThat(registry.isUnlocked("jira_get_issue")).isTrue();
        assertThat(metrics.getToolsUnlocked()).isEqualTo(1);
    }

    @Test
    void searchTools_noMatch_listsAvailableNames() {
        DeferredToolRegistry registry = registryWith(spec("jira_get_issue", "Get a Jira issue"));
        SearchToolsToolExecutor executor = new SearchToolsToolExecutor(registry, new AgentRunMetrics());

        String result = executor.execute(request(SearchToolsToolExecutor.TOOL_NAME, "{\"query\":\"kubernetes\"}"), null);

        assertThat(result).contains("No tools match").contains("jira_get_issue");
    }

    @Test
    void searchTools_requiresQuery() {
        SearchToolsToolExecutor executor = new SearchToolsToolExecutor(new DeferredToolRegistry(), new AgentRunMetrics());
        assertThat(executor.execute(request(SearchToolsToolExecutor.TOOL_NAME, "{}"), null)).startsWith("Error:");
    }

    private static ToolProvider mcpProviderWith(int toolCount) {
        ToolExecutor executor = (req, id) -> "ran " + req.name();
        return req -> {
            ToolProviderResult.Builder builder = ToolProviderResult.builder();
            IntStream.range(0, toolCount).forEach(i -> builder.add(spec("mcp_tool_" + i, "MCP tool " + i), executor));
            return builder.build();
        };
    }

    @Test
    void deferringProvider_belowThreshold_passesThrough() {
        AgentRunContext context = new AgentRunContext(false, false, true, 5);
        ToolProviderResult result = new DeferringToolProvider(mcpProviderWith(5), context).provideTools(null);

        assertThat(result.tools()).hasSize(5);
        assertThat(context.getDeferredTools().isActive()).isFalse();
    }

    @Test
    void deferringProvider_disabled_passesThrough() {
        AgentRunContext context = new AgentRunContext(false, false, false, 5);
        ToolProviderResult result = new DeferringToolProvider(mcpProviderWith(30), context).provideTools(null);

        assertThat(result.tools()).hasSize(30);
        assertThat(context.getDeferredTools().isActive()).isFalse();
    }

    @Test
    void deferringProvider_aboveThreshold_defersAndAddsSearchTools() {
        AgentRunContext context = new AgentRunContext(false, false, true, 5);
        ToolProviderResult result = new DeferringToolProvider(mcpProviderWith(8), context).provideTools(null);

        assertThat(result.tools()).hasSize(9);
        assertThat(result.toolExecutorByName(SearchToolsToolExecutor.TOOL_NAME)).isNotNull();
        assertThat(context.getDeferredTools().hiddenToolNames()).hasSize(8);
        ToolSpecification searchSpec = result.tools().keySet().stream()
                .filter(s -> s.name().equals(SearchToolsToolExecutor.TOOL_NAME)).findFirst().orElseThrow();
        assertThat(searchSpec.description()).contains("8 MCP tools").contains("mcp_tool_0");
    }

    @Test
    void deferringProvider_directCallUnlocksTool() {
        AgentRunContext context = new AgentRunContext(false, false, true, 1);
        ToolProviderResult result = new DeferringToolProvider(mcpProviderWith(3), context).provideTools(null);

        String output = result.toolExecutorByName("mcp_tool_2").execute(request("mcp_tool_2", "{\"id\":\"x\"}"), null);

        assertThat(output).isEqualTo("ran mcp_tool_2");
        assertThat(context.getDeferredTools().isUnlocked("mcp_tool_2")).isTrue();
        assertThat(context.getMetrics().getToolsUnlocked()).isEqualTo(1);
    }
}
