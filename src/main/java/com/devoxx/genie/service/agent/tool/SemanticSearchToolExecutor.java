package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.service.rag.SearchResult;
import com.devoxx.genie.service.rag.SemanticSearchService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * Agent tool that exposes RAG semantic search to the LLM. Wraps
 * {@link SemanticSearchService#search(Project, String)} so the model can choose
 * semantic retrieval for conceptual queries instead of falling back to lexical
 * tools like {@code search_files}.
 *
 * <p>Query expansion is intentionally NOT used here: the agent loop already
 * orchestrates its own search strategy, so spending another LLM round-trip on
 * expansion inside every tool call is wasteful.
 *
 * <p>Errors are returned as strings (not thrown) so the agent loop can recover
 * by trying a different tool.
 */
@Slf4j
public class SemanticSearchToolExecutor implements ToolExecutor {

    static final int MAX_SNIPPET_CHARS = 1500;

    private final Project project;

    public SemanticSearchToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        String query = ToolArgumentParser.getString(request.arguments(), "query");
        if (query == null || query.isBlank()) {
            return "Error: 'query' parameter is required.";
        }

        List<SearchResult> results;
        try {
            results = searchService().search(project, query);
        } catch (Exception e) {
            log.warn("semantic_search failed for query '{}': {}", query, e.getMessage());
            return "Error: Semantic search is unavailable - " + e.getMessage()
                    + ". Verify Docker, ChromaDB, and the embedding model (e.g. nomic-embed-text in Ollama) are running, "
                    + "and that the project has been indexed in DevoxxGenie settings.";
        }

        if (results.isEmpty()) {
            return "No semantic matches found for query: " + query
                    + ". The index may not contain content related to this query, or no chunk passed the configured minimum score.";
        }

        return formatResults(query, results);
    }

    @NotNull SemanticSearchService searchService() {
        return SemanticSearchService.getInstance();
    }

    private static @NotNull String formatResults(@NotNull String query, @NotNull List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(results.size())
                .append(" semantic match").append(results.size() == 1 ? "" : "es")
                .append(" for \"").append(query).append("\":\n\n");

        int i = 1;
        for (SearchResult r : results) {
            String filePath = r.filePath() != null ? r.filePath() : "(unknown)";
            double score = r.score() != null ? r.score() : 0.0;
            String content = r.content() != null ? r.content() : "";
            if (content.length() > MAX_SNIPPET_CHARS) {
                content = content.substring(0, MAX_SNIPPET_CHARS) + "\n... (snippet truncated)";
            }

            sb.append(i++).append(". ").append(filePath)
                    .append(" (score: ").append(String.format(Locale.ROOT, "%.2f", score)).append(")\n")
                    .append(content).append("\n\n");
        }
        return sb.toString().stripTrailing();
    }
}
