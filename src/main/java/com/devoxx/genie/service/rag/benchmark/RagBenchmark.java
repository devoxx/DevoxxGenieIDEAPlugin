package com.devoxx.genie.service.rag.benchmark;

import com.devoxx.genie.service.rag.chunking.RagChunker;
import com.devoxx.genie.service.rag.embedding.EmbeddingProfile;
import com.devoxx.genie.service.rag.embedding.ProfiledEmbedder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.devoxx.genie.service.rag.IndexerConstants.FILE_PATH;

/**
 * Measures retrieval quality for one embedding configuration on one repository.
 *
 * <p>Indexes the given corpus files with the plugin's own chunker ({@link RagChunker}) and the
 * configured {@link ProfiledEmbedder} into a throwaway {@link InMemoryEmbeddingStore}, then asks
 * every benchmark question and scores where the expected file lands ({@link RetrievalMetrics}).
 *
 * <p>Never connects to ChromaDB: the vectors only exist for the duration of {@link #run}.
 * Retrieval uses no minimum score, unlike the plugin's search, so a threshold can't hide
 * differences in ranking between configurations.
 */
public final class RagBenchmark {

    /** Same batch size as the plugin's indexer. */
    static final int EMBEDDING_BATCH_SIZE = 64;
    /** How many retrieved files are kept per question in the result, for inspection. */
    static final int TOP_FILES_REPORTED = 10;

    private final ProfiledEmbedder embedder;
    private final RagChunker chunker;
    private PrintStream progress;

    public RagBenchmark(@NotNull ProfiledEmbedder embedder, @NotNull RagChunker chunker) {
        this.embedder = embedder;
        this.chunker = chunker;
    }

    /** Optional progress output (e.g. {@code System.err}); silent when not set. */
    public RagBenchmark withProgress(PrintStream progress) {
        this.progress = progress;
        return this;
    }

    public record QuestionResult(String id, String question, List<String> expected, int rank, List<String> topFiles) {}

    public record Result(String dataset,
                         int datasetVersion,
                         EmbeddingProfile profile,
                         int topChunks,
                         int corpusFiles,
                         int skippedFiles,
                         int chunks,
                         int failedChunks,
                         long indexMillis,
                         long queryMillis,
                         RetrievalMetrics.Summary summary,
                         List<QuestionResult> questions) {}

    /**
     * @param repoRoot    directory the corpus paths are relative to
     * @param corpusFiles repo-relative files to index (see {@link BenchmarkCorpus})
     * @param topChunks   chunks retrieved per question before collapsing to files
     */
    public @NotNull Result run(@NotNull Path repoRoot,
                               @NotNull List<String> corpusFiles,
                               @NotNull BenchmarkDataset dataset,
                               int topChunks) {
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

        long indexStart = System.currentTimeMillis();
        int skipped = 0;
        int stored = 0;
        int failed = 0;
        List<TextSegment> pending = new ArrayList<>();
        for (int i = 0; i < corpusFiles.size(); i++) {
            String relative = corpusFiles.get(i);
            String content;
            try {
                content = Files.readString(repoRoot.resolve(relative));
            } catch (IOException e) {
                // Same as the plugin: unreadable (binary, non-UTF-8, deleted) files are skipped.
                skipped++;
                continue;
            }
            for (TextSegment chunk : chunker.chunk(Path.of(relative), content)) {
                pending.add(TextSegment.from(chunk.text(), Metadata.from(FILE_PATH, relative)));
            }
            while (pending.size() >= EMBEDDING_BATCH_SIZE) {
                List<TextSegment> batch = new ArrayList<>(pending.subList(0, EMBEDDING_BATCH_SIZE));
                pending.subList(0, EMBEDDING_BATCH_SIZE).clear();
                int ok = store(batch, store);
                stored += ok;
                failed += batch.size() - ok;
            }
            if (progress != null && (i + 1) % 100 == 0) {
                progress.printf("  indexed %d/%d files, %d chunks%n", i + 1, corpusFiles.size(), stored);
            }
        }
        if (!pending.isEmpty()) {
            int ok = store(pending, store);
            stored += ok;
            failed += pending.size() - ok;
        }
        long indexMillis = System.currentTimeMillis() - indexStart;

        long queryStart = System.currentTimeMillis();
        List<QuestionResult> results = new ArrayList<>();
        List<Integer> ranks = new ArrayList<>();
        for (BenchmarkDataset.Question q : dataset.questions()) {
            Embedding query = embedder.embedQuery(q.question());
            List<String> chunkFiles = store.search(EmbeddingSearchRequest.builder()
                            .queryEmbedding(query)
                            .maxResults(topChunks)
                            .minScore(0.0)
                            .build())
                    .matches().stream()
                    .map(EmbeddingMatch::embedded)
                    .map(s -> s.metadata().getString(FILE_PATH))
                    .toList();
            List<String> rankedFiles = RetrievalMetrics.rankFiles(chunkFiles);
            int rank = RetrievalMetrics.firstRelevantRank(rankedFiles, q.expected());
            ranks.add(rank);
            results.add(new QuestionResult(q.id(), q.question(), q.expected(), rank,
                    rankedFiles.subList(0, Math.min(TOP_FILES_REPORTED, rankedFiles.size()))));
        }
        long queryMillis = System.currentTimeMillis() - queryStart;

        return new Result(dataset.name(), dataset.version(), embedder.profile(), topChunks,
                corpusFiles.size(), skipped, stored, failed, indexMillis, queryMillis,
                RetrievalMetrics.summarize(ranks), results);
    }

    /** Embed and store one batch; falls back to one-by-one like the plugin. Returns chunks stored. */
    private int store(List<TextSegment> batch, InMemoryEmbeddingStore<TextSegment> store) {
        try {
            store.addAll(embedder.embedDocuments(batch), batch);
            return batch.size();
        } catch (RuntimeException batchFailure) {
            int ok = 0;
            for (TextSegment segment : batch) {
                try {
                    store.add(embedder.embedDocument(segment.text()), segment);
                    ok++;
                } catch (RuntimeException ignored) {
                    // counted as failed by the caller
                }
            }
            return ok;
        }
    }

    public static @NotNull String toJson(@NotNull Result result) {
        return toJson(result, Map.of());
    }

    /** Pretty JSON of {@code result}, with {@code runInfo} (model, URL, git commit, …) under {@code "run"}. */
    public static @NotNull String toJson(@NotNull Result result, @NotNull Map<String, String> runInfo) {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode root = mapper.createObjectNode();
        root.set("run", mapper.valueToTree(runInfo));
        root.setAll((ObjectNode) mapper.valueToTree(result));
        try {
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise benchmark result", e);
        }
    }
}
