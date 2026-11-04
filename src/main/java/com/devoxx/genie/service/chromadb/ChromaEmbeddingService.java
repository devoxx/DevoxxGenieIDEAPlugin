package com.devoxx.genie.service.chromadb;

import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import com.devoxx.genie.service.rag.manifest.IndexManifestService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public final class ChromaEmbeddingService {

    private final DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

    private EmbeddingStore<TextSegment> embeddingStore;

    /**
     * Test-only override for the embedding model. When non-null, {@link #getEmbeddingModel()}
     * returns this instead of building a new {@link OllamaEmbeddingModel}.
     */
    @Setter
    private EmbeddingModel embeddingModelOverride;

    /**
     * Embedding clients keyed by Ollama URL + fingerprint. More than one can be live at once:
     * after a model change, search keeps querying with the model the index was built with
     * while a re-index embeds with the newly configured one.
     */
    private final Map<String, EmbeddingModel> embeddingModels = new HashMap<>();

    @NotNull
    public static ChromaEmbeddingService getInstance() {
        return ApplicationManager.getApplication().getService(ChromaEmbeddingService.class);
    }

    public void init(Project project) {
        String url = "http://localhost:" + stateService.getIndexerPort();
        try {
            this.embeddingStore = ChromaEmbeddingStore.builder()
                    .apiVersion(ChromaApiVersion.V2)
                    .tenantName("default_tenant")
                    .databaseName("default_database")
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

    /** The embedding setup the settings ask for; a full re-index builds the index with it. */
    public @NotNull EmbeddingFingerprint configuredFingerprint() {
        return EmbeddingFingerprint.ollama(stateService.getRagEmbeddingModelName());
    }

    /**
     * The embedding setup the project's existing index was built with, or the configured one
     * when the project has no index yet. Queries must be embedded with this, so an index keeps
     * working after the model is changed in settings until the user re-indexes.
     */
    public @NotNull EmbeddingFingerprint indexFingerprint(@NotNull Project project) {
        return IndexManifestService.getInstance().forProject(project).indexFingerprint()
                .map(EmbeddingFingerprint::parse)
                .orElseGet(this::configuredFingerprint);
    }

    /** Embedding model for the {@linkplain #configuredFingerprint() configured} setup. */
    public EmbeddingModel getEmbeddingModel() {
        return getEmbeddingModel(configuredFingerprint());
    }

    /**
     * Returns a cached {@link OllamaEmbeddingModel} for {@code fingerprint}, rebuilding it only
     * when the configured Ollama URL changes. Building a model per call would cost a client
     * setup + handshake per chunk during indexing.
     */
    public synchronized EmbeddingModel getEmbeddingModel(@NotNull EmbeddingFingerprint fingerprint) {
        if (embeddingModelOverride != null) {
            return embeddingModelOverride;
        }
        String url = stateService.getOllamaModelUrl();
        return embeddingModels.computeIfAbsent(url + "|" + fingerprint.id(),
                key -> OllamaEmbeddingModel.builder()
                        .baseUrl(url)
                        .modelName(fingerprint.modelName())
                        .build());
    }
}
