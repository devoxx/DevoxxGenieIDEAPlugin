package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.BacklogConfig;
import com.devoxx.genie.model.spec.BacklogDocument;
import com.devoxx.genie.model.spec.DefinitionOfDoneItem;
import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpecServiceTest {

    private static final String TASK_1 = """
            ---
            id: TASK-1
            title: First task
            status: To Do
            priority: high
            assignee:
              - alice
            labels:
              - bug
              - urgent
            dependencies: []
            references: []
            documentation: []
            ordinal: 1
            ---

            This is the first task description.
            """;

    private static final String TASK_2 = """
            ---
            id: TASK-2
            title: Second task
            status: In Progress
            priority: medium
            assignee:
              - bob
            labels:
              - feature
            dependencies:
              - TASK-1
            references: []
            documentation: []
            ordinal: 2
            ---

            This is the second task.
            """;

    private static final String TASK_3 = """
            ---
            id: TASK-3
            title: Third task done
            status: Done
            priority: low
            assignee: []
            labels: []
            dependencies: []
            references: []
            documentation: []
            ordinal: 3
            ---

            Completed task.
            """;

    private static final String DOC_1 = """
            ---
            id: DOC-1
            title: Architecture overview
            ---

            This document describes the architecture.
            """;

    private static final String DOC_2 = """
            ---
            id: DOC-2
            title: API reference
            ---

            Endpoints and methods for the REST API.
            """;

    // ── Task read operations ───────────────────────────────────────────

    @Test
    void getAllSpecs_returnsAllCachedTasks(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2);
            SpecService service = mocks.createService();

            List<TaskSpec> specs = service.getAllSpecs();

            assertThat(specs).hasSize(2);
            assertThat(specs).extracting(TaskSpec::getId)
                    .containsExactlyInAnyOrder("TASK-1", "TASK-2");
        }
    }

    @Test
    void getAllSpecs_returnsEmptyList_whenNoTasks(@TempDir Path tempDir) {
        try (var mocks = new MockContext(tempDir)) {
            SpecService service = mocks.createService();

            assertThat(service.getAllSpecs()).isEmpty();
        }
    }

    @Test
    void getSpec_returnsSpecById(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1);
            SpecService service = mocks.createService();

            TaskSpec spec = service.getSpec("TASK-1");

            assertThat(spec).isNotNull();
            assertThat(spec.getId()).isEqualTo("TASK-1");
            assertThat(spec.getTitle()).isEqualTo("First task");
            assertThat(spec.getStatus()).isEqualTo("To Do");
            assertThat(spec.getPriority()).isEqualTo("high");
        }
    }

    @Test
    void getSpec_caseInsensitive(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1);
            SpecService service = mocks.createService();

            assertThat(service.getSpec("task-1")).isNotNull();
            assertThat(service.getSpec("Task-1")).isNotNull();
        }
    }

    @Test
    void getSpec_returnsNull_whenNotFound(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1);
            SpecService service = mocks.createService();

            assertThat(service.getSpec("TASK-999")).isNull();
        }
    }

    @Test
    void getSpecsByStatus_filtersCorrectly(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            List<TaskSpec> todo = service.getSpecsByStatus("To Do");
            List<TaskSpec> inProgress = service.getSpecsByStatus("In Progress");
            List<TaskSpec> done = service.getSpecsByStatus("Done");

            assertThat(todo).hasSize(1);
            assertThat(todo.get(0).getId()).isEqualTo("TASK-1");
            assertThat(inProgress).hasSize(1);
            assertThat(inProgress.get(0).getId()).isEqualTo("TASK-2");
            assertThat(done).hasSize(1);
            assertThat(done.get(0).getId()).isEqualTo("TASK-3");
        }
    }

    @Test
    void getSpecsByStatus_caseInsensitive(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1);
            SpecService service = mocks.createService();

            assertThat(service.getSpecsByStatus("to do")).hasSize(1);
        }
    }

    @Test
    void getStatuses_returnsDistinctStatuses(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            List<String> statuses = service.getStatuses();

            assertThat(statuses).containsExactlyInAnyOrder("To Do", "In Progress", "Done");
        }
    }

    @Test
    void getStatuses_returnsEmptyList_whenNoTasks(@TempDir Path tempDir) {
        try (var mocks = new MockContext(tempDir)) {
            SpecService service = mocks.createService();

            assertThat(service.getStatuses()).isEmpty();
        }
    }

    // ── getSpecsByFilters ──────────────────────────────────────────────

    @Test
    void getSpecsByFilters_filterByStatus(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            List<TaskSpec> results = service.getSpecsByFilters("To Do", null, null, null, 0);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo("TASK-1");
        }
    }

    @Test
    void getSpecsByFilters_filterByAssignee(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            List<TaskSpec> results = service.getSpecsByFilters(null, "alice", null, null, 0);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo("TASK-1");
        }
    }

    @Test
    void getSpecsByFilters_filterByLabels(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            List<TaskSpec> results = service.getSpecsByFilters(null, null, List.of("bug"), null, 0);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo("TASK-1");
        }
    }

    @Test
    void getSpecsByFilters_filterBySearch(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            List<TaskSpec> results = service.getSpecsByFilters(null, null, null, "Second", 0);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo("TASK-2");
        }
    }

    @Test
    void getSpecsByFilters_searchMatchesId(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2);
            SpecService service = mocks.createService();

            List<TaskSpec> results = service.getSpecsByFilters(null, null, null, "TASK-2", 0);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo("TASK-2");
        }
    }

    @Test
    void getSpecsByFilters_searchMatchesDescription(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2);
            SpecService service = mocks.createService();

            List<TaskSpec> results = service.getSpecsByFilters(null, null, null, "first task description", 0);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo("TASK-1");
        }
    }

    @Test
    void getSpecsByFilters_withLimit(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            List<TaskSpec> results = service.getSpecsByFilters(null, null, null, null, 2);

            assertThat(results).hasSize(2);
        }
    }

    @Test
    void getSpecsByFilters_noFilters_returnsAll(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2);
            SpecService service = mocks.createService();

            List<TaskSpec> results = service.getSpecsByFilters(null, null, null, null, 0);

            assertThat(results).hasSize(2);
        }
    }

    @Test
    void getSpecsByFilters_emptyStringsIgnored(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2);
            SpecService service = mocks.createService();

            List<TaskSpec> results = service.getSpecsByFilters("", "", List.of(), "", 0);

            assertThat(results).hasSize(2);
        }
    }

    // ── searchSpecs ────────────────────────────────────────────────────

    @Test
    void searchSpecs_matchesTitleAndDescription(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            List<TaskSpec> byTitle = service.searchSpecs("First", null, null, 0);
            assertThat(byTitle).hasSize(1);
            assertThat(byTitle.get(0).getId()).isEqualTo("TASK-1");

            List<TaskSpec> byDesc = service.searchSpecs("Completed", null, null, 0);
            assertThat(byDesc).hasSize(1);
            assertThat(byDesc.get(0).getId()).isEqualTo("TASK-3");
        }
    }

    @Test
    void searchSpecs_filterByStatus(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            List<TaskSpec> results = service.searchSpecs("task", "To Do", null, 0);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo("TASK-1");
        }
    }

    @Test
    void searchSpecs_filterByPriority(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            List<TaskSpec> results = service.searchSpecs("task", null, "high", 0);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo("TASK-1");
        }
    }

    @Test
    void searchSpecs_withLimit(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            List<TaskSpec> results = service.searchSpecs("task", null, null, 1);

            assertThat(results).hasSize(1);
        }
    }

    @Test
    void searchSpecs_emptyFiltersIgnored(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            List<TaskSpec> results = service.searchSpecs("task", "", "", 0);

            assertThat(results).hasSize(3);
        }
    }

    // ── Fuzzy search (searchSpecs) ───────────────────────────────────────

    @Test
    void searchSpecs_fuzzyMatchesTitleWithTypo(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            // "Frist" is a typo for "First"
            List<TaskSpec> results = service.searchSpecs("Frist task", null, null, 0);

            assertThat(results).isNotEmpty();
            assertThat(results).extracting(TaskSpec::getId).contains("TASK-1");
        }
    }

    @Test
    void searchSpecs_fuzzyMatchesPartialWord(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            // "Secon" is partial prefix of "Second"
            List<TaskSpec> results = service.searchSpecs("Secon", null, null, 0);

            assertThat(results).isNotEmpty();
            assertThat(results).extracting(TaskSpec::getId).contains("TASK-2");
        }
    }

    @Test
    void searchSpecs_fuzzyRanksExactMatchFirst(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            // "Second task" is an exact substring in TASK-2's title
            // "second" also partially matches "first task description" (less so)
            List<TaskSpec> results = service.searchSpecs("Second task", null, null, 0);

            assertThat(results).isNotEmpty();
            assertThat(results.get(0).getId()).isEqualTo("TASK-2");
        }
    }

    @Test
    void searchSpecs_fuzzyNoMatchForUnrelatedQuery(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            List<TaskSpec> results = service.searchSpecs("database migration", null, null, 0);

            assertThat(results).isEmpty();
        }
    }

    @Test
    void searchSpecs_fuzzyMatchesReversedTokens(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            // Tokens in reversed order should still match
            List<TaskSpec> results = service.searchSpecs("task First", null, null, 0);

            assertThat(results).isNotEmpty();
            assertThat(results).extracting(TaskSpec::getId).contains("TASK-1");
        }
    }

    // ── Fuzzy search (getSpecsByFilters) ───────────────────────────────────

    @Test
    void getSpecsByFilters_fuzzySearchMatchesWithTypo(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            // "Secnd" is a typo for "Second"
            List<TaskSpec> results = service.getSpecsByFilters(null, null, null, "Secnd", 0);

            assertThat(results).isNotEmpty();
            assertThat(results).extracting(TaskSpec::getId).contains("TASK-2");
        }
    }

    @Test
    void getSpecsByFilters_fuzzySearchCombinedWithStatusFilter(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1, TASK_2, TASK_3);
            SpecService service = mocks.createService();

            // "task" matches all, but filter by "To Do" status
            List<TaskSpec> results = service.getSpecsByFilters("To Do", null, null, "task", 0);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo("TASK-1");
        }
    }

    // ── Fuzzy search (searchDocuments) ──────────────────────────────────────

    @Test
    void searchDocuments_fuzzyMatchesTitleWithTypo(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedDocFiles(tempDir, DOC_1, DOC_2);
            SpecService service = mocks.createService();

            // "Architectre" is a typo for "Architecture"
            List<BacklogDocument> results = service.searchDocuments("Architectre", 0);

            assertThat(results).isNotEmpty();
            assertThat(results).extracting(BacklogDocument::getId).contains("DOC-1");
        }
    }

    @Test
    void searchDocuments_fuzzyRanksExactMatchFirst(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedDocFiles(tempDir, DOC_1, DOC_2);
            SpecService service = mocks.createService();

            // "API" exact substring match in DOC-2
            List<BacklogDocument> results = service.searchDocuments("API", 0);

            assertThat(results).isNotEmpty();
            assertThat(results.get(0).getId()).isEqualTo("DOC-2");
        }
    }

    @Test
    void searchDocuments_fuzzyNoMatchForUnrelated(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedDocFiles(tempDir, DOC_1, DOC_2);
            SpecService service = mocks.createService();

            List<BacklogDocument> results = service.searchDocuments("kubernetes deployment", 0);

            assertThat(results).isEmpty();
        }
    }

    // ── Completed directory scanning ───────────────────────────────────

    @Test
    void refresh_scansCompletedDirectory(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            Path completedDir = tempDir.resolve("backlog/completed");
            Files.createDirectories(completedDir);
            Files.writeString(completedDir.resolve("task-3.md"), TASK_3, StandardCharsets.UTF_8);

            SpecService service = mocks.createService();

            assertThat(service.getSpec("TASK-3")).isNotNull();
            assertThat(service.getSpec("TASK-3").getStatus()).isEqualTo("Done");
        }
    }

    // ── hasSpecDirectory ───────────────────────────────────────────────

    @Test
    void hasSpecDirectory_returnsTrueWhenExists(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            Files.createDirectories(tempDir.resolve("backlog"));
            SpecService service = mocks.createService();

            assertThat(service.hasSpecDirectory()).isTrue();
        }
    }

    @Test
    void hasSpecDirectory_returnsFalseWhenMissing(@TempDir Path tempDir) {
        try (var mocks = new MockContext(tempDir)) {
            // Don't create backlog dir
            SpecService service = mocks.createService();

            assertThat(service.hasSpecDirectory()).isFalse();
        }
    }

    // ── Document read operations ───────────────────────────────────────

    @Test
    void getAllDocuments_returnsCachedDocs(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedDocFiles(tempDir, DOC_1, DOC_2);
            SpecService service = mocks.createService();

            List<BacklogDocument> docs = service.getAllDocuments();

            assertThat(docs).hasSize(2);
            assertThat(docs).extracting(BacklogDocument::getId)
                    .containsExactlyInAnyOrder("DOC-1", "DOC-2");
        }
    }

    @Test
    void getDocument_returnsDocById(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedDocFiles(tempDir, DOC_1);
            SpecService service = mocks.createService();

            BacklogDocument doc = service.getDocument("DOC-1");

            assertThat(doc).isNotNull();
            assertThat(doc.getTitle()).isEqualTo("Architecture overview");
            assertThat(doc.getContent()).contains("architecture");
        }
    }

    @Test
    void getDocument_caseInsensitive(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedDocFiles(tempDir, DOC_1);
            SpecService service = mocks.createService();

            assertThat(service.getDocument("doc-1")).isNotNull();
        }
    }

    @Test
    void getDocument_returnsNull_whenNotFound(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedDocFiles(tempDir, DOC_1);
            SpecService service = mocks.createService();

            assertThat(service.getDocument("DOC-999")).isNull();
        }
    }

    @Test
    void searchDocuments_matchesTitleAndContent(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedDocFiles(tempDir, DOC_1, DOC_2);
            SpecService service = mocks.createService();

            List<BacklogDocument> byTitle = service.searchDocuments("Architecture", 0);
            assertThat(byTitle).hasSize(1);
            assertThat(byTitle.get(0).getId()).isEqualTo("DOC-1");

            List<BacklogDocument> byContent = service.searchDocuments("REST API", 0);
            assertThat(byContent).hasSize(1);
            assertThat(byContent.get(0).getId()).isEqualTo("DOC-2");
        }
    }

    @Test
    void searchDocuments_withLimit(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedDocFiles(tempDir, DOC_1, DOC_2);
            SpecService service = mocks.createService();

            List<BacklogDocument> results = service.searchDocuments("doc", 1);

            assertThat(results).hasSize(1);
        }
    }

    @Test
    void listDocuments_returnsAll_whenNoSearch(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedDocFiles(tempDir, DOC_1, DOC_2);
            SpecService service = mocks.createService();

            assertThat(service.listDocuments(null)).hasSize(2);
            assertThat(service.listDocuments("")).hasSize(2);
        }
    }

    @Test
    void listDocuments_filtersWhenSearchProvided(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedDocFiles(tempDir, DOC_1, DOC_2);
            SpecService service = mocks.createService();

            List<BacklogDocument> results = service.listDocuments("API");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo("DOC-2");
        }
    }

    // ── Document parsing edge cases ────────────────────────────────────

    @Test
    void refresh_ignoresDocWithoutFrontmatter(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            Path docsDir = tempDir.resolve("backlog/docs");
            Files.createDirectories(docsDir);
            Files.writeString(docsDir.resolve("plain.md"), "Just plain markdown, no frontmatter.", StandardCharsets.UTF_8);

            SpecService service = mocks.createService();

            assertThat(service.getAllDocuments()).isEmpty();
        }
    }

    @Test
    void refresh_ignoresDocWithoutId(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            Path docsDir = tempDir.resolve("backlog/docs");
            Files.createDirectories(docsDir);
            Files.writeString(docsDir.resolve("noid.md"), """
                    ---
                    title: No ID doc
                    ---

                    Content here.
                    """, StandardCharsets.UTF_8);

            SpecService service = mocks.createService();

            assertThat(service.getAllDocuments()).isEmpty();
        }
    }

    // ── Task write operations ──────────────────────────────────────────

    @Test
    void createTask_generatesIdAndWritesFile(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec spec = TaskSpec.builder()
                    .title("New task")
                    .description("Task description")
                    .build();
            TaskSpec created = service.createTask(spec);

            assertThat(created.getId()).isNotNull();
            assertThat(created.getId()).startsWith("TASK-");
            assertThat(created.getFilePath()).isNotNull();
            assertThat(created.getCreatedAt()).isNotNull();
            assertThat(Files.exists(Path.of(created.getFilePath()))).isTrue();
        }
    }

    @Test
    void createTask_usesExistingIdIfProvided(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec spec = TaskSpec.builder()
                    .id("CUSTOM-42")
                    .title("Custom ID task")
                    .build();
            TaskSpec created = service.createTask(spec);

            assertThat(created.getId()).isEqualTo("CUSTOM-42");
        }
    }

    @Test
    void createTask_preservesExistingCreatedAt(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec spec = TaskSpec.builder()
                    .title("Timed task")
                    .createdAt("2024-01-15 10:00")
                    .build();
            TaskSpec created = service.createTask(spec);

            assertThat(created.getCreatedAt()).isEqualTo("2024-01-15 10:00");
        }
    }

    @Test
    void createTask_isRetrievableAfterCreation(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec spec = TaskSpec.builder()
                    .title("Findable task")
                    .build();
            TaskSpec created = service.createTask(spec);

            TaskSpec found = service.getSpec(created.getId());
            assertThat(found).isNotNull();
            assertThat(found.getTitle()).isEqualTo("Findable task");
        }
    }

    @Test
    void updateTask_writesChangesToDisk(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec created = service.createTask(TaskSpec.builder().title("Original").build());
            created.setTitle("Updated title");
            created.setStatus("In Progress");
            service.updateTask(created);

            TaskSpec reloaded = service.getSpec(created.getId());
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getTitle()).isEqualTo("Updated title");
            assertThat(reloaded.getStatus()).isEqualTo("In Progress");
            assertThat(reloaded.getUpdatedAt()).isNotNull();
        }
    }

    @Test
    void updateTask_throwsWhenNoFilePath(@TempDir Path tempDir) {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec spec = TaskSpec.builder().title("No path").build();

            assertThatThrownBy(() -> service.updateTask(spec))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("no file path");
        }
    }

    @Test
    void completeTask_setsStatusToDone(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec created = service.createTask(TaskSpec.builder().title("To complete").build());
            service.completeTask(created.getId());

            TaskSpec completed = service.getSpec(created.getId());
            assertThat(completed).isNotNull();
            assertThat(completed.getStatus()).isEqualTo("Done");
            assertThat(completed.getUpdatedAt()).isNotNull();
        }
    }

    @Test
    void completeTask_throwsWhenTaskNotFound(@TempDir Path tempDir) {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            assertThatThrownBy(() -> service.completeTask("NONEXISTENT"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("Task not found");
        }
    }

    @Test
    void archiveTask_movesFileToArchiveDir(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec created = service.createTask(TaskSpec.builder().title("To archive").build());
            String originalPath = created.getFilePath();
            assertThat(Files.exists(Path.of(originalPath))).isTrue();

            service.archiveTask(created.getId());

            // Original file should be gone
            assertThat(Files.exists(Path.of(originalPath))).isFalse();
            // File should exist in archive dir
            Path archiveDir = tempDir.resolve("backlog/archive/tasks");
            assertThat(Files.list(archiveDir).count()).isGreaterThan(0);
        }
    }

    @Test
    void archiveTask_throwsWhenTaskNotFound(@TempDir Path tempDir) {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            assertThatThrownBy(() -> service.archiveTask("NONEXISTENT"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("Task not found");
        }
    }

    // ── Document write operations ──────────────────────────────────────

    @Test
    void createDocument_generatesIdAndWritesFile(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            BacklogDocument doc = service.createDocument("Test Doc", "Document content here.");

            assertThat(doc.getId()).startsWith("DOC-");
            assertThat(doc.getTitle()).isEqualTo("Test Doc");
            assertThat(doc.getContent()).isEqualTo("Document content here.");
            assertThat(doc.getFilePath()).isNotNull();
            assertThat(Files.exists(Path.of(doc.getFilePath()))).isTrue();
        }
    }

    @Test
    void createDocument_isRetrievableAfterCreation(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            BacklogDocument created = service.createDocument("Findable Doc", "Content.");

            BacklogDocument found = service.getDocument(created.getId());
            assertThat(found).isNotNull();
            assertThat(found.getTitle()).isEqualTo("Findable Doc");
        }
    }

    @Test
    void updateDocument_writesChangesToDisk(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            BacklogDocument created = service.createDocument("Original Doc", "Original content.");
            service.updateDocument(created.getId(), "Updated content.", "Updated Doc");

            BacklogDocument reloaded = service.getDocument(created.getId());
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getContent()).isEqualTo("Updated content.");
            assertThat(reloaded.getTitle()).isEqualTo("Updated Doc");
        }
    }

    @Test
    void updateDocument_keepsTitle_whenTitleIsNull(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            BacklogDocument created = service.createDocument("Keep Title", "Content.");
            service.updateDocument(created.getId(), "New content.", null);

            BacklogDocument reloaded = service.getDocument(created.getId());
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getTitle()).isEqualTo("Keep Title");
        }
    }

    @Test
    void updateDocument_keepsTitle_whenTitleIsEmpty(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            BacklogDocument created = service.createDocument("Keep Title", "Content.");
            service.updateDocument(created.getId(), "New content.", "");

            BacklogDocument reloaded = service.getDocument(created.getId());
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getTitle()).isEqualTo("Keep Title");
        }
    }

    @Test
    void updateDocument_throwsWhenNotFound(@TempDir Path tempDir) {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            assertThatThrownBy(() -> service.updateDocument("NONEXISTENT", "content", null))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("Document not found");
        }
    }

    // ── Refresh & Listeners ────────────────────────────────────────────

    @Test
    void refresh_clearsAndReloadsCache(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1);
            SpecService service = mocks.createService();
            assertThat(service.getAllSpecs()).hasSize(1);

            // Add another task file and refresh
            Path tasksDir = tempDir.resolve("backlog/tasks");
            Files.writeString(tasksDir.resolve("task-2.md"), TASK_2, StandardCharsets.UTF_8);
            service.refresh();

            assertThat(service.getAllSpecs()).hasSize(2);
        }
    }

    @Test
    void refresh_handlesNullBasePath(@TempDir Path tempDir) {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class);
             MockedStatic<LocalFileSystem> lfsMock = Mockito.mockStatic(LocalFileSystem.class)) {
            DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
            when(stateService.getSpecDirectory()).thenReturn("backlog");
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            lfsMock.when(LocalFileSystem::getInstance).thenReturn(mock(LocalFileSystem.class));

            Project project = mock(Project.class);
            when(project.getBasePath()).thenReturn(null);
            when(project.getName()).thenReturn("TestProject");
            MessageBus messageBus = mock(MessageBus.class);
            MessageBusConnection connection = mock(MessageBusConnection.class);
            when(project.getMessageBus()).thenReturn(messageBus);
            when(messageBus.connect()).thenReturn(connection);

            SpecService service = new SpecService(project);

            assertThat(service.getAllSpecs()).isEmpty();
            assertThat(service.hasSpecDirectory()).isFalse();
        }
    }

    @Test
    void addAndRemoveChangeListener(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1);
            SpecService service = mocks.createService();

            AtomicInteger callCount = new AtomicInteger(0);
            Runnable listener = callCount::incrementAndGet;

            service.addChangeListener(listener);
            service.refresh();
            assertThat(callCount.get()).isEqualTo(1);

            service.refresh();
            assertThat(callCount.get()).isEqualTo(2);

            service.removeChangeListener(listener);
            service.refresh();
            assertThat(callCount.get()).isEqualTo(2); // no longer called
        }
    }

    // ── dispose ────────────────────────────────────────────────────────

    @Test
    void dispose_clearsCachesAndListeners(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            seedTaskFiles(tempDir, TASK_1);
            seedDocFiles(tempDir, DOC_1);
            SpecService service = mocks.createService();
            service.addChangeListener(() -> {});

            assertThat(service.getAllSpecs()).isNotEmpty();
            assertThat(service.getAllDocuments()).isNotEmpty();

            service.dispose();

            assertThat(service.getAllSpecs()).isEmpty();
            assertThat(service.getAllDocuments()).isEmpty();
        }
    }

    // ── buildTaskFileName (tested indirectly via createTask) ───────────

    @Test
    void createTask_generatesCleanFileName(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec spec = TaskSpec.builder()
                    .id("TASK-1")
                    .title("My Cool Task!")
                    .build();
            TaskSpec created = service.createTask(spec);

            // File name should be lowercase id + sanitized title
            String fileName = Path.of(created.getFilePath()).getFileName().toString();
            assertThat(fileName).isEqualTo("task-1 - My-Cool-Task.md");
        }
    }

    @Test
    void createTask_handlesBlankTitle(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec spec = TaskSpec.builder()
                    .id("TASK-1")
                    .title("  ")
                    .build();
            TaskSpec created = service.createTask(spec);

            String fileName = Path.of(created.getFilePath()).getFileName().toString();
            assertThat(fileName).isEqualTo("task-1.md");
        }
    }

    @Test
    void createTask_handlesNullTitle(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec spec = TaskSpec.builder()
                    .id("TASK-1")
                    .title(null)
                    .build();
            TaskSpec created = service.createTask(spec);

            String fileName = Path.of(created.getFilePath()).getFileName().toString();
            assertThat(fileName).isEqualTo("task-1.md");
        }
    }

    // ── Task with acceptance criteria round-trip ───────────────────────

    @Test
    void createTask_withAcceptanceCriteria_roundTrips(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec spec = TaskSpec.builder()
                    .title("AC task")
                    .acceptanceCriteria(List.of(
                            AcceptanceCriterion.builder().index(1).text("First criterion").checked(false).build(),
                            AcceptanceCriterion.builder().index(2).text("Second criterion").checked(true).build()
                    ))
                    .build();
            TaskSpec created = service.createTask(spec);

            TaskSpec loaded = service.getSpec(created.getId());
            assertThat(loaded).isNotNull();
            assertThat(loaded.getAcceptanceCriteria()).hasSize(2);
        }
    }

    // ── Definition of Done defaults auto-population ─────────────────────

    @Test
    void createTask_appliesDodDefaults_whenTaskHasNoDod(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();

            // Set DoD defaults in config
            BacklogConfigService configService = BacklogConfigService.getInstance(mocks.project);
            BacklogConfig config = configService.getConfig();
            config.setDefinitionOfDone(List.of("Tests pass", "Code reviewed", "No regressions"));
            configService.saveConfig(config);

            SpecService service = mocks.createService();

            TaskSpec spec = TaskSpec.builder().title("Task with DoD defaults").build();
            TaskSpec created = service.createTask(spec);

            assertThat(created.getDefinitionOfDone()).hasSize(3);
            assertThat(created.getDefinitionOfDone().get(0).getText()).isEqualTo("Tests pass");
            assertThat(created.getDefinitionOfDone().get(1).getText()).isEqualTo("Code reviewed");
            assertThat(created.getDefinitionOfDone().get(2).getText()).isEqualTo("No regressions");
            assertThat(created.getDefinitionOfDone()).allMatch(d -> !d.isChecked());
        }
    }

    @Test
    void createTask_doesNotOverrideExistingDod(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();

            // Set DoD defaults in config
            BacklogConfigService configService = BacklogConfigService.getInstance(mocks.project);
            BacklogConfig config = configService.getConfig();
            config.setDefinitionOfDone(List.of("Default item 1", "Default item 2"));
            configService.saveConfig(config);

            SpecService service = mocks.createService();

            TaskSpec spec = TaskSpec.builder()
                    .title("Task with custom DoD")
                    .definitionOfDone(List.of(
                            DefinitionOfDoneItem.builder().index(0).text("My custom item").checked(false).build()
                    ))
                    .build();
            TaskSpec created = service.createTask(spec);

            // Should keep the task's own DoD, not apply defaults
            assertThat(created.getDefinitionOfDone()).hasSize(1);
            assertThat(created.getDefinitionOfDone().get(0).getText()).isEqualTo("My custom item");
        }
    }

    @Test
    void createTask_skipDodDefaults_doesNotApplyDefaults(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();

            // Set DoD defaults in config
            BacklogConfigService configService = BacklogConfigService.getInstance(mocks.project);
            BacklogConfig config = configService.getConfig();
            config.setDefinitionOfDone(List.of("Tests pass", "Code reviewed"));
            configService.saveConfig(config);

            SpecService service = mocks.createService();

            TaskSpec spec = TaskSpec.builder().title("Task skipping DoD defaults").build();
            TaskSpec created = service.createTask(spec, true); // skipDodDefaults = true

            assertThat(created.getDefinitionOfDone()).isEmpty();
        }
    }

    @Test
    void createTask_noDodDefaults_whenConfigHasNone(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec spec = TaskSpec.builder().title("Task with no config DoD").build();
            TaskSpec created = service.createTask(spec);

            assertThat(created.getDefinitionOfDone()).isEmpty();
        }
    }

    @Test
    void createTask_dodDefaults_persistedToFile(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();

            // Set DoD defaults in config
            BacklogConfigService configService = BacklogConfigService.getInstance(mocks.project);
            BacklogConfig config = configService.getConfig();
            config.setDefinitionOfDone(List.of("Docs updated", "Deployed to staging"));
            configService.saveConfig(config);

            SpecService service = mocks.createService();

            TaskSpec spec = TaskSpec.builder().title("Persistence check").build();
            TaskSpec created = service.createTask(spec);

            // Verify the DoD items are written to the file and can be read back
            TaskSpec reloaded = service.getSpec(created.getId());
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getDefinitionOfDone()).hasSize(2);
            assertThat(reloaded.getDefinitionOfDone().get(0).getText()).isEqualTo("Docs updated");
            assertThat(reloaded.getDefinitionOfDone().get(1).getText()).isEqualTo("Deployed to staging");
        }
    }

    // ── Bulk archive done tasks ─────────────────────────────────────────

    @Test
    void archiveDoneTasks_movesAllDoneTasksToArchive(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            // Create 3 tasks: 1 Done, 1 In Progress, 1 To Do
            TaskSpec done1 = service.createTask(TaskSpec.builder().title("Done task").status("Done").build());
            TaskSpec inProgress = service.createTask(TaskSpec.builder().title("In progress").status("In Progress").build());
            TaskSpec todo = service.createTask(TaskSpec.builder().title("Todo task").status("To Do").build());

            int archived = service.archiveDoneTasks();

            assertThat(archived).isEqualTo(1);
            assertThat(service.getAllSpecs()).hasSize(2);
            assertThat(service.getSpec(done1.getId())).isNull(); // archived
            assertThat(service.getSpec(inProgress.getId())).isNotNull();
            assertThat(service.getSpec(todo.getId())).isNotNull();
        }
    }

    @Test
    void archiveDoneTasks_archivesMultipleDoneTasks(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            service.createTask(TaskSpec.builder().title("Done 1").status("Done").build());
            service.createTask(TaskSpec.builder().title("Done 2").status("Done").build());
            service.createTask(TaskSpec.builder().title("Done 3").status("Done").build());
            service.createTask(TaskSpec.builder().title("Not done").status("To Do").build());

            int archived = service.archiveDoneTasks();

            assertThat(archived).isEqualTo(3);
            assertThat(service.getAllSpecs()).hasSize(1);
            assertThat(service.getAllSpecs().get(0).getTitle()).isEqualTo("Not done");
        }
    }

    @Test
    void archiveDoneTasks_returnsZero_whenNoDoneTasks(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            service.createTask(TaskSpec.builder().title("Todo 1").status("To Do").build());
            service.createTask(TaskSpec.builder().title("In progress 1").status("In Progress").build());

            int archived = service.archiveDoneTasks();

            assertThat(archived).isEqualTo(0);
            assertThat(service.getAllSpecs()).hasSize(2);
        }
    }

    @Test
    void archiveDoneTasks_returnsZero_whenNoTasks(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            int archived = service.archiveDoneTasks();

            assertThat(archived).isEqualTo(0);
        }
    }

    // ── Get archived tasks ───────────────────────────────────────────────

    @Test
    void getArchivedTasks_returnsTasksInArchiveDir(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            // Create and archive a task
            TaskSpec task = service.createTask(TaskSpec.builder().title("To archive").status("Done").build());
            service.archiveTask(task.getId());

            List<TaskSpec> archived = service.getArchivedTasks();

            assertThat(archived).hasSize(1);
            assertThat(archived.get(0).getId()).isEqualTo(task.getId());
            assertThat(archived.get(0).getTitle()).isEqualTo("To archive");
        }
    }

    @Test
    void getArchivedTasks_returnsEmptyList_whenNoArchivedTasks(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            service.createTask(TaskSpec.builder().title("Active task").status("To Do").build());

            List<TaskSpec> archived = service.getArchivedTasks();

            assertThat(archived).isEmpty();
        }
    }

    @Test
    void getArchivedTasks_returnsMultipleArchivedTasks(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            service.createTask(TaskSpec.builder().title("Done 1").status("Done").build());
            service.createTask(TaskSpec.builder().title("Done 2").status("Done").build());
            service.createTask(TaskSpec.builder().title("Active").status("To Do").build());

            service.archiveDoneTasks();

            List<TaskSpec> archived = service.getArchivedTasks();

            assertThat(archived).hasSize(2);
            assertThat(archived).extracting(TaskSpec::getTitle)
                    .containsExactlyInAnyOrder("Done 1", "Done 2");
        }
    }

    @Test
    void getArchivedTasks_archivedTasksNotInMainCache(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec task = service.createTask(TaskSpec.builder().title("To archive").status("Done").build());
            service.archiveTask(task.getId());

            // Main cache should not contain archived task
            assertThat(service.getSpec(task.getId())).isNull();
            assertThat(service.getAllSpecs()).isEmpty();

            // But getArchivedTasks should find it
            assertThat(service.getArchivedTasks()).hasSize(1);
        }
    }

    // ── Unarchive task ───────────────────────────────────────────────────

    @Test
    void unarchiveTask_movesTaskBackToTasksDir(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec task = service.createTask(TaskSpec.builder().title("Round trip").status("Done").build());
            service.archiveTask(task.getId());

            // Verify it's archived
            assertThat(service.getSpec(task.getId())).isNull();
            assertThat(service.getArchivedTasks()).hasSize(1);

            // Unarchive
            service.unarchiveTask(task.getId());

            // Should be back in main cache
            assertThat(service.getSpec(task.getId())).isNotNull();
            assertThat(service.getSpec(task.getId()).getTitle()).isEqualTo("Round trip");
            assertThat(service.getArchivedTasks()).isEmpty();
        }
    }

    @Test
    void unarchiveTask_throwsWhenTaskNotInArchive(@TempDir Path tempDir) {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            assertThatThrownBy(() -> service.unarchiveTask("NONEXISTENT"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("Archived task not found");
        }
    }

    @Test
    void unarchiveTask_restoresCorrectTask_whenMultipleArchived(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            mocks.initBacklog();
            SpecService service = mocks.createService();

            TaskSpec task1 = service.createTask(TaskSpec.builder().title("Task A").status("Done").build());
            TaskSpec task2 = service.createTask(TaskSpec.builder().title("Task B").status("Done").build());
            service.archiveDoneTasks();

            assertThat(service.getArchivedTasks()).hasSize(2);

            // Unarchive only task1
            service.unarchiveTask(task1.getId());

            assertThat(service.getSpec(task1.getId())).isNotNull();
            assertThat(service.getSpec(task2.getId())).isNull();
            assertThat(service.getArchivedTasks()).hasSize(1);
            assertThat(service.getArchivedTasks().get(0).getId()).isEqualTo(task2.getId());
        }
    }

    // ── Spec files without id are skipped ──────────────────────────────

    @Test
    void refresh_ignoresSpecWithoutId(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            Path tasksDir = tempDir.resolve("backlog/tasks");
            Files.createDirectories(tasksDir);
            Files.writeString(tasksDir.resolve("noid.md"), """
                    ---
                    title: No ID task
                    status: To Do
                    ---

                    No id field.
                    """, StandardCharsets.UTF_8);

            SpecService service = mocks.createService();

            assertThat(service.getAllSpecs()).isEmpty();
        }
    }

    @Test
    void refresh_ignoresSpecWithoutFrontmatter(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            Path tasksDir = tempDir.resolve("backlog/tasks");
            Files.createDirectories(tasksDir);
            Files.writeString(tasksDir.resolve("plain.md"), "Just plain markdown.", StandardCharsets.UTF_8);

            SpecService service = mocks.createService();

            assertThat(service.getAllSpecs()).isEmpty();
        }
    }

    // ── Root-level md scanning (backward compat) ───────────────────────

    @Test
    void refresh_scansRootLevelMdFiles(@TempDir Path tempDir) throws IOException {
        try (var mocks = new MockContext(tempDir)) {
            Path backlogDir = tempDir.resolve("backlog");
            Files.createDirectories(backlogDir);
            Files.writeString(backlogDir.resolve("root-task.md"), TASK_1, StandardCharsets.UTF_8);

            SpecService service = mocks.createService();

            assertThat(service.getSpec("TASK-1")).isNotNull();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void seedTaskFiles(Path tempDir, String... tasks) throws IOException {
        Path tasksDir = tempDir.resolve("backlog/tasks");
        Files.createDirectories(tasksDir);
        for (int i = 0; i < tasks.length; i++) {
            Files.writeString(tasksDir.resolve("task-" + (i + 1) + ".md"), tasks[i], StandardCharsets.UTF_8);
        }
    }

    private void seedDocFiles(Path tempDir, String... docs) throws IOException {
        Path docsDir = tempDir.resolve("backlog/docs");
        Files.createDirectories(docsDir);
        for (int i = 0; i < docs.length; i++) {
            Files.writeString(docsDir.resolve("doc-" + (i + 1) + ".md"), docs[i], StandardCharsets.UTF_8);
        }
    }

    /**
     * Manages mocked statics for DevoxxGenieStateService and LocalFileSystem,
     * plus a properly configured Project mock with MessageBus.
     */
    private static class MockContext implements AutoCloseable {
        final MockedStatic<DevoxxGenieStateService> stateMock;
        final MockedStatic<LocalFileSystem> lfsMock;
        final DevoxxGenieStateService stateService;
        final Project project;
        final Path tempDir;

        MockContext(Path tempDir) {
            this.tempDir = tempDir;
            stateMock = Mockito.mockStatic(DevoxxGenieStateService.class);
            lfsMock = Mockito.mockStatic(LocalFileSystem.class);

            stateService = mock(DevoxxGenieStateService.class);
            when(stateService.getSpecDirectory()).thenReturn("backlog");
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

            LocalFileSystem lfs = mock(LocalFileSystem.class);
            lfsMock.when(LocalFileSystem::getInstance).thenReturn(lfs);

            project = mock(Project.class);
            when(project.getBasePath()).thenReturn(tempDir.toString());
            when(project.getName()).thenReturn("TestProject");

            MessageBus messageBus = mock(MessageBus.class);
            MessageBusConnection connection = mock(MessageBusConnection.class);
            when(project.getMessageBus()).thenReturn(messageBus);
            when(messageBus.connect()).thenReturn(connection);

            // Wire up BacklogConfigService for write operations
            BacklogConfigService configService = new BacklogConfigService(project);
            when(project.getService(BacklogConfigService.class)).thenReturn(configService);
        }

        SpecService createService() {
            return new SpecService(project);
        }

        void initBacklog() {
            try {
                BacklogConfigService configService = project.getService(BacklogConfigService.class);
                configService.initBacklog("TestProject");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
            lfsMock.close();
            stateMock.close();
        }
    }
}
