package com.devoxx.genie.service.rag;

import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import com.devoxx.genie.service.rag.manifest.InMemoryIndexManifest;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TASK-282: "Start indexing" must be incremental, and re-indexing a file must replace its
 * chunks, never add a second copy. Before the fix every full run re-embedded all files and
 * stored another copy of each chunk (a real project had 56% duplicate chunks after three runs).
 */
class ProjectIndexerIncrementalTest {

    private static final EmbeddingFingerprint FP = EmbeddingFingerprint.LEGACY;

    private MockedStatic<ApplicationManager> applicationManagerStatic;
    private MockedStatic<ChromaEmbeddingService> chromaServiceStatic;
    private InMemoryEmbeddingStore<TextSegment> store;
    private InMemoryIndexManifest manifest;
    private ProjectIndexerService indexer;
    private CountingEmbeddingModel model;

    @BeforeEach
    void setUp() {
        applicationManagerStatic = Mockito.mockStatic(ApplicationManager.class);
        chromaServiceStatic = Mockito.mockStatic(ChromaEmbeddingService.class);
        applicationManagerStatic.when(ApplicationManager::getApplication).thenReturn(mock(Application.class));
        ChromaEmbeddingService chroma = mock(ChromaEmbeddingService.class);
        chromaServiceStatic.when(ChromaEmbeddingService::getInstance).thenReturn(chroma);

        store = new InMemoryEmbeddingStore<>();
        model = new CountingEmbeddingModel();
        when(chroma.getEmbeddingStore()).thenReturn(store);
        when(chroma.getEmbeddingModel(any())).thenReturn(model);
        when(chroma.configuredFingerprint()).thenReturn(FP);

        manifest = new InMemoryIndexManifest();
        indexer = new ProjectIndexerService();
        indexer.setManifest(manifest);
    }

    @AfterEach
    void tearDown() {
        applicationManagerStatic.close();
        chromaServiceStatic.close();
    }

    @Test
    void secondIncrementalRun_reEmbedsNothing(@TempDir Path tmp) throws IOException {
        Path file = write(tmp.resolve("A.java"), "class A { void a() { System.out.println(\"a\"); } }");
        assertThat(indexer.indexFile(file, false, FP)).isEqualTo(ProjectIndexerService.FileOutcome.INDEXED);
        int vectors = storedVectors();
        int embedded = model.embedded.get();

        assertThat(indexer.indexFile(file, false, FP)).isEqualTo(ProjectIndexerService.FileOutcome.UNCHANGED);

        assertThat(model.embedded.get()).as("an unchanged file must not be embedded again").isEqualTo(embedded);
        assertThat(storedVectors()).isEqualTo(vectors);
    }

    @Test
    void forcedReindex_replacesTheFilesChunks_insteadOfAddingCopies(@TempDir Path tmp) throws IOException {
        Path file = write(tmp.resolve("A.java"), "class A { void a() { System.out.println(\"a\"); } }");
        indexer.indexFile(file, false, FP);
        int vectors = storedVectors();

        indexer.indexFile(file, true, FP);
        indexer.indexFile(file, true, FP);

        assertThat(storedVectors()).as("re-indexing must not duplicate chunks").isEqualTo(vectors);
    }

    @Test
    void changedFile_isReEmbedded_andItsOldChunksAreGone(@TempDir Path tmp) throws IOException {
        Path file = write(tmp.resolve("A.java"), "class A { void before() { System.out.println(\"old\"); } }");
        indexer.indexFile(file, false, FP);

        write(file, "class A { void after() { System.out.println(\"new\"); } }");
        assertThat(indexer.indexFile(file, false, FP)).isEqualTo(ProjectIndexerService.FileOutcome.INDEXED);

        assertThat(storedTexts()).noneMatch(t -> t.contains("before")).anyMatch(t -> t.contains("after"));
    }

