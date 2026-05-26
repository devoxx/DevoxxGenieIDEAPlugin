package com.devoxx.genie.service.rag;

import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end reproducer for the indexing→retrieval pipeline.
 *
 * <p>Before the v2 fix, {@code ProjectIndexerService.markFileAsIndexed} embedded the absolute
 * <em>file path</em> string rather than the chunk content, so a query for content that
 * appeared inside a file could never match — the vector store only held path embeddings.
 * The first test in this class is the canary for that bug: it indexes a temp file whose
 * content contains a unique phrase, then asserts a query for that phrase returns the chunk.
 * It fails on the pre-v2 code and passes on the fixed code.
 */
class SemanticSearchServiceTest {

    private MockedStatic<ApplicationManager> applicationManagerStatic;
    private MockedStatic<ChromaEmbeddingService> chromaServiceStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;

    private ChromaEmbeddingService mockChromaService;
    private DevoxxGenieStateService mockStateService;
    private Project mockProject;
    private Application mockApplication;

    private EmbeddingStore<TextSegment> store;
    private EmbeddingModel embeddingModel;

    @BeforeEach
    void setUp() {
        applicationManagerStatic = Mockito.mockStatic(ApplicationManager.class);
        chromaServiceStatic = Mockito.mockStatic(ChromaEmbeddingService.class);
        stateServiceStatic = Mockito.mockStatic(DevoxxGenieStateService.class);

        mockApplication = mock(Application.class);
        mockProject = mock(Project.class);
        mockChromaService = mock(ChromaEmbeddingService.class);
        mockStateService = mock(DevoxxGenieStateService.class);

        applicationManagerStatic.when(ApplicationManager::getApplication).thenReturn(mockApplication);

        // Both services use static getInstance(); route them to our mocks.
        chromaServiceStatic.when(ChromaEmbeddingService::getInstance).thenReturn(mockChromaService);
        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);

        // SemanticSearchService.getInstance() goes through ApplicationManager — let it build
        // a real instance backed by the mocked dependencies above.
        lenient().when(mockApplication.getService(SemanticSearchService.class))
                .thenAnswer(inv -> new SemanticSearchService());

        // Fresh in-memory store and embedding model per test.
        store = new InMemoryEmbeddingStore<>();
        embeddingModel = new DeterministicHashEmbeddingModel();

        when(mockChromaService.getEmbeddingStore()).thenReturn(store);
        when(mockChromaService.getEmbeddingModel()).thenReturn(embeddingModel);
        // init(Project) is a no-op for the in-memory store.
        Mockito.doNothing().when(mockChromaService).init(any());

