package com.devoxx.genie.service.rag.benchmark;

import com.devoxx.genie.service.rag.chunking.RagChunker;
import com.devoxx.genie.service.rag.embedding.EmbeddingProfile;
import com.devoxx.genie.service.rag.embedding.ProfiledEmbedder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class RagBenchmarkTest {

    /**
     * Bag-of-words hashing embedder: texts sharing words get similar vectors, so a question
     * mentioning "kangaroo" retrieves the file about kangaroos. Records what it was sent.
     */
    static final class HashingModel implements EmbeddingModel {
        final List<String> seen = Collections.synchronizedList(new ArrayList<>());

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            List<Embedding> out = new ArrayList<>();
            for (TextSegment s : segments) {
                seen.add(s.text());
                float[] v = new float[256];
                for (String word : s.text().toLowerCase(Locale.ROOT).split("[^a-z]+")) {
                    if (word.length() < 4) continue; // ignore short words and prefixes like "q:"
                    v[Math.floorMod(word.hashCode(), v.length)] += 1f;
                }
                v[0] += 0.01f; // never all-zero
                out.add(Embedding.from(v));
            }
            return Response.from(out);
        }
    }

    private static Path repo(Path tmp) throws IOException {
        Files.createDirectories(tmp.resolve("src"));
        Files.createDirectories(tmp.resolve("docs"));
        Files.writeString(tmp.resolve("src/Kangaroo.java"),
                "class Kangaroo { void jump() { /* kangaroo jumping across the outback desert */ } }\n");
        Files.writeString(tmp.resolve("src/Penguin.java"),
                "class Penguin { void swim() { /* penguin swimming through antarctic water */ } }\n");
        Files.writeString(tmp.resolve("docs/volcano.md"),
                "# Volcano\n\nThe volcano erupts lava and ash over the mountain village every century.\n");
        return tmp;
    }

    private static BenchmarkDataset dataset() {
        return new BenchmarkDataset("t", 1, "", List.of(), List.of(
                new BenchmarkDataset.Question("kangaroo", "which animal is jumping across the outback desert?",
                        List.of("src/Kangaroo.java")),
                new BenchmarkDataset.Question("volcano", "where does lava and ash erupt over the village?",
                        List.of("docs/volcano.md")),
                new BenchmarkDataset.Question("missing", "zebra stripes savanna grassland",
                        List.of("src/Zebra.java"))));
    }

    private static final List<String> CORPUS = List.of("src/Kangaroo.java", "src/Penguin.java", "docs/volcano.md");

    @Test
    void ranksTheExpectedFileFirstAndScoresTheRun(@TempDir Path tmp) throws IOException {
        Path root = repo(tmp);
        RagBenchmark benchmark = new RagBenchmark(
                new ProfiledEmbedder(new HashingModel(), EmbeddingProfile.NONE), new RagChunker());

        RagBenchmark.Result result = benchmark.run(root, CORPUS, dataset(), 50);

        assertThat(result.corpusFiles()).isEqualTo(3);
        assertThat(result.chunks()).isGreaterThanOrEqualTo(3);
        assertThat(result.questions()).extracting(RagBenchmark.QuestionResult::id, RagBenchmark.QuestionResult::rank)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("kangaroo", 1),
                        org.assertj.core.groups.Tuple.tuple("volcano", 1),
                        org.assertj.core.groups.Tuple.tuple("missing", 0));
        assertThat(result.summary().questions()).isEqualTo(3);
        assertThat(result.summary().recallAt1()).isCloseTo(2.0 / 3, org.assertj.core.api.Assertions.within(1e-9));
    }

    @Test
    void retrievedFilesAreRepoRelativeAndDeduplicated(@TempDir Path tmp) throws IOException {
        Path root = repo(tmp);
        RagBenchmark benchmark = new RagBenchmark(
                new ProfiledEmbedder(new HashingModel(), EmbeddingProfile.NONE), new RagChunker());

        RagBenchmark.Result result = benchmark.run(root, CORPUS, dataset(), 50);

        List<String> top = result.questions().get(0).topFiles();
        assertThat(top).doesNotHaveDuplicates();
        assertThat(top).allMatch(CORPUS::contains);
    }

    @Test
    void documentAndQueryPrefixesReachTheModel(@TempDir Path tmp) throws IOException {
        Path root = repo(tmp);
        HashingModel model = new HashingModel();
        RagBenchmark benchmark = new RagBenchmark(
                new ProfiledEmbedder(model, new EmbeddingProfile("q: ", "d: ", null)), new RagChunker());

        benchmark.run(root, CORPUS, dataset(), 50);

        assertThat(model.seen).anyMatch(t -> t.startsWith("d: class Kangaroo"));
        assertThat(model.seen).contains("q: which animal is jumping across the outback desert?");
    }

    @Test
    void unreadableOrMissingFilesAreSkippedNotFatal(@TempDir Path tmp) throws IOException {
        Path root = repo(tmp);
        Files.write(root.resolve("src/Binary.java"), new byte[]{(byte) 0xC3, (byte) 0x28, 0, 1, 2});
        List<String> corpus = new ArrayList<>(CORPUS);
        corpus.add("src/Binary.java");
        corpus.add("src/DeletedSinceGitLsFiles.java");

        RagBenchmark.Result result = new RagBenchmark(
                new ProfiledEmbedder(new HashingModel(), EmbeddingProfile.NONE), new RagChunker())
                .run(root, corpus, dataset(), 50);

        assertThat(result.corpusFiles()).isEqualTo(5);
        assertThat(result.skippedFiles()).isEqualTo(2);
        assertThat(result.questions().get(0).rank()).isEqualTo(1);
    }

    @Test
    void resultSerialisesToJsonWithConfigAndMetrics(@TempDir Path tmp) throws IOException {
        Path root = repo(tmp);
        RagBenchmark.Result result = new RagBenchmark(
                new ProfiledEmbedder(new HashingModel(), new EmbeddingProfile("q: ", "d: ", null)), new RagChunker())
                .run(root, CORPUS, dataset(), 50);

        JsonNode json = new ObjectMapper().readTree(RagBenchmark.toJson(result));

        assertThat(json.at("/profile/queryPrefix").asText()).isEqualTo("q: ");
        assertThat(json.at("/profile/documentPrefix").asText()).isEqualTo("d: ");
        assertThat(json.at("/summary/recallAt5").isNumber()).isTrue();
        assertThat(json.at("/summary/mrr").isNumber()).isTrue();
        assertThat(json.at("/questions").size()).isEqualTo(3);
        assertThat(json.at("/questions/0/rank").asInt()).isEqualTo(1);
    }
}