    @Test
    void embeddingFailure_keepsTheFilesExistingChunks_andItStaysTracked(@TempDir Path tmp) throws IOException {
        // e.g. Ollama stopped: replacing chunks must not delete the old ones before the new ones exist,
        // or every edited file would silently drop out of the index (the watcher ignores untracked files).
        Path file = write(tmp.resolve("A.java"), "class A { void before() { System.out.println(\"old\"); } }");
        indexer.indexFile(file, false, FP);
        int vectors = storedVectors();

        write(file, "class A { void after() { System.out.println(\"new\"); } }");
        model.failing = true;
        assertThat(indexer.indexFile(file, false, FP)).isEqualTo(ProjectIndexerService.FileOutcome.FAILED);

        assertThat(storedVectors()).isEqualTo(vectors);
        assertThat(storedTexts()).anyMatch(t -> t.contains("before"));
        assertThat(manifest.isTracked(file)).as("the watcher must still re-index it later").isTrue();
    }

    @Test
    void fileThatBecameBlank_losesItsChunks_andIsNoLongerTracked(@TempDir Path tmp) throws IOException {
        Path file = write(tmp.resolve("A.java"), "class A { void a() { System.out.println(\"a\"); } }");
        indexer.indexFile(file, false, FP);

        Files.writeString(file, "   \n");
        assertThat(indexer.indexFile(file, false, FP)).isEqualTo(ProjectIndexerService.FileOutcome.SKIPPED);

        assertThat(storedVectors()).isZero();
        assertThat(manifest.isTracked(file)).isFalse();
    }

    @Test
    void filesNoLongerInTheIndexedSet_arePruned(@TempDir Path tmp) throws IOException {
        Path kept = write(tmp.resolve("Kept.java"), "class Kept { void k() { System.out.println(\"kept\"); } }");
        Path gone = write(tmp.resolve("Gone.java"), "class Gone { void g() { System.out.println(\"gone\"); } }");
        indexer.indexFile(kept, false, FP);
        indexer.indexFile(gone, false, FP);

        // Gone.java was deleted, or is now excluded: it is not part of this run's file set.
        int removed = indexer.pruneFilesNotIn(List.of(kept));

        assertThat(removed).isEqualTo(1);
        assertThat(manifest.isTracked(gone)).isFalse();
        assertThat(manifest.isTracked(kept)).isTrue();
        assertThat(storedTexts()).noneMatch(t -> t.contains("Gone")).anyMatch(t -> t.contains("Kept"));
    }

    @Test
    void pruning_isSkipped_whenTheScanFoundNothing(@TempDir Path tmp) throws IOException {
        // An empty scan more likely means the scan failed than that every file was deleted.
        Path file = write(tmp.resolve("A.java"), "class A { void a() { System.out.println(\"a\"); } }");
        indexer.indexFile(file, false, FP);

        assertThat(indexer.pruneFilesNotIn(List.of())).isZero();
        assertThat(manifest.isTracked(file)).isTrue();
    }

    @Test
    void clearIndex_dropsEveryChunkAndManifestEntry(@TempDir Path tmp) throws IOException {
        Path file = write(tmp.resolve("A.java"), "class A { void a() { System.out.println(\"a\"); } }");
        indexer.indexFile(file, false, FP);
        assertThat(storedVectors()).isPositive();

        assertThat(indexer.clearIndex()).isTrue();

        assertThat(storedVectors()).isZero();
        assertThat(manifest.trackedPaths()).isEmpty();
    }

    private static Path write(Path file, String content) throws IOException {
        Files.writeString(file, "package x;\n" + content + "\n");
        return file;
    }

    private int storedVectors() {
        return storedTexts().size();
    }

    private List<String> storedTexts() {
        return store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[]{1f, 0f, 0f}))
                .maxResults(1000)
                .minScore(0.0)
                .build()).matches().stream().map(match -> match.embedded().text()).toList();
    }

    /** Same vector for every text; counts how many texts were embedded. */
    private static final class CountingEmbeddingModel implements EmbeddingModel {
        final AtomicInteger embedded = new AtomicInteger();
        volatile boolean failing;

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            if (failing) {
                throw new RuntimeException("connection refused");
            }
            embedded.addAndGet(segments.size());
            return Response.from(segments.stream().map(s -> Embedding.from(new float[]{1f, 0f, 0f})).toList());
        }
    }
}
