package com.devoxx.genie.service.rag.benchmark;

import com.devoxx.genie.model.ProjectScanDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Keeps the checked-in benchmark dataset honest: if a file moves or a question can no longer
 * be answered by the corpus, this fails in CI instead of silently dragging the scores down.
 */
class BenchmarkDatasetTest {

    static final Path DATASET = Path.of("src/test/resources/rag-benchmark/devoxxgenie-questions.json");

    @Test
    void checkedInDatasetHasEnoughWellFormedQuestions() throws IOException {
        BenchmarkDataset dataset = BenchmarkDataset.load(DATASET);

        assertThat(dataset.questions()).hasSizeGreaterThanOrEqualTo(20);
        Set<String> ids = new HashSet<>();
        for (BenchmarkDataset.Question q : dataset.questions()) {
            assertThat(ids.add(q.id())).as("duplicate id " + q.id()).isTrue();
            assertThat(q.question()).as(q.id()).isNotBlank();
            assertThat(q.expected()).as(q.id()).isNotEmpty();
        }
    }

    @Test
    void everyExpectedFileExistsInTheRepository() throws IOException {
        for (BenchmarkDataset.Question q : BenchmarkDataset.load(DATASET).questions()) {
            for (String path : q.expected()) {
                assertThat(Path.of(path)).as("question '%s' expects a file that no longer exists", q.id())
                        .isRegularFile();
            }
        }
    }

    @Test
    void everyExpectedFileIsPartOfTheBenchmarkCorpus() throws IOException {
        // An expected file the corpus filter drops can never be retrieved, so its question
        // would always score as a miss.
        BenchmarkDataset dataset = BenchmarkDataset.load(DATASET);
        for (BenchmarkDataset.Question q : dataset.questions()) {
            List<String> kept = BenchmarkCorpus.select(q.expected(),
                    ProjectScanDefaults.INCLUDED_FILE_EXTENSIONS,
                    ProjectScanDefaults.EXCLUDED_DIRECTORIES,
                    dataset.excludePaths());
            assertThat(kept).as("question '%s' expects files outside the corpus", q.id())
                    .containsExactlyElementsOf(q.expected());
        }
    }

    @Test
    void duplicateIdsAreRejected(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("d.json");
        Files.writeString(file, """
                {"name":"x","questions":[
                  {"id":"a","question":"q1","expected":["A.java"]},
                  {"id":"a","question":"q2","expected":["B.java"]}]}
                """);

        assertThatThrownBy(() -> BenchmarkDataset.load(file))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("a");
    }

    @Test
    void questionWithoutExpectedFilesIsRejected(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("d.json");
        Files.writeString(file, """
                {"name":"x","questions":[{"id":"a","question":"q1","expected":[]}]}
                """);

        assertThatThrownBy(() -> BenchmarkDataset.load(file)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingExcludePathsDefaultsToEmpty(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("d.json");
        Files.writeString(file, """
                {"name":"x","questions":[{"id":"a","question":"q1","expected":["A.java"]}]}
                """);

        assertThat(BenchmarkDataset.load(file).excludePaths()).isEmpty();
    }
}
