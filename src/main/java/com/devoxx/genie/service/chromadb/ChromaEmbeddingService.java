package com.devoxx.genie.service.chromadb;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

@Slf4j
@Service
public final class ChromaEmbeddingService {

    static final String EMBEDDING_MODEL_NAME = "nomic-embed-text";

    private final DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

    private EmbeddingStore<TextSegment> embeddingStore;

    /**
     * Test-only override for the embedding model. When non-null, {@link #getEmbeddingModel()}
     * returns this instead of building a new {@link OllamaEmbeddingModel}.
     */
    @Setter
    private EmbeddingModel embeddingModelOverride;

    // Cached embedding model; rebuilt when the Ollama URL or model name changes.
    private EmbeddingModel cachedEmbeddingModel;
    private String cachedEmbeddingUrl;
    private String cachedEmbeddingModelName;

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

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }

    @TestOnly
    public void setEmbeddingStore(EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingStore = embeddingStore;
    }

    private @NotNull String getCollectionName(@NotNull Project project) {
        return project.getName()
                      .toLowerCase()
                      .replaceAll("[^a-z0-9-]", "-");
    }

    /**
     * Returns a cached {@link OllamaEmbeddingModel}, rebuilding it only when the configured
     * Ollama URL or model name changes. The previous implementation built a fresh model
     * (and underlying HTTP client) on every call, costing a setup + handshake per chunk
     * during indexing.
     */
    public synchronized EmbeddingModel getEmbeddingModel() {
        if (embeddingModelOverride != null) {
            return embeddingModelOverride;
        }
        String url = stateService.getOllamaModelUrl();
        if (cachedEmbeddingModel == null
                || !EMBEDDING_MODEL_NAME.equals(cachedEmbeddingModelName)
                || url == null || !url.equals(cachedEmbeddingUrl)) {
            cachedEmbeddingModel = OllamaEmbeddingModel.builder()
                    .baseUrl(url)
                    .modelName(EMBEDDING_MODEL_NAME)
                    .build();
            cachedEmbeddingUrl = url;
            cachedEmbeddingModelName = EMBEDDING_MODEL_NAME;
        }
        return cachedEmbeddingModel;
    }
}