        // Default search knobs: same as production defaults so the test exercises realistic config.
        when(mockStateService.getIndexerMinScore()).thenReturn(0.7);
        when(mockStateService.getIndexerMaxResults()).thenReturn(10);
    }

    @AfterEach
    void tearDown() {
        applicationManagerStatic.close();
        chromaServiceStatic.close();
        stateServiceStatic.close();
    }

    @Test
    void indexedChunkContentIsRetrievableByContentQuery(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("Auth.java");
        String uniquePhrase = "the secret needle xyzzy123 plugh-frobozz marker";
        Files.writeString(file, "package x;\nclass Auth { /* " + uniquePhrase + " */ void m() {} }\n");

        ProjectIndexerService indexer = new ProjectIndexerService();
        indexer.indexFile(file);

        // Use the exact stored chunk text as the query: under the deterministic hash model,
        // identical text → identical vector → cosine similarity 1.0, well above the 0.7 threshold.
        List<SearchResult> hits = SemanticSearchService.getInstance().search(mockProject,
                "package x;\nclass Auth { /* " + uniquePhrase + " */ void m() {} }");

        assertThat(hits)
                .as("search() must return the chunk; pre-v2 code embedded the file PATH and so could never match content")
                .isNotEmpty();
        assertThat(hits.get(0).content())
                .as("returned chunk should carry the original segment text, not the file path")
                .contains(uniquePhrase);
        assertThat(hits.get(0).filePath()).isEqualTo(file.toAbsolutePath().toString());
        assertThat(hits.get(0).score()).isGreaterThanOrEqualTo(0.99);
    }

    @Test
    void searchReturnsAllMatchingChunksNotJustOnePerFile() throws IOException {
        // Store two segments from the same file. Pre-v2 code returned a Map<filePath, ...>
        // which silently dropped duplicates.
        String absPath = "/fake/Same.java";
        TextSegment seg1 = TextSegment.from("alpha-content-1", metadata(absPath));
        TextSegment seg2 = TextSegment.from("beta-content-2", metadata(absPath));
        store.add(embeddingModel.embed(seg1.text()).content(), seg1);
        store.add(embeddingModel.embed(seg2.text()).content(), seg2);

        // Lower min score so we get both hits even when only one is an exact match.
        when(mockStateService.getIndexerMinScore()).thenReturn(0.0);

        List<SearchResult> hits = SemanticSearchService.getInstance().search(mockProject, "alpha-content-1");

        assertThat(hits)
                .as("both chunks from the same file should be present; old Map-keyed-by-path silently collapsed them")
                .hasSize(2);
        assertThat(hits).extracting(SearchResult::content)
                .containsExactlyInAnyOrder("alpha-content-1", "beta-content-2");
    }

    @Test
    void reindexingUnchangedFileIsANoOp(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("Same.java");
        Files.writeString(file, "class Same { /* unique-marker-9876 */ }\n");

        ProjectIndexerService indexer = new ProjectIndexerService();
        indexer.indexFile(file);
        int afterFirst = storedEntryCount();
        assertThat(afterFirst).as("first index pass should produce at least one segment").isGreaterThan(0);

        indexer.indexFile(file);
        int afterSecond = storedEntryCount();

        assertThat(afterSecond)
                .as("re-indexing an unchanged file must not duplicate segments — isFileIndexed should detect the existing entries via metadata filter")
                .isEqualTo(afterFirst);
    }

    @Test
    void reindexingPicksUpFilesWithUpdatedMtime(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("Edited.java");
        Files.writeString(file, "class Edited { /* v1 */ }\n");

        ProjectIndexerService indexer = new ProjectIndexerService();
        indexer.indexFile(file);
        int afterFirst = storedEntryCount();

        // Rewrite with newer mtime; the embedded content changes too.
        Files.writeString(file, "class Edited { /* v2 with brand new content */ }\n");
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 5_000));

        indexer.indexFile(file);
        int afterSecond = storedEntryCount();

        assertThat(afterSecond)
                .as("a file whose mtime advanced must be re-embedded (added to the index again)")
                .isGreaterThan(afterFirst);
    }

    /** Probe the in-memory store via a no-op search; embedAll on the InMemoryEmbeddingStore exposes no count. */
    private int storedEntryCount() {
        Embedding probe = embeddingModel.embed("__probe__").content();
        return store.search(dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                        .queryEmbedding(probe)
                        .maxResults(Integer.MAX_VALUE)
                        .minScore(0.0)
                        .build())
                .matches().size();
    }

    private static dev.langchain4j.data.document.Metadata metadata(String absolutePath) {
        dev.langchain4j.data.document.Metadata m = new dev.langchain4j.data.document.Metadata();
        m.put(IndexerConstants.FILE_PATH, absolutePath);
        m.put(IndexerConstants.LAST_MODIFIED, 0L);
        m.put(IndexerConstants.INDEXED_AT, 0L);
        m.put(IndexerConstants.EMBEDDING_SCHEMA_VERSION_KEY, IndexerConstants.CURRENT_EMBEDDING_SCHEMA_VERSION);
        return m;
    }

    /**
     * Deterministic, hash-derived embedding model: same text → identical unit vector,
     * different text → uncorrelated unit vectors. Lets us write content-vs-content
     * assertions without depending on a real embedder (no Ollama, no network).
     */
    private static final class DeterministicHashEmbeddingModel implements EmbeddingModel {

        private static final int DIM = 128;

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
            // Seed with a stable 64-bit derivation of the string so the JVM's hashCode
            // perturbation policy doesn't make the test flaky across runs.
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
