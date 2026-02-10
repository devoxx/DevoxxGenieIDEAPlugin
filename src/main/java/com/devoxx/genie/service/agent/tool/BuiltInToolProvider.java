package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides built-in IDE tools for agentic interactions:
 * read_file, write_file, edit_file, list_files, search_files, run_command, run_tests, parallel_explore.
 */
public class BuiltInToolProvider implements ToolProvider {

    private final Map<ToolSpecification, ToolExecutor> tools;
    private @Nullable ParallelExploreToolExecutor parallelExploreExecutor;

    public BuiltInToolProvider(@NotNull Project project) {
        tools = new LinkedHashMap<>();

        // read_file
        tools.put(
                ToolSpecification.builder()
                        .name("read_file")
                        .description("Read the contents of a file in the project")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("path", "File path relative to project root")
                                .required("path")
                                .build())
                        .build(),
                new ReadFileToolExecutor(project)
        );

        // write_file
        tools.put(
                ToolSpecification.builder()
                        .name("write_file")
                        .description("Write content to a file in the project. Creates the file and parent directories if they don't exist.")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("path", "File path relative to project root")
                                .addStringProperty("content", "The content to write to the file")
                                .required("path", "content")
                                .build())
                        .build(),
                new WriteFileToolExecutor(project)
        );

        // edit_file
        tools.put(
                ToolSpecification.builder()
                        .name("edit_file")
                        .description("Edit a file by replacing an exact string match with new content. " +
                                "The file must already exist. If the old_string appears multiple times, " +
                                "either provide more context to make it unique, or set replace_all to true.")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("path", "File path relative to project root")
                                .addStringProperty("old_string", "The exact text to find in the file")
                                .addStringProperty("new_string", "The replacement text")
                                .addBooleanProperty("replace_all",
                                        "Whether to replace all occurrences (default: false)")
                                .required("path", "old_string", "new_string")
                                .build())
                        .build(),
                new EditFileToolExecutor(project)
        );

        // list_files
        tools.put(
                ToolSpecification.builder()
                        .name("list_files")
                        .description("List files and directories in the project. Skips common build/VCS directories.")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("path", "Directory path relative to project root (defaults to root)")
                                .addBooleanProperty("recursive", "Whether to list files recursively (default: false)")
                                .build())
                        .build(),
                new ListFilesToolExecutor(project)
        );

        // search_files
        tools.put(
                ToolSpecification.builder()
                        .name("search_files")
                        .description("Search for a regex pattern in project files. Returns matching lines with file paths and line numbers.")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("pattern", "Regex pattern to search for")
                                .addStringProperty("path", "Directory path to search in (defaults to project root)")
                                .addStringProperty("file_pattern", "Glob pattern to filter files (e.g. '*.java')")
                                .required("pattern")
                                .build())
                        .build(),
                new SearchFilesToolExecutor(project)
        );

        // run_command
        tools.put(
                ToolSpecification.builder()
                        .name("run_command")
                        .description("Execute a terminal command in the project directory. Has a 30-second timeout.")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("command", "The command to execute")
                                .addStringProperty("working_dir", "Working directory relative to project root (defaults to project root)")
                                .required("command")
                                .build())
                        .build(),
                new RunCommandToolExecutor(project)
        );

        // run_tests — only when test execution is enabled
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getTestExecutionEnabled())) {
            tools.put(
                    ToolSpecification.builder()
                            .name("run_tests")
                            .description("Run tests in the project. Auto-detects build system (Gradle/Maven/npm/etc.) " +
                                    "and executes the appropriate test command. Returns structured results with pass/fail counts. " +
                                    "Use this after modifying code to verify changes don't break existing tests. " +
                                    "Has a configurable timeout (default 5 minutes).")
                            .parameters(JsonObjectSchema.builder()
                                    .addStringProperty("test_target",
                                            "Specific test class, method, or pattern to run (optional). " +
                                            "Gradle: 'com.example.MyTest' or 'MyTest.testMethod'. " +
                                            "Maven: 'MyTest' or 'MyTest#testMethod'. If omitted, runs all tests.")
                                    .addStringProperty("working_dir",
                                            "Working directory relative to project root (defaults to project root)")
                                    .build())
                            .build(),
                    new RunTestsToolExecutor(project)
            );
        }

        // Backlog tools — only when SDD is enabled
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getSpecBrowserEnabled())) {
            registerBacklogTools(project);
        }

        // parallel_explore — only when enabled in settings
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getParallelExploreEnabled())) {
            parallelExploreExecutor = new ParallelExploreToolExecutor(project);
            tools.put(
                    ToolSpecification.builder()
                            .name("parallel_explore")
                            .description("Launch multiple sub-agents in parallel to explore different aspects " +
                                    "of the codebase simultaneously. Each sub-agent has its own model and read-only " +
                                    "tool access (read_file, list_files, search_files). Use this for broad exploration " +
                                    "tasks that benefit from investigating multiple angles at once. " +
                                    "Provide 2-5 focused exploration queries.")
                            .parameters(JsonObjectSchema.builder()
                                    .addProperty("queries", JsonArraySchema.builder()
                                            .items(JsonStringSchema.builder()
                                                    .description("An exploration query for a sub-agent")
                                                    .build())
                                            .description("List of exploration queries, one per sub-agent (2-5 queries)")
                                            .build())
                                    .required("queries")
                                    .build())
                            .build(),
                    parallelExploreExecutor
            );
        }
    }

    private void registerBacklogTools(@NotNull Project project) {
        BacklogTaskToolExecutor taskExecutor = new BacklogTaskToolExecutor(project);
        BacklogDocumentToolExecutor documentExecutor = new BacklogDocumentToolExecutor(project);
        BacklogMilestoneToolExecutor milestoneExecutor = new BacklogMilestoneToolExecutor(project);

        // Task tools (8)
        tools.put(BacklogToolSpecifications.taskCreate(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskList(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskSearch(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskView(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskEdit(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskComplete(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskArchive(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskFindRelated(), taskExecutor);

        // Document tools (5)
        tools.put(BacklogToolSpecifications.documentList(), documentExecutor);
        tools.put(BacklogToolSpecifications.documentView(), documentExecutor);
        tools.put(BacklogToolSpecifications.documentCreate(), documentExecutor);
        tools.put(BacklogToolSpecifications.documentUpdate(), documentExecutor);
        tools.put(BacklogToolSpecifications.documentSearch(), documentExecutor);

        // Milestone tools (5)
        tools.put(BacklogToolSpecifications.milestoneList(), milestoneExecutor);
        tools.put(BacklogToolSpecifications.milestoneAdd(), milestoneExecutor);
        tools.put(BacklogToolSpecifications.milestoneRename(), milestoneExecutor);
        tools.put(BacklogToolSpecifications.milestoneRemove(), milestoneExecutor);
        tools.put(BacklogToolSpecifications.milestoneArchive(), milestoneExecutor);
    }

    /**
     * Returns the ParallelExploreToolExecutor if it was created (i.e. parallel explore is enabled).
     * Used by {@link com.devoxx.genie.service.agent.AgentToolProviderFactory} to register
     * it as a cancellable child of the AgentLoopTracker.
     */
    @Nullable
    public ParallelExploreToolExecutor getParallelExploreExecutor() {
        return parallelExploreExecutor;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ToolProviderResult.Builder builder = ToolProviderResult.builder();
        for (Map.Entry<ToolSpecification, ToolExecutor> entry : tools.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }
}
