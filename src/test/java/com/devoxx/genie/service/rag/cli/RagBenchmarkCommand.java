package com.devoxx.genie.service.rag.cli;

import com.devoxx.genie.model.ProjectScanDefaults;
import com.devoxx.genie.service.rag.benchmark.BenchmarkCorpus;
import com.devoxx.genie.service.rag.benchmark.BenchmarkDataset;
import com.devoxx.genie.service.rag.benchmark.RagBenchmark;
import com.devoxx.genie.service.rag.benchmark.RetrievalMetrics;
import com.devoxx.genie.service.rag.chunking.RagChunker;
import com.devoxx.genie.service.rag.embedding.EmbeddingProfile;
import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import com.devoxx.genie.service.rag.embedding.ProfiledEmbedder;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * {@code RagCli benchmark}: measures RAG retrieval quality for one Ollama embedding
 * configuration against a question dataset. See
 * {@code src/test/resources/rag-benchmark/README.md}.
 *
 * <p>Runs entirely outside the IDE and never touches ChromaDB; vectors are kept in memory.
 */
public final class RagBenchmarkCommand {

    static final String DEFAULT_DATASET = "src/test/resources/rag-benchmark/devoxxgenie-questions.json";
    static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";
    static final String DEFAULT_MODEL = EmbeddingFingerprint.DEFAULT_MODEL_NAME;
    static final int DEFAULT_TOP_CHUNKS = 50;
    /** A question "passes" in the per-question list when an expected file is in the top 5. */
    static final int PASS_AT = 5;

    private static final Map<String, String[]> PREFIX_PRESETS = Map.of(
            "none", new String[]{"", ""},
            "nomic", new String[]{"search_query: ", "search_document: "}
    );

    private RagBenchmarkCommand() {}

    /** Parsed command-line options; flags win over environment variables, which win over defaults. */
    record Options(Path dataset, Path repo, String ollamaUrl, String model,
                   EmbeddingProfile profile, int topChunks, @Nullable Path out) {

        static Options parse(String[] args, UnaryOperator<String> env) {
            Map<String, String> flags = new LinkedHashMap<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (!arg.startsWith("--")) {
                    throw new IllegalArgumentException("Unexpected argument: " + arg);
                }
                String name;
                String value;
                int eq = arg.indexOf('=');
                if (eq > 0) {
                    name = arg.substring(0, eq);
                    value = arg.substring(eq + 1);
                } else {
                    name = arg;
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for " + name);
                    }
                    value = args[++i];
                }
                if (!KNOWN_FLAGS.contains(name)) {
                    throw new IllegalArgumentException("Unknown option: " + name);
                }
                flags.put(name, value);
            }

            // Precedence: explicit prefix flag > --prefix-preset flag > environment variable > none.
            String presetQuery = null;
            String presetDoc = null;
            if (flags.containsKey("--prefix-preset")) {
                String[] preset = PREFIX_PRESETS.get(flags.get("--prefix-preset").toLowerCase(Locale.ROOT));
                if (preset == null) {
                    throw new IllegalArgumentException("Unknown --prefix-preset '" + flags.get("--prefix-preset")
                            + "'; known: " + PREFIX_PRESETS.keySet());
                }
                presetQuery = preset[0];
                presetDoc = preset[1];
            }
            String queryPrefix = pick(pick(flags.get("--query-prefix"), presetQuery, null),
                    env.apply("DEVOXXGENIE_EMBED_QUERY_PREFIX"), "");
            String docPrefix = pick(pick(flags.get("--doc-prefix"), presetDoc, null),
                    env.apply("DEVOXXGENIE_EMBED_DOC_PREFIX"), "");
            String dimension = pick(flags.get("--dimension"), env.apply("DEVOXXGENIE_EMBED_DIMENSION"), null);

