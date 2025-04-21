package com.devoxx.genie.service.chromadb;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
@Service
public final class ChromaEmbeddingService {
    
    private final DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

    @Getter
    private ChromaEmbeddingStore embeddingStore;

    @NotNull
    public static ChromaEmbeddingService getInstance() {
        return ApplicationManager.getApplication().getService(ChromaEmbeddingService.class);
    }

    public void init(Project project) {
        String url = "http://localhost:" + stateService.getIndexerPort();
        try {
            this.embeddingStore = ChromaEmbeddingStore.builder()
                    .baseUrl(url)
                    .logRequests(true)
                    .logResponses(true)
                    .collectionName(getCollectionName(project))
                    .build();
        } catch (Exception e) {
            log.error("Failed to initialize ChromaDB via {}: {}", url, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the Chroma DB collection name for the given project.
     * @param project the project
     * @return the collection name for the project
     */
    private @NotNull String getCollectionName(@NotNull Project project) {
        return project.getName()
                      .toLowerCase()
                      .replaceAll("[^a-z0-9-]", "-");
    }

    public OllamaEmbeddingModel getEmbeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(stateService.getOllamaModelUrl())
                .modelName("nomic-embed-text")
                .build();
    }
}
