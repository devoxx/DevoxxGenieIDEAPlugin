package com.devoxx.genie.service.rag;

import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the exact chunks the plugin's indexer produces for a fixed set of real-world files,
 * so that moving the chunking logic (task-269) provably does not change indexing behaviour.
 *
 * <p>The chunks are captured at the embedding boundary: a recording {@link EmbeddingModel}
 * sees every segment text the indexer sends to be embedded, in order. That's after splitter
 * selection and {@link ChunkQualityFilter}, i.e. exactly what ends up in the vector store.
 *
 * <p>The golden files under {@code src/test/resources/rag-chunking/golden/} were generated
 * from the indexer as it was before the chunking extraction. To regenerate them after an
 * <em>intentional</em> chunking change, run with {@code UPDATE_RAG_GOLDEN=true} and review the diff.
 */
class ChunkingCharacterizationTest {

    static final Path FIXTURES = Path.of("src/test/resources/rag-chunking/fixtures");
    static final Path GOLDEN = Path.of("src/test/resources/rag-chunking/golden");

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private MockedStatic<ApplicationManager> applicationManagerStatic;
    private MockedStatic<ChromaEmbeddingService> chromaServiceStatic;
    private RecordingEmbeddingModel recorder;

    @BeforeEach
    void setUp() {
        applicationManagerStatic = Mockito.mockStatic(ApplicationManager.class);
        chromaServiceStatic = Mockito.mockStatic(ChromaEmbeddingService.class);

        Application app = mock(Application.class);
        ChromaEmbeddingService chroma = mock(ChromaEmbeddingService.class);
        applicationManagerStatic.when(ApplicationManager::getApplication).thenReturn(app);
        chromaServiceStatic.when(ChromaEmbeddingService::getInstance).thenReturn(chroma);

        recorder = new RecordingEmbeddingModel();
        when(chroma.getEmbeddingStore()).thenReturn(new InMemoryEmbeddingStore<>());
        when(chroma.getEmbeddingModel()).thenReturn(recorder);
        Mockito.doNothing().when(chroma).init(any());
    }

    @AfterEach
    void tearDown() {
        applicationManagerStatic.close();
        chromaServiceStatic.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SampleIndexer.java",
            "SampleBubble.kt",
            "sample-agent-mode.md",
            "sample-workflow.yml",
            "sample.properties"
    })
    void indexerProducesTheGoldenChunks(String fixture) throws IOException {
        Path file = FIXTURES.resolve(fixture);

        new ProjectIndexerService().indexFile(file);
        List<String> actual = recorder.texts();

        Path golden = GOLDEN.resolve(fixture + ".json");
        if (Boolean.parseBoolean(System.getenv("UPDATE_RAG_GOLDEN"))) {
            Files.createDirectories(GOLDEN);
            JSON.writeValue(golden.toFile(), actual);
        }

        assertThat(golden).as("golden file missing; run once with UPDATE_RAG_GOLDEN=true").exists();
        List<String> expected = JSON.readValue(golden.toFile(), new TypeReference<>() {});
        assertThat(actual).as("chunks for " + fixture).isNotEmpty().containsExactlyElementsOf(expected);
    }

    /** Records every text sent for embedding; returns a constant vector. */
    static final class RecordingEmbeddingModel implements EmbeddingModel {
        private final List<String> texts = Collections.synchronizedList(new ArrayList<>());

        List<String> texts() {
            return List.copyOf(texts);
        }

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            List<Embedding> out = new ArrayList<>(segments.size());
            for (TextSegment s : segments) {
                texts.add(s.text());
                out.add(Embedding.from(new float[]{1f, 0f, 0f}));
            }
            return Response.from(out);
        }
    }
}