            String out = flags.get("--out");
            return new Options(
                    Path.of(flags.getOrDefault("--dataset", DEFAULT_DATASET)),
                    Path.of(flags.getOrDefault("--repo", ".")),
                    pick(flags.get("--ollama-url"), env.apply("DEVOXXGENIE_OLLAMA_URL"), DEFAULT_OLLAMA_URL),
                    pick(flags.get("--model"), env.apply("DEVOXXGENIE_EMBED_MODEL"), DEFAULT_MODEL),
                    new EmbeddingProfile(queryPrefix, docPrefix, dimension == null ? null : positiveInt("--dimension", dimension)),
                    flags.containsKey("--top-chunks") ? positiveInt("--top-chunks", flags.get("--top-chunks")) : DEFAULT_TOP_CHUNKS,
                    out == null ? null : Path.of(out));
        }

        private static final List<String> KNOWN_FLAGS = List.of(
                "--dataset", "--repo", "--ollama-url", "--model", "--query-prefix", "--doc-prefix",
                "--prefix-preset", "--dimension", "--top-chunks", "--out");

        /** Prefixes may legitimately be empty, so only {@code null} means "not given". */
        private static String pick(@Nullable String flag, @Nullable String envValue, @Nullable String fallback) {
            if (flag != null) return flag;
            if (envValue != null) return envValue;
            return fallback;
        }

        private static int positiveInt(String name, String value) {
            try {
                int n = Integer.parseInt(value.trim());
                if (n <= 0) throw new IllegalArgumentException(name + " must be positive, was " + value);
                return n;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(name + " must be a number, was '" + value + "'");
            }
        }
    }

    static void run(String[] args) throws IOException, InterruptedException {
        Options options;
        try {
            options = Options.parse(args, System::getenv);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println();
            printUsage();
            System.exit(2);
            return;
        }

        Path repo = options.repo().toAbsolutePath().normalize();
        BenchmarkDataset dataset = BenchmarkDataset.load(options.dataset());
        List<String> corpus = BenchmarkCorpus.select(gitLsFiles(repo),
                ProjectScanDefaults.INCLUDED_FILE_EXTENSIONS,
                ProjectScanDefaults.EXCLUDED_DIRECTORIES,
                dataset.excludePaths());

        System.err.printf(Locale.ROOT, "Benchmark '%s' v%d: %d questions, %d corpus files%n",
                dataset.name(), dataset.version(), dataset.questions().size(), corpus.size());
        System.err.printf(Locale.ROOT, "Model %s @ %s, query prefix %s, document prefix %s, dimension %s%n",
                options.model(), options.ollamaUrl(), quote(options.profile().queryPrefix()),
                quote(options.profile().documentPrefix()),
                options.profile().outputDimension() == null ? "native" : options.profile().outputDimension());

        OllamaEmbeddingModel model = OllamaEmbeddingModel.builder()
                .baseUrl(options.ollamaUrl())
                .modelName(options.model())
                .timeout(Duration.ofMinutes(5))
                .build();
        RagBenchmark.Result result = new RagBenchmark(new ProfiledEmbedder(model, options.profile()), new RagChunker())
                .withProgress(System.err)
                .run(repo, corpus, dataset, options.topChunks());

        printReport(result);

        if (options.out() != null) {
            Map<String, String> runInfo = new LinkedHashMap<>();
            runInfo.put("timestamp", Instant.now().toString());
            runInfo.put("model", options.model());
            runInfo.put("ollamaUrl", options.ollamaUrl());
            runInfo.put("gitCommit", git(repo, "rev-parse", "--short", "HEAD"));
            runInfo.put("gitDirty", String.valueOf(!git(repo, "status", "--porcelain").isEmpty()));
            Path out = options.out();
            if (out.getParent() != null) Files.createDirectories(out.getParent());
            Files.writeString(out, RagBenchmark.toJson(result, runInfo));
            System.err.println("Wrote " + out.toAbsolutePath());
        }
    }

    static void printReport(RagBenchmark.Result result) {
        System.out.println();
        System.out.printf(Locale.ROOT, "%-4s %-5s %-40s %s%n", "pass", "rank", "question", "best retrieved file (on miss)");
        for (RagBenchmark.QuestionResult q : result.questions()) {
            boolean pass = q.rank() >= 1 && q.rank() <= PASS_AT;
            String detail = pass || q.topFiles().isEmpty() ? "" : q.topFiles().get(0);
            System.out.printf(Locale.ROOT, "%-4s %-5s %-40s %s%n", pass ? "✓" : "✗",
                    q.rank() == 0 ? "-" : String.valueOf(q.rank()), q.id(), detail);
        }
        RetrievalMetrics.Summary s = result.summary();
        System.out.println();
        System.out.printf(Locale.ROOT, "Questions: %d   files: %d (skipped %d)   chunks: %d (failed %d)%n",
                s.questions(), result.corpusFiles(), result.skippedFiles(), result.chunks(), result.failedChunks());
        System.out.printf(Locale.ROOT, "recall@1 %.3f   recall@5 %.3f   recall@10 %.3f   MRR %.3f%n",
                s.recallAt1(), s.recallAt5(), s.recallAt10(), s.mrr());
        System.out.printf(Locale.ROOT, "Index %.1fs   queries %.1fs%n", result.indexMillis() / 1000.0, result.queryMillis() / 1000.0);
    }

    private static @NotNull List<String> gitLsFiles(Path repo) throws IOException, InterruptedException {
        String out = git(repo, "ls-files");
        if (out.isEmpty()) {
            throw new IllegalStateException("git ls-files returned nothing; is " + repo + " a git repository?");
        }
        return Arrays.stream(out.split("\n")).filter(s -> !s.isBlank()).toList();
    }

    private static @NotNull String git(Path repo, String... args) throws IOException, InterruptedException {
        String[] cmd = new String[args.length + 3];
        cmd[0] = "git";
        cmd[1] = "-C";
        cmd[2] = repo.toString();
        System.arraycopy(args, 0, cmd, 3, args.length);
        Process p = new ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        String output;
        try (InputStream in = p.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        p.waitFor();
        return output.strip();
    }

    private static String quote(String s) {
        return s.isEmpty() ? "(none)" : "'" + s + "'";
    }

    static void printUsage() {
        System.err.println("""
                Usage:
                  ragBenchmark [options]          (or: ragQuery --args="benchmark [options]")

                Options (flag > environment variable > default):
                  --dataset <file>        question set (default src/test/resources/rag-benchmark/devoxxgenie-questions.json)
                  --repo <dir>            repository to index (default .)
                  --ollama-url <url>      DEVOXXGENIE_OLLAMA_URL         (default http://localhost:11434)
                  --model <name>          DEVOXXGENIE_EMBED_MODEL        (default nomic-embed-text)
                  --query-prefix <text>   DEVOXXGENIE_EMBED_QUERY_PREFIX (default none)
                  --doc-prefix <text>     DEVOXXGENIE_EMBED_DOC_PREFIX   (default none)
                  --prefix-preset <name>  none | nomic  (explicit prefixes win)
                  --dimension <n>         DEVOXXGENIE_EMBED_DIMENSION    (default: model's native size)
                  --top-chunks <n>        chunks retrieved per question (default 50)
                  --out <file>            also write the full result as JSON

                Prefixes ending in a space: use the environment variable (Gradle --args splits on
                spaces), e.g. DEVOXXGENIE_EMBED_QUERY_PREFIX="Query: ", or --prefix-preset nomic.
                """);
    }
}
