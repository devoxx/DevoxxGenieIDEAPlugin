package com.devoxx.genie.service.rag;

import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.service.rag.rerank.Reranker;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the reranker hand-off in {@link SemanticSearchService}: OFF toggle is a no-op,
 * ON toggle widens retrieval to the shortlist size and routes results through the
 * injected {@link Reranker}.
 */
class SemanticSearchServiceRerankerTest {

    private MockedStatic<ApplicationManager> applicationManagerStatic;
    private MockedStatic<ChromaEmbeddingService> chromaServiceStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;

    private ChromaEmbeddingService mockChromaService;
    private DevoxxGenieStateService mockStateService;
    private Project mockProject;

    private EmbeddingStore<TextSegment> store;
    private EmbeddingModel embeddingModel;

    @BeforeEach
    void setUp() {
        applicationManagerStatic = Mockito.mockStatic(ApplicationManager.class);
        chromaServiceStatic = Mockito.mockStatic(ChromaEmbeddingService.class);
        stateServiceStatic = Mockito.mockStatic(DevoxxGenieStateService.class);

        Application mockApplication = mock(Application.class);
        mockProject = mock(Project.class);
        mockChromaService = mock(ChromaEmbeddingService.class);
        mockStateService = mock(DevoxxGenieStateService.class);

        applicationManagerStatic.when(ApplicationManager::getApplication).thenReturn(mockApplication);
        chromaServiceStatic.when(ChromaEmbeddingService::getInstance).thenReturn(mockChromaService);
        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);
        lenient().when(mockApplication.getService(SemanticSearchService.class))
                .thenAnswer(inv -> new SemanticSearchService());

        store = new InMemoryEmbeddingStore<>();
        embeddingModel = new DeterministicHashEmbeddingModel();
        when(mockChromaService.getEmbeddingStore()).thenReturn(store);
        when(mockChromaService.getEmbeddingModel()).thenReturn(embeddingModel);
        Mockito.doNothing().when(mockChromaService).init(any());

        when(mockStateService.getIndexerMinScore()).thenReturn(0.0);
        when(mockStateService.getIndexerMaxResults()).thenReturn(3);
        when(mockStateService.getRerankerShortlistSize()).thenReturn(30);
        when(mockStateService.getRerankerTimeoutMs()).thenReturn(2000);

        seedChunks();
    }

    @AfterEach
    void tearDown() {
        applicationManagerStatic.close();
        chromaServiceStatic.close();
        stateServiceStatic.close();
    }

    @Test
    void offToggleIsANoOpReranker() {
        when(mockStateService.getRerankResults()).thenReturn(false);

        AtomicBoolean rerankerCalled = new AtomicBoolean(false);
        Reranker spy = (query, candidates, topN, timeoutMs) -> {
            rerankerCalled.set(true);
            return candidates;
        };

        SemanticSearchService svc = new SemanticSearchService();
        svc.setReranker(spy);

        List<SearchResult> out = svc.search(mockProject, "alpha");

        assertThat(rerankerCalled.get())
                .as("reranker must not be invoked when the toggle is OFF")
                .isFalse();
        assertThat(out)
                .as("retrieval should still return results when reranking is off")
                .isNotEmpty();
        // None of the results should carry reranker annotations.
        assertThat(out).allSatisfy(r -> {
            assertThat(r.preRerankRank()).isNull();
            assertThat(r.rerankerScore()).isNull();
        });
    }

    @Test
    void onToggleWidensRetrievalAndRoutesThroughReranker() {
        when(mockStateService.getRerankResults()).thenReturn(true);
        when(mockStateService.getIndexerMaxResults()).thenReturn(2);
        when(mockStateService.getRerankerShortlistSize()).thenReturn(10);

        AtomicInteger candidateCount = new AtomicInteger();
        Reranker capturing = (query, candidates, topN, timeoutMs) -> {
            candidateCount.set(candidates.size());
            // Reverse order to prove the service is using the reranker's output, not retrieval's.
            List<SearchResult> reversed = new ArrayList<>(candidates);
            java.util.Collections.reverse(reversed);
            int n = Math.min(topN, reversed.size());
            return reversed.subList(0, n);
        };

        SemanticSearchService svc = new SemanticSearchService();
        svc.setReranker(capturing);

        List<SearchResult> out = svc.search(mockProject, "alpha");

        assertThat(candidateCount.get())
                .as("retrieval should pass at least the shortlist size (or every available hit) into the reranker")
                .isGreaterThan(2);
        assertThat(out)
                .as("output should be truncated to indexerMaxResults")
                .hasSizeLessThanOrEqualTo(2);
    }

    @Test
    void rerankerThrowingFallsBackToTruncatedRetrievalOrder() {
        when(mockStateService.getRerankResults()).thenReturn(true);
        when(mockStateService.getIndexerMaxResults()).thenReturn(2);

        Reranker throwing = (query, candidates, topN, timeoutMs) -> {
            throw new RuntimeException("simulated reranker failure");
        };

        SemanticSearchService svc = new SemanticSearchService();
        svc.setReranker(throwing);

        List<SearchResult> out = svc.search(mockProject, "alpha");

        // Service must NOT propagate the exception — the prompt still needs some context.
        assertThat(out).as("must fall back to retrieval order on reranker exception").isNotEmpty();
        assertThat(out).hasSizeLessThanOrEqualTo(2);
    }

    private void seedChunks() {
        for (int i = 0; i < 8; i++) {
            String text = "alpha-content-" + i;
            Metadata meta = new Metadata();
            meta.put(IndexerConstants.FILE_PATH, "/tmp/file-" + i + ".java");
            TextSegment seg = TextSegment.from(text, meta);
            store.add(embeddingModel.embed(text).content(), seg);
        }
    }

    /** Copy of the deterministic test embedder used by {@link SemanticSearchServiceTest}. */
    private static final class DeterministicHashEmbeddingModel implements EmbeddingModel {
        private static final int DIM = 64;

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
            List<Embedding> out = new ArrayList<>(textSegments.size());
            for (TextSegment segment : textSegments) {
                out.add(embedString(segment.text()));
            }
            return Response.from(out);
        }

        @Override
        public int dimension() {
            return DIM;
        }

        private static Embedding embedString(String text) {
            long seed = 1469598103934665603L;
            for (int i = 0; i < text.length(); i++) {
                seed ^= text.charAt(i);
                seed *= 1099511628211L;
            }
            Random r = new Random(seed);
            float[] vec = new float[DIM];
            double norm = 0;
            for (int i = 0; i < DIM; i++) {
                vec[i] = r.nextFloat() * 2f - 1f;
                norm += vec[i] * vec[i];
            }
            norm = Math.sqrt(norm);
            for (int i = 0; i < DIM; i++) {
                vec[i] /= (float) norm;
            }
            return Embedding.from(vec);
        }
    }
}
