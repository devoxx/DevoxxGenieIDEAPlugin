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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
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

    private static final EmbeddingFingerprint NOMIC = EmbeddingFingerprint.LEGACY;
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
    void modelThatCannotEmbedLeavesTheExistingIndexUntouched(@TempDir Path tmp) throws IOException {
        // e.g. not pulled, or a chat model such as gemma4 that Ollama refuses to embed with.
        Path file = indexedWith(NOMIC, tmp.resolve("A.java"));
        int vectors = storedVectors();
        EmbeddingModel broken = mock(EmbeddingModel.class);
        when(broken.embed(Mockito.anyString())).thenThrow(new RuntimeException("model 'bge-m3' not found"));
        when(chroma.getEmbeddingModel(BGE)).thenReturn(broken);

        assertThatThrownBy(() -> indexer.prepareIndexFor(BGE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bge-m3")
                .hasMessageContaining("not changed");

        assertThat(storedVectors()).as("the working index must survive").isEqualTo(vectors).isPositive();
        assertThat(manifest.isCurrent(file, NOMIC.id())).isTrue();
    }

    @Test
    void dimensionLargerThanTheModelProvidesLeavesTheIndexUntouched(@TempDir Path tmp) throws IOException {
        Path file = indexedWith(NOMIC, tmp.resolve("A.java"));
        int vectors = storedVectors();
        EmbeddingFingerprint tooLarge = new EmbeddingFingerprint("ollama", "bge-m3", 9999, "");

        assertThatThrownBy(() -> indexer.prepareIndexFor(tooLarge))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("larger than the model's native dimension");

        assertThat(storedVectors()).isEqualTo(vectors).isPositive();
        assertThat(manifest.isCurrent(file, NOMIC.id())).isTrue();
    }

    @Test
    void documentPrefixGoesToTheModelButNeverIntoTheStoredText(@TempDir Path tmp) throws IOException {
        EmbeddingFingerprint prefixed = new EmbeddingFingerprint("ollama", "nomic-embed-text", null, "search_document: ");
        RecordingEmbeddingModel model = new RecordingEmbeddingModel();
        when(chroma.getEmbeddingModel(prefixed)).thenReturn(model);

        indexedWith(prefixed, tmp.resolve("A.java"));

        assertThat(model.received).isNotEmpty().allMatch(text -> text.startsWith("search_document: "));
        assertThat(storedTexts()).isNotEmpty().noneMatch(text -> text.contains("search_document"));
    }

    @Test
    void perSegmentFallbackAppliesTheDocumentPrefixToo(@TempDir Path tmp) throws IOException {
        EmbeddingFingerprint prefixed = new EmbeddingFingerprint("ollama", "nomic-embed-text", null, "search_document: ");
        RecordingEmbeddingModel model = new RecordingEmbeddingModel();
        model.failBatches = true;
        when(chroma.getEmbeddingModel(prefixed)).thenReturn(model);

        indexedWith(prefixed, tmp.resolve("A.java"));

        assertThat(model.received).isNotEmpty().allMatch(text -> text.startsWith("search_document: "));
        assertThat(storedVectors()).isPositive();
        assertThat(storedTexts()).noneMatch(text -> text.contains("search_document"));
    }

    @Test
    void vectorsAreStoredAtTheConfiguredDimensionWithUnitLength(@TempDir Path tmp) throws IOException {
        EmbeddingFingerprint truncated = new EmbeddingFingerprint("ollama", "nomic-embed-text", 2, "");

        indexedWith(truncated, tmp.resolve("A.java"));

        float[] vector = store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[]{1f, 0f})).maxResults(1).build())
                .matches().get(0).embedding().vector();
        assertThat(vector).hasSize(2);
        assertThat(Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1])).isCloseTo(1.0, within(1e-5));
    }

    @Test
    void chunksThatFailToEmbedAreNotCountedAsIndexed(@TempDir Path tmp) throws IOException {
        // The failed embeddinggemma run: the manifest claimed 18,626 segments with none stored.
        EmbeddingModel broken = mock(EmbeddingModel.class);
        when(broken.embedAll(Mockito.anyList())).thenThrow(new RuntimeException("model not found"));
        when(broken.embed(Mockito.anyString())).thenThrow(new RuntimeException("model not found"));
        when(chroma.getEmbeddingModel(BGE)).thenReturn(broken);
        when(chroma.configuredFingerprint()).thenReturn(BGE);
        Path file = tmp.resolve("A.java");
        Files.writeString(file, "package x;\nclass A { void m() { System.out.println(\"hello\"); } }\n");

        indexer.indexFile(file);

        assertThat(manifest.isTracked(file)).isFalse();
        assertThat(manifest.indexFingerprint()).isEmpty();
    }

    @Test
    void queryPrefixIsRecordedSoAStaleIndexCanStillBeSearched(@TempDir Path tmp) throws IOException {
        when(chroma.configuredQueryPrefix()).thenReturn("search_query: ");

        indexedWith(NOMIC, tmp.resolve("A.java"));

        assertThat(manifest.indexQueryPrefix()).isEqualTo("search_query: ");
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
        Mockito.clearInvocations(chroma); // prepareIndexFor test-embeds; only count the file's embedding
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

    private List<String> storedTexts() {
        return store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(new FixedEmbeddingModel().embed("q").content())
                .maxResults(1000)
                .minScore(0.0)
                .build()).matches().stream().map(match -> match.embedded().text()).toList();
    }

    /** Records every text the model receives; with {@code failBatches}, only single texts succeed. */
    private static final class RecordingEmbeddingModel implements EmbeddingModel {
        private final List<String> received = new java.util.ArrayList<>();
        private boolean failBatches;

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            if (failBatches && !isTestEmbed(segments)) {
                failBatches = false; // The fallback's single-text calls then succeed.
                throw new RuntimeException("batch rejected");
            }
            segments.forEach(segment -> received.add(segment.text()));
            return Response.from(segments.stream().map(s -> Embedding.from(new float[]{1f, 0f, 0f})).toList());
        }

        private static boolean isTestEmbed(List<TextSegment> segments) {
            return segments.get(0).text().contains("embedding model check");
        }
    }

    /** Same vector for every text; these tests are about which model is used, not similarity. */
    private static final class FixedEmbeddingModel implements EmbeddingModel {
        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            return Response.from(segments.stream().map(s -> Embedding.from(new float[]{1f, 0f, 0f})).toList());
        }
    }
}
