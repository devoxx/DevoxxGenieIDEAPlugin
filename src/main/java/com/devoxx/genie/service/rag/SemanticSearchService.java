package com.devoxx.genie.service.rag;

import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.service.rag.rerank.NoOpReranker;
import com.devoxx.genie.service.rag.rerank.OllamaReranker;
import com.devoxx.genie.service.rag.rerank.Reranker;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.ExpandingQueryTransformer;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import static com.devoxx.genie.service.rag.IndexerConstants.FILE_PATH;

@Slf4j
@Service
public final class SemanticSearchService {

    private final ChromaEmbeddingService embeddingService;
    private final DevoxxGenieStateService stateService;
    /**
     * Reranker used when the "Rerank results" setting is enabled. Defaults to
     * {@link OllamaReranker} in production; tests use {@link #setReranker(Reranker)}
     * to substitute a deterministic stand-in. {@code volatile} because the field can be
     * swapped from one thread (a test {@code @BeforeEach}) and read from another (the
     * thread running {@link #search(Project, String, ChatModel)}).
     */
    private volatile @NotNull Reranker reranker = new OllamaReranker();

    @NotNull
    public static SemanticSearchService getInstance() {
        return ApplicationManager.getApplication().getService(SemanticSearchService.class);
    }

    public SemanticSearchService() {
        this.embeddingService = ChromaEmbeddingService.getInstance();
        this.stateService = DevoxxGenieStateService.getInstance();
    }

    /**
     * Test seam — allows swapping in a {@link NoOpReranker} / mocked reranker without
     * monkey-patching static factories. Not used in production code.
     */
    @TestOnly
    public void setReranker(@NotNull Reranker reranker) {
        this.reranker = reranker;
    }

    /**
     * Single-query search — kept for callers that don't have a chat model available
     * (find-command, integration tests). Equivalent to
     * {@link #search(Project, String, ChatModel)} with a null model.
     */
    public @NotNull List<SearchResult> search(Project project, String query) {
        return search(project, query, null);
    }

    /**
     * Search the project's vector index for chunks semantically similar to the query.
     *
     * <p>When {@code chatModel} is non-null AND query expansion is enabled in settings, the
     * query is first paraphrased into N variants via langchain4j's {@link
     * ExpandingQueryTransformer}; each variant is retrieved independently and the results
     * are fused via Reciprocal Rank Fusion ({@link QueryExpansionFuser}). This addresses
     * meta-style user queries ("where do we discuss X?") which embed near boilerplate rather
     * than near the content the user actually wants.
     *
     * <p>When expansion is disabled or unavailable, falls back to single-query embedding +
     * store lookup — identical to the previous behavior.
     *
     * <p>When the "Rerank results" toggle is enabled, the retrieval shortlist (sized by
     * {@code rerankerShortlistSize}) is passed to the configured {@link Reranker} which
     * returns the top {@code indexerMaxResults} entries. When the toggle is OFF, retrieval
     * returns {@code indexerMaxResults} directly and the reranker is bypassed.
     *
     * <p>Returns one {@link SearchResult} per matching chunk (so multiple chunks from the
     * same file are preserved). Results are ordered by descending score.
     */
    public @NotNull List<SearchResult> search(Project project, String query, @Nullable ChatModel chatModel) {
        embeddingService.init(project);

        boolean rerank = Boolean.TRUE.equals(stateService.getRerankResults());
        int finalTopN = stateService.getIndexerMaxResults();
        // When rerank is on, retrieval returns the wider shortlist so the reranker has room
        // to reorder; otherwise retrieval returns exactly what the prompt will see.
        int retrievalMax = rerank
                ? Math.max(finalTopN, safeShortlistSize())
                : finalTopN;

        List<SearchResult> retrieved;
        if (chatModel != null && Boolean.TRUE.equals(stateService.getRagQueryExpansionEnabled())) {
            retrieved = searchWithExpansion(query, chatModel, retrievalMax);
        } else {
            retrieved = singleQuerySearch(query, retrievalMax);
        }

        if (!rerank || retrieved.isEmpty()) {
            return retrieved;
        }

        long timeoutMs = stateService.getRerankerTimeoutMs() != null
                ? stateService.getRerankerTimeoutMs()
                : 2000L;
        try {
            return reranker.rerank(query, retrieved, finalTopN, timeoutMs);
        } catch (Exception e) {
            // Reranker contract is best-effort, but defend against impls that throw rather
            // than falling back internally — we still need to return *something* useful.
            log.warn("Reranker threw ({}); falling back to retrieval order", e.getMessage());
            int n = Math.min(finalTopN, retrieved.size());
            return new ArrayList<>(retrieved.subList(0, n));
        }
    }

    private int safeShortlistSize() {
        Integer cfg = stateService.getRerankerShortlistSize();
        return (cfg == null || cfg <= 0) ? 30 : cfg;
    }

    private @NotNull List<SearchResult> searchWithExpansion(String query,
                                                            @NotNull ChatModel chatModel,
                                                            int maxResults) {
        int n = stateService.getRagQueryExpansionN() == null ? 3 : stateService.getRagQueryExpansionN();
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        variants.add(query); // keep the original as the unexpanded baseline
        try {
            ExpandingQueryTransformer expander = new ExpandingQueryTransformer(chatModel, n);
            Collection<Query> expanded = expander.transform(Query.from(query));
            expanded.forEach(q -> variants.add(q.text()));
        } catch (Exception e) {
            // If the LLM call fails (timeout, key, format), fall back to the original alone
            // rather than aborting the whole prompt. The single-query baseline is still useful.
            log.warn("Query expansion failed ({}); falling back to original query", e.getMessage());
        }
        return QueryExpansionFuser.expandAndFuse(
                variants,
                v -> singleQuerySearch(v, maxResults),
                maxResults);
    }

    private @NotNull List<SearchResult> singleQuerySearch(@NotNull String query, int maxResults) {
        Embedding queryEmbedding = embeddingService.getEmbeddingModel().embed(query).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .minScore(stateService.getIndexerMinScore())
                .maxResults(maxResults)
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
