package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.service.rag.SearchResult;
import com.devoxx.genie.service.rag.SemanticSearchService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SemanticSearchToolExecutorTest {

    @Mock
    private Project project;

    @Mock
    private SemanticSearchService semanticSearchService;

    private SemanticSearchToolExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new SemanticSearchToolExecutor(project) {
            @Override
            SemanticSearchService searchService() {
                return semanticSearchService;
            }
        };
    }

    private static ToolExecutionRequest request(String args) {
        return ToolExecutionRequest.builder()
                .name("semantic_search")
                .arguments(args)
                .build();
    }

    @Test
    void execute_missingQuery_returnsError() {
        String result = executor.execute(request("{}"), null);
        assertThat(result).contains("Error").contains("query");
    }

    @Test
    void execute_blankQuery_returnsError() {
        String result = executor.execute(request("{\"query\": \"   \"}"), null);
        assertThat(result).contains("Error").contains("query");
    }

    @Test
    void execute_nullQuery_returnsError() {
        String result = executor.execute(request("{\"query\": null}"), null);
        assertThat(result).contains("Error").contains("query");
    }

    @Test
    void execute_invalidJson_returnsError() {
        String result = executor.execute(request("not json"), null);
        // ToolArgumentParser swallows the parse error and returns null for the field,
        // which surfaces as the "query is required" error — that's the user-facing
        // contract the agent should recover from.
        assertThat(result).contains("Error");
    }

    @Test
    void execute_emptyResults_returnsNoMatchesMessage() {
        when(semanticSearchService.search(eq(project), eq("vector databases"))).thenReturn(List.of());

        String result = executor.execute(request("{\"query\": \"vector databases\"}"), null);

        assertThat(result)
                .contains("No semantic matches")
                .contains("vector databases");
    }

    @Test
    void execute_searchThrows_returnsUsefulError() {
        when(semanticSearchService.search(any(Project.class), any(String.class)))
                .thenThrow(new RuntimeException("ChromaDB connection refused"));

        String result = executor.execute(request("{\"query\": \"anything\"}"), null);

        assertThat(result)
                .contains("Error")
                .contains("Semantic search is unavailable")
                .contains("ChromaDB connection refused")
                .contains("Docker");
    }

    @Test
    void execute_validResults_formatsAsRankedList() {
        List<SearchResult> results = List.of(
                new SearchResult("workshop/slides/rag-intro.html", 0.84, "RAG combines retrieval with generation."),
                new SearchResult("workshop/slides/rag-eval.html", 0.79, "Evaluating RAG quality requires...")
        );
        when(semanticSearchService.search(eq(project), eq("which slides discuss RAG"))).thenReturn(results);

        String result = executor.execute(request("{\"query\": \"which slides discuss RAG\"}"), null);

        assertThat(result)
                .contains("Found 2 semantic matches")
                .contains("which slides discuss RAG")
                .contains("1. workshop/slides/rag-intro.html")
                .contains("(score: 0.84)")
                .contains("RAG combines retrieval with generation.")
                .contains("2. workshop/slides/rag-eval.html")
                .contains("(score: 0.79)")
                .contains("Evaluating RAG quality requires...");
    }

    @Test
    void execute_singleResult_usesSingularPhrasing() {
        when(semanticSearchService.search(any(Project.class), any(String.class)))
                .thenReturn(List.of(new SearchResult("a.txt", 0.5, "x")));

        String result = executor.execute(request("{\"query\": \"q\"}"), null);

        assertThat(result).contains("Found 1 semantic match for")
                .doesNotContain("matches for");
    }

    @Test
    void execute_truncatesLongSnippets() {
        String huge = "x".repeat(SemanticSearchToolExecutor.MAX_SNIPPET_CHARS + 500);
        when(semanticSearchService.search(any(Project.class), any(String.class)))
                .thenReturn(List.of(new SearchResult("big.txt", 0.7, huge)));

        String result = executor.execute(request("{\"query\": \"q\"}"), null);

        assertThat(result).contains("snippet truncated");
        // Body should not contain the full string (only the truncated portion).
        assertThat(result).doesNotContain("x".repeat(SemanticSearchToolExecutor.MAX_SNIPPET_CHARS + 1));
    }

    @Test
    void execute_handlesNullFieldsInSearchResult() {
        when(semanticSearchService.search(any(Project.class), any(String.class)))
                .thenReturn(List.of(new SearchResult(null, null, null)));

        String result = executor.execute(request("{\"query\": \"q\"}"), null);

        // Must not NPE — placeholders fill in for missing data.
        assertThat(result).contains("(unknown)").contains("(score: 0.00)");
    }
}
