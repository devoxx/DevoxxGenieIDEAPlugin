package com.devoxx.genie.service.rag.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RagBenchmarkCommandTest {

    private static RagBenchmarkCommand.Options parse(Map<String, String> env, String... args) {
        return RagBenchmarkCommand.Options.parse(args, env::get);
    }

    @Test
    void defaultsMatchTheCurrentPluginSetup() {
        RagBenchmarkCommand.Options o = parse(Map.of());

        assertThat(o.ollamaUrl()).isEqualTo("http://localhost:11434");
        assertThat(o.model()).isEqualTo("nomic-embed-text");
        assertThat(o.profile().queryPrefix()).isEmpty();
        assertThat(o.profile().documentPrefix()).isEmpty();
        assertThat(o.profile().outputDimension()).isNull();
        assertThat(o.dataset()).isEqualTo(Path.of("src/test/resources/rag-benchmark/devoxxgenie-questions.json"));
        assertThat(o.repo()).isEqualTo(Path.of("."));
        assertThat(o.topChunks()).isEqualTo(50);
        assertThat(o.out()).isNull();
    }

    @Test
    void flagsOverrideEverything() {
        RagBenchmarkCommand.Options o = parse(
                Map.of("DEVOXXGENIE_EMBED_MODEL", "from-env"),
                "--model", "bge-m3", "--ollama-url", "http://gpu:11434",
                "--query-prefix", "Query: ", "--doc-prefix", "Document: ", "--dimension", "256",
                "--top-chunks", "80", "--out", "build/r.json", "--repo", "/r", "--dataset", "d.json");

        assertThat(o.model()).isEqualTo("bge-m3");
        assertThat(o.ollamaUrl()).isEqualTo("http://gpu:11434");
        assertThat(o.profile().queryPrefix()).isEqualTo("Query: ");
        assertThat(o.profile().documentPrefix()).isEqualTo("Document: ");
        assertThat(o.profile().outputDimension()).isEqualTo(256);
        assertThat(o.topChunks()).isEqualTo(80);
        assertThat(o.out()).isEqualTo(Path.of("build/r.json"));
        assertThat(o.repo()).isEqualTo(Path.of("/r"));
        assertThat(o.dataset()).isEqualTo(Path.of("d.json"));
    }

    @Test
    void equalsSignFormKeepsTrailingSpaces() {
        RagBenchmarkCommand.Options o = parse(Map.of(), "--query-prefix=search_query: ");

        assertThat(o.profile().queryPrefix()).isEqualTo("search_query: ");
    }

    @Test
    void environmentVariablesAreUsedWhenNoFlagIsGiven() {
        RagBenchmarkCommand.Options o = parse(Map.of(
                "DEVOXXGENIE_OLLAMA_URL", "http://env:1",
                "DEVOXXGENIE_EMBED_MODEL", "env-model",
                "DEVOXXGENIE_EMBED_QUERY_PREFIX", "q> ",
                "DEVOXXGENIE_EMBED_DOC_PREFIX", "d> ",
                "DEVOXXGENIE_EMBED_DIMENSION", "512"));

        assertThat(o.ollamaUrl()).isEqualTo("http://env:1");
        assertThat(o.model()).isEqualTo("env-model");
        assertThat(o.profile().queryPrefix()).isEqualTo("q> ");
        assertThat(o.profile().documentPrefix()).isEqualTo("d> ");
        assertThat(o.profile().outputDimension()).isEqualTo(512);
    }

    @Test
    void nomicPresetSetsBothPrefixes() {
        RagBenchmarkCommand.Options o = parse(Map.of(), "--prefix-preset", "nomic");

        assertThat(o.profile().queryPrefix()).isEqualTo("search_query: ");
        assertThat(o.profile().documentPrefix()).isEqualTo("search_document: ");
    }

    @Test
    void explicitPrefixWinsOverPreset() {
        RagBenchmarkCommand.Options o = parse(Map.of(), "--prefix-preset", "nomic", "--query-prefix", "custom: ");

        assertThat(o.profile().queryPrefix()).isEqualTo("custom: ");
        assertThat(o.profile().documentPrefix()).isEqualTo("search_document: ");
    }

    @Test
    void presetFlagWinsOverEnvironmentPrefixes() {
        // Flags beat environment variables; the preset is a flag.
        RagBenchmarkCommand.Options o = parse(Map.of(
                        "DEVOXXGENIE_EMBED_QUERY_PREFIX", "env-q: ",
                        "DEVOXXGENIE_EMBED_DOC_PREFIX", "env-d: "),
                "--prefix-preset", "nomic");

        assertThat(o.profile().queryPrefix()).isEqualTo("search_query: ");
        assertThat(o.profile().documentPrefix()).isEqualTo("search_document: ");
    }

    @Test
    void unknownPresetOrFlagIsRejected() {
        assertThatThrownBy(() -> parse(Map.of(), "--prefix-preset", "nope"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nope");
        assertThatThrownBy(() -> parse(Map.of(), "--modle", "x"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("--modle");
    }

    @Test
    void flagWithoutValueIsRejected() {
        assertThatThrownBy(() -> parse(Map.of(), "--model"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("--model");
    }

    @Test
    void invalidNumbersAreRejected() {
        assertThatThrownBy(() -> parse(Map.of(), "--dimension", "abc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parse(Map.of(), "--top-chunks", "0")).isInstanceOf(IllegalArgumentException.class);
    }
}
