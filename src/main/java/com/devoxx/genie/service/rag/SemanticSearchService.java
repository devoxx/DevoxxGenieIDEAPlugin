package com.devoxx.genie.service.rag;

import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
        this.embeddingService = ChromaEmbeddingService.getInstance();
        this.stateService = DevoxxGenieStateService.getInstance();
    }

    /**
     * Search the project's vector index for chunks semantically similar to the query.
     *
     * <p>Returns one {@link SearchResult} per matching chunk (so multiple chunks from the same
     * file are preserved). Results are ordered by descending score.
     */
    public @NotNull List<SearchResult> search(Project project, String query) {
        embeddingService.init(project);

        Embedding queryEmbedding = embeddingService.getEmbeddingModel().embed(query).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .minScore(stateService.getIndexerMinScore())
                .maxResults(stateService.getIndexerMaxResults())
                .build();

        List<SearchResult> results = new ArrayList<>();
        embeddingService.getEmbeddingStore().search(request)
                .matches()
                .forEach(match -> {
                    String filePath = match.embedded().metadata().getString(FILE_PATH);
                    results.add(new SearchResult(filePath, match.score(), match.embedded().text()));
                });
        return results;
    }
}
