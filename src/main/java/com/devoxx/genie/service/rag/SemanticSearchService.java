package com.devoxx.genie.service.rag;

import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.devoxx.genie.service.rag.IndexerConstants.FILE_PATH;

@Service
public final class SemanticSearchService {

    private final ChromaEmbeddingService embeddingService;
    private final DevoxxGenieStateService stateService;

    @NotNull
    public static SemanticSearchService getInstance() {
        return ApplicationManager.getApplication().getService(SemanticSearchService.class);
    }

    public SemanticSearchService() {
        this.embeddingService = ApplicationManager.getApplication()
                .getService(ChromaEmbeddingService.class);
        this.stateService = ApplicationManager.getApplication()
                .getService(DevoxxGenieStateService.class);
    }

    /**
     * Search code snippets based on a query string.
     * @param query Search query
     * @return Map of search results with file paths as keys
     */
    public @NotNull Map<String, SearchResult> search(Project project, String query) {
        embeddingService.init(project);

        Embedding queryEmbedding = embeddingService.getEmbeddingModel().embed(query).content();

        Map<String, SearchResult> results = new HashMap<>();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .minScore(stateService.getIndexerMinScore())
                .maxResults(stateService.getMaxSearchResults())
                .build();

        embeddingService.getEmbeddingStore().search(request)
                .matches()
                .forEach(match ->
                        results.put(match.embedded().metadata().getString(FILE_PATH),
                                new SearchResult(match.score(), match.embedded().text())));

        return results;
    }
}