package com.devoxx.genie.service.rag.benchmark;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A retrieval benchmark: natural-language questions about one repository, each paired with
 * the repo-relative file path(s) whose chunks answer it. Loaded from JSON; see
 * {@code src/test/resources/rag-benchmark/README.md} for the format.
 *
 * @param excludePaths repo-relative path prefixes left out of the corpus (e.g. test fixtures
 *                     that are copies of real files and would compete with them)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BenchmarkDataset(String name,
                               int version,
                               String description,
                               List<String> excludePaths,
                               List<Question> questions) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Question(String id, String question, List<String> expected) {}

    public BenchmarkDataset {
        excludePaths = excludePaths == null ? List.of() : List.copyOf(excludePaths);
        questions = questions == null ? List.of() : List.copyOf(questions);
        Set<String> ids = new HashSet<>();
        for (Question q : questions) {
            if (q.id() == null || q.id().isBlank()) {
                throw new IllegalArgumentException("Every question needs an id");
            }
            if (!ids.add(q.id())) {
                throw new IllegalArgumentException("Duplicate question id: " + q.id());
            }
            if (q.question() == null || q.question().isBlank()) {
                throw new IllegalArgumentException("Question " + q.id() + " has no text");
            }
            if (q.expected() == null || q.expected().isEmpty()) {
                throw new IllegalArgumentException("Question " + q.id() + " has no expected files");
            }
        }
    }

    public static @NotNull BenchmarkDataset load(@NotNull Path file) throws IOException {
        try {
            return new ObjectMapper().readValue(file.toFile(), BenchmarkDataset.class);
        } catch (com.fasterxml.jackson.databind.exc.ValueInstantiationException e) {
            // Surface validation failures from the compact constructor as-is.
            if (e.getCause() instanceof IllegalArgumentException iae) {
                throw iae;
            }
            throw e;
        }
    }
}
