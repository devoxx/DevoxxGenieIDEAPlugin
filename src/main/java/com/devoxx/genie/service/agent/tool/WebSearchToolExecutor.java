package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.service.websearch.WebSearchConfig;
import com.devoxx.genie.service.websearch.WebSearchReadiness;
import com.devoxx.genie.service.websearch.WebSearchService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchResults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Agent tool that performs a live web search using whichever provider the user has
 * selected in Settings → DevoxxGenie → Web search (see {@link WebSearchService}).
 *
 * <p>Returns raw structured results (title, URL, snippet) so the agent can reason
 * over sources directly without an extra LLM summarisation step.
 *
 * <p>Errors are returned as strings (not thrown) so the agent loop recovers gracefully.
 */
@Slf4j
public class WebSearchToolExecutor implements ToolExecutor {

    static final int MAX_SNIPPET_CHARS = 1000;

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        String query = ToolArgumentParser.getString(request.arguments(), "query");
        if (query == null || query.isBlank()) {
            return "Error: 'query' parameter is required.";
        }

        WebSearchConfig config = WebSearchConfig.fromState(DevoxxGenieStateService.getInstance());
        WebSearchReadiness readiness = WebSearchService.readiness(config);
        if (!readiness.isReady()) {
            return "Error: " + readiness.message();
        }
        String providerName = WebSearchService.definition(config.provider()).displayName();

        try {
            WebSearchResults results = WebSearchService.search(config, query);
            List<WebSearchOrganicResult> hits = results.results();
            if (hits == null || hits.isEmpty()) {
                return "No results found for query: " + query;
            }
            return formatResults(query, hits);
        } catch (Exception e) {
            log.warn("web_search via {} failed for query '{}': {}", providerName, query, e.getMessage());
            return "Error: Web search failed (" + providerName + ") - " + e.getMessage() +
                   ". Verify the " + providerName + " settings in " + WebSearchReadiness.SETTINGS_PATH + ".";
        }
    }

    @NotNull
    private static String formatResults(@NotNull String query,
                                        @NotNull List<WebSearchOrganicResult> hits) {
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(hits.size())
          .append(hits.size() == 1 ? " result" : " results")
          .append(" for \"").append(query).append("\":\n\n");
        int i = 1;
        for (WebSearchOrganicResult hit : hits) {
            String title   = hit.title()   != null ? hit.title()          : "(no title)";
            String url     = hit.url()     != null ? hit.url().toString() : "(no url)";
            String snippet = hit.snippet() != null ? hit.snippet()        : "";
            if (snippet.length() > MAX_SNIPPET_CHARS) {
                snippet = snippet.substring(0, MAX_SNIPPET_CHARS) + "... (truncated)";
            }
            sb.append(i++).append(". ").append(title).append("\n")
              .append("   URL: ").append(url).append("\n");
            if (!snippet.isBlank()) {
                sb.append("   ").append(snippet).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
