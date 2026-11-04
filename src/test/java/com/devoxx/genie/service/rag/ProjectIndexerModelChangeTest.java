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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TASK-212: an index is bound to the embedding setup that built it. A full re-index with a
 * different model starts from an empty index; until then, everything that touches the
 * existing index keeps using the model it was built with.
 */
class ProjectIndexerModelChangeTest {

    private static final EmbeddingFingerprint NOMIC = EmbeddingFingerprint.DEFAULT;
    private static final EmbeddingFingerprint BGE = EmbeddingFingerprint.ollama("bge-m3");

    private MockedStatic<ApplicationManager> applicationManagerStatic;
    private MockedStatic<ChromaEmbeddingService> chromaServiceStatic;
    private ChromaEmbeddingService chroma;
    private InMemoryEmbeddingStore<TextSegment> store;
    private InMemoryIndexManifest manifest;
    private ProjectIndexerService indexer;

    @BeforeEach
    void setUp() {
        applicationManagerStatic = Mockito.mockStatic(ApplicationManager.class);
        chromaServiceStatic = Mockito.mockStatic(ChromaEmbeddingService.class);
        applicationManagerStatic.when(ApplicationManager::getApplication).thenReturn(mock(Application.class));
        chroma = mock(ChromaEmbeddingService.class);
        chromaServiceStatic.when(ChromaEmbeddingService::getInstance).thenReturn(chroma);

        store = new InMemoryEmbeddingStore<>();
        when(chroma.getEmbeddingStore()).thenReturn(store);
        when(chroma.getEmbeddingModel(any())).thenReturn(new FixedEmbeddingModel());

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
    void sameModelKeepsTheExistingIndex(@TempDir Path tmp) throws IOException {
        Path file = indexedWith(NOMIC, tmp.resolve("A.java"));
        int vectors = storedVectors();

        assertThat(indexer.prepareIndexFor(NOMIC)).isTrue();

        assertThat(storedVectors()).isEqualTo(vectors).isPositive();
        assertThat(manifest.isCurrent(file, NOMIC.id())).isTrue();
    }

    @Test
    void differentModelStartsFromAnEmptyIndex(@TempDir Path tmp) throws IOException {
        Path file = indexedWith(NOMIC, tmp.resolve("A.java"));
        assertThat(storedVectors()).isPositive();

        assertThat(indexer.prepareIndexFor(BGE)).isTrue();

        assertThat(storedVectors()).as("nomic vectors must not share an index with bge-m3 ones").isZero();
        assertThat(manifest.trackedPaths()).isEmpty();
        assertThat(manifest.isCurrent(file, BGE.id())).isFalse();
    }

    @Test
    void neverIndexedProjectIsLeftAlone() {
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> mockStore = mock(EmbeddingStore.class);
        when(chroma.getEmbeddingStore()).thenReturn(mockStore);

        assertThat(indexer.prepareIndexFor(BGE)).isTrue();

        verify(mockStore, never()).removeAll();
    }

    @Test
    void indexIsKeptWhenTheOldVectorsCannotBeDropped(@TempDir Path tmp) throws IOException {
        Path file = indexedWith(NOMIC, tmp.resolve("A.java"));
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> failing = mock(EmbeddingStore.class);
        doThrow(new RuntimeException("ChromaDB is down")).when(failing).removeAll();
        when(chroma.getEmbeddingStore()).thenReturn(failing);

        assertThat(indexer.prepareIndexFor(BGE))
                .as("indexing must not proceed: it would add bge-m3 vectors to a nomic index")
                .isFalse();
        assertThat(manifest.isCurrent(file, NOMIC.id())).as("manifest still matches the stored vectors").isTrue();
    }

    @Test
    void changedFilesKeepUsingTheIndexModelUntilTheUserReindexes(@TempDir Path tmp) throws IOException {
        indexedWith(NOMIC, tmp.resolve("A.java"));
        when(chroma.configuredFingerprint()).thenReturn(BGE); // user switched settings to bge-m3

        Path edited = tmp.resolve("B.java");
        Files.writeString(edited, "package x;\nclass B { void saved() { System.out.println(\"edited after the switch\"); } }\n");
        indexer.indexFile(edited);

        verify(chroma, never()).getEmbeddingModel(BGE);
        assertThat(manifest.isCurrent(edited, NOMIC.id())).isTrue();
        assertThat(manifest.indexFingerprint()).contains(NOMIC.id());
    }

    @Test
    void afterTheRebuildFilesAreEmbeddedWithTheConfiguredModel(@TempDir Path tmp) throws IOException {
        Path file = indexedWith(NOMIC, tmp.resolve("A.java"));
        when(chroma.configuredFingerprint()).thenReturn(BGE);

        indexer.prepareIndexFor(BGE);
        indexer.indexFile(file);

        verify(chroma).getEmbeddingModel(BGE);
        assertThat(manifest.isCurrent(file, BGE.id())).isTrue();
        assertThat(manifest.indexFingerprint()).contains(BGE.id());
    }

    private Path indexedWith(EmbeddingFingerprint fingerprint, Path file) throws IOException {
        Files.writeString(file, "package x;\nclass A { void m() { System.out.println(\"hello\"); } }\n");
        when(chroma.configuredFingerprint()).thenReturn(fingerprint);
        indexer.indexFile(file);
        assertThat(manifest.isCurrent(file, fingerprint.id())).isTrue();
        return file;
    }

    private int storedVectors() {
        return store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(new FixedEmbeddingModel().embed("q").content())
                .maxResults(1000)
                .minScore(0.0)
                .build()).matches().size();
    }

    /** Same vector for every text; these tests are about which model is used, not similarity. */
    private static final class FixedEmbeddingModel implements EmbeddingModel {
        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            return Response.from(segments.stream().map(s -> Embedding.from(new float[]{1f, 0f, 0f})).toList());
        }
    }
}
