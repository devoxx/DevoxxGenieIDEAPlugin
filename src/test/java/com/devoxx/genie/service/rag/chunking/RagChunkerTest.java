package com.devoxx.genie.service.rag.chunking;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RagChunker} must run without any IntelliJ services (so the standalone RAG benchmark
 * can use it) and must produce exactly the chunks the plugin's indexer produced before the
 * chunking logic was extracted — the same golden files {@code ChunkingCharacterizationTest}
 * checks against the indexer itself.
 */
class RagChunkerTest {

    private static final Path FIXTURES = Path.of("src/test/resources/rag-chunking/fixtures");
    private static final Path GOLDEN = Path.of("src/test/resources/rag-chunking/golden");

    private final RagChunker chunker = new RagChunker();

    @ParameterizedTest
    @ValueSource(strings = {
            "SampleIndexer.java",
            "SampleBubble.kt",
            "sample-agent-mode.md",
            "sample-workflow.yml",
            "sample.properties"
    })
    void producesTheGoldenChunksWithoutTheIde(String fixture) throws IOException {
        Path file = FIXTURES.resolve(fixture);

        List<String> actual = chunker.chunk(file, Files.readString(file)).stream()
                .map(TextSegment::text)
                .toList();

        List<String> expected = new ObjectMapper().readValue(
                GOLDEN.resolve(fixture + ".json").toFile(), new TypeReference<>() {});
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    void blankContentProducesNoChunks() {
        assertThat(chunker.chunk(Path.of("Empty.java"), "  \n\t\n")).isEmpty();
    }

    @Test
    void lowContentChunksAreDropped() {
        // A table skeleton has fewer distinct tokens than ChunkQualityFilter requires, so the
        // only chunk the splitter produces is dropped; one prose sentence is kept.
        assertThat(chunker.chunk(Path.of("table.md"), "| --- | --- |\n| a | b |\n")).isEmpty();
        assertThat(chunker.chunk(Path.of("notes.md"),
                "The Model Context Protocol lets the agent call external tools safely."))
                .hasSize(1);
    }

    @Test
    void splitterIsChosenByFileExtension() {
        // Source code gets a line-based splitter — statement boundaries beat the recursive
        // default's preference for sentence punctuation.
        assertThat(chunker.splitterFor(Path.of("Auth.java")))
                .isInstanceOf(DocumentByLineSplitter.class);
        assertThat(chunker.splitterFor(Path.of("README.md")))
                .isInstanceOf(DocumentByParagraphSplitter.class);

        // Unknown extensions return the cached default splitter — same instance every call.
        var unknown1 = chunker.splitterFor(Path.of("config.toml"));
        var unknown2 = chunker.splitterFor(Path.of("data.csv"));
        assertThat(unknown1).isSameAs(unknown2);

        // Code splitter is NOT the cached default.
        assertThat(chunker.splitterFor(Path.of("Foo.py"))).isNotSameAs(unknown1);
    }

    @Test
    void returnedSegmentsCarryNoMetadata() {
        // Metadata (file path, timestamps, schema version) is the indexer's job, not the chunker's.
        List<TextSegment> segments = chunker.chunk(Path.of("A.java"),
                "class A { void run() { System.out.println(\"hello world from the chunker\"); } }");
        assertThat(segments).isNotEmpty();
        assertThat(segments).allMatch(s -> s.metadata().toMap().isEmpty());
    }
}
