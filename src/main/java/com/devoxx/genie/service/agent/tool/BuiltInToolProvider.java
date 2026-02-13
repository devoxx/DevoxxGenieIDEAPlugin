package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.service.agent.tool.psi.*;
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

import java.util.*;

/**
 * Provides built-in IDE tools for agentic interactions:
 * read_file, write_file, edit_file, list_files, search_files, run_command, fetch_page, run_tests, parallel_explore.
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

        // fetch_page
        tools.put(
                ToolSpecification.builder()
                        .name("fetch_page")
                        .description("Fetch a web page by URL and return its readable text content. " +
                                "HTML tags, CSS, and JavaScript are stripped. " +
                                "Useful for reading documentation, API references, and web pages. " +
                                "Large pages are truncated to 100K characters.")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("url", "The URL to fetch (must start with http:// or https://)")
                                .required("url")
                                .build())
                        .build(),
                new FetchPageToolExecutor()
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

        // PSI (Program Structure Interface) tools — only when enabled in settings
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getPsiToolsEnabled())) {
            registerPsiTools(project);
        }
    }

    private void registerPsiTools(@NotNull Project project) {
        // find_symbols — search for symbol definitions by name
        tools.put(
                ToolSpecification.builder()
                        .name("find_symbols")
                        .description("Search for symbol definitions (classes, methods, fields) by name in the project. " +
                                "Unlike text search, this uses the IDE's semantic index and only returns actual declarations, not usages. " +
                                "Works across all languages supported by the IDE (Java, Kotlin, Python, JS/TS, Go, etc.).")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("name", "Exact symbol name to search for (e.g. 'ChatService', 'executeQuery')")
                                .addStringProperty("kind", "Optional filter: 'class', 'method', or 'field'")
                                .required("name")
                                .build())
                        .build(),
                new FindSymbolsToolExecutor(project)
        );

        // document_symbols — list all symbols defined in a file
        tools.put(
                ToolSpecification.builder()
                        .name("document_symbols")
                        .description("List all symbol definitions in a file with their kind (class, method, field) " +
                                "and line numbers. Shows the nesting structure (e.g. methods inside classes). " +
                                "Useful for understanding file structure before reading specific sections.")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("file", "File path relative to project root")
                                .required("file")
                                .build())
                        .build(),
                new DocumentSymbolsToolExecutor(project)
        );

        // find_references — find all usages of a symbol
        tools.put(
                ToolSpecification.builder()
                        .name("find_references")
                        .description("Find all references (usages) of a symbol defined at a given file and line. " +
                                "Uses the IDE's semantic reference search, which is more accurate than text search " +
                                "because it understands imports, qualified names, and language semantics.")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("file", "File path relative to project root where the symbol is defined")
                                .addIntegerProperty("line", "1-based line number where the symbol is defined")
                                .addStringProperty("symbol", "Optional: symbol name to disambiguate if multiple definitions are on the same line")
                                .required("file", "line")
                                .build())
                        .build(),
                new FindReferencesToolExecutor(project)
        );

        // find_definition — go to the definition of a symbol
        tools.put(
                ToolSpecification.builder()
                        .name("find_definition")
                        .description("Navigate from a symbol usage to its definition. Given a file position where " +
                                "a symbol is used, resolves and returns the location where it is defined. " +
                                "Understands imports, inheritance, and cross-file references.")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("file", "File path relative to project root containing the symbol usage")
                                .addIntegerProperty("line", "1-based line number of the symbol usage")
                                .addIntegerProperty("column", "Optional: 1-based column for precise positioning")
                                .addStringProperty("symbol", "Optional: symbol name to look for on the line")
                                .required("file", "line")
                                .build())
                        .build(),
                new FindDefinitionToolExecutor(project)
        );

        // find_implementations — find implementations of an interface/abstract class/method
        tools.put(
                ToolSpecification.builder()
                        .name("find_implementations")
                        .description("Find all implementations of an interface, abstract class, or abstract method. " +
                                "Useful for understanding the type hierarchy and finding concrete implementations.")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("file", "File path relative to project root where the interface/class is defined")
                                .addIntegerProperty("line", "1-based line number of the interface/class/method definition")
                                .addStringProperty("symbol", "Optional: symbol name to disambiguate if multiple definitions are on the same line")
                                .required("file", "line")
                                .build())
                        .build(),
                new FindImplementationsToolExecutor(project)
        );
    }

    private void registerBacklogTools(@NotNull Project project) {
        BacklogTaskToolExecutor taskExecutor = new BacklogTaskToolExecutor(project);
        BacklogDocumentToolExecutor documentExecutor = new BacklogDocumentToolExecutor(project);
        BacklogMilestoneToolExecutor milestoneExecutor = new BacklogMilestoneToolExecutor(project);

        // Task tools (7)
        tools.put(BacklogToolSpecifications.taskCreate(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskList(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskSearch(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskView(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskEdit(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskComplete(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskArchive(), taskExecutor);

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
        List<String> disabledTools = DevoxxGenieStateService.getInstance().getDisabledAgentTools();
        Set<String> disabledSet = disabledTools != null ? new HashSet<>(disabledTools) : Collections.emptySet();

        ToolProviderResult.Builder builder = ToolProviderResult.builder();
        for (Map.Entry<ToolSpecification, ToolExecutor> entry : tools.entrySet()) {
            if (!disabledSet.contains(entry.getKey().name())) {
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }
}
