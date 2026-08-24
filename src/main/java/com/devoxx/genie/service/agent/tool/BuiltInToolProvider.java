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
 *
 * <p>The LLM-facing tool descriptions live in {@link BuiltInToolDescriptions}, which resolves the
 * user's per-tool override (Settings → DevoxxGenie → Agent) before falling back to the shipped
 * text. A fresh provider is built per prompt, so an edited description applies to the next prompt.
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
                        .description(BuiltInToolDescriptions.effective("read_file"))
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
                        .description(BuiltInToolDescriptions.effective("write_file"))
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
                        .description(BuiltInToolDescriptions.effective("edit_file"))
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
                        .description(BuiltInToolDescriptions.effective("list_files"))
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
                        .description(BuiltInToolDescriptions.effective("search_files"))
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
                        .description(BuiltInToolDescriptions.effective("run_command"))
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
                        .description(BuiltInToolDescriptions.effective("fetch_page"))
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
                            .description(BuiltInToolDescriptions.effective("run_tests"))
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

        // Security scan tools — only when security scanning is enabled
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getSecurityScanEnabled())) {
            DevoxxGenieStateService secState = DevoxxGenieStateService.getInstance();
            SecurityScanToolExecutor secExecutor = new SecurityScanToolExecutor(project);
            tools.put(SecurityScanToolSpecification.securityScan(), secExecutor);
            if (Boolean.TRUE.equals(secState.getGitleaksScanToolEnabled())) {
                tools.put(SecurityScanToolSpecification.gitleaksScan(), secExecutor);
            }
            if (Boolean.TRUE.equals(secState.getOpengrepScanToolEnabled())) {
                tools.put(SecurityScanToolSpecification.opengrepScan(), secExecutor);
            }
            if (Boolean.TRUE.equals(secState.getTrivyScanToolEnabled())) {
                tools.put(SecurityScanToolSpecification.trivyScan(), secExecutor);
            }
        }

        // parallel_explore — only when enabled in settings
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getParallelExploreEnabled())) {
            parallelExploreExecutor = new ParallelExploreToolExecutor(project);
            tools.put(
                    ToolSpecification.builder()
                            .name("parallel_explore")
                            .description(BuiltInToolDescriptions.effective("parallel_explore"))
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

        // semantic_search — only when RAG is enabled in settings. (task-222 collapsed the
        // separate per-session "ragActivated" toggle; ragEnabled is now the single source
        // of truth.) Lets the LLM choose vector retrieval for conceptual queries instead of
        // falling back to lexical search_files. When this tool is registered,
        // MessageCreationService also stops injecting <SemanticContext> passively.
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getRagEnabled())) {
            tools.put(
                    ToolSpecification.builder()
                            .name("semantic_search")
                            .description(BuiltInToolDescriptions.effective("semantic_search"))
                            .parameters(JsonObjectSchema.builder()
                                    .addStringProperty("query",
                                            "Natural-language query describing the concept or topic to retrieve. " +
                                                    "Do NOT pass a regex — this is semantic similarity, not text matching.")
                                    .required("query")
                                    .build())
                            .build(),
                    new SemanticSearchToolExecutor(project)
            );
        }

        // web_search — only when web search agent tool is enabled in settings
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getWebSearchAgentToolEnabled())) {
            tools.put(
                    ToolSpecification.builder()
                            .name("web_search")
                            .description(BuiltInToolDescriptions.effective("web_search"))
                            .parameters(JsonObjectSchema.builder()
                                    .addStringProperty("query",
                                            "Search query as a natural-language question or keyword phrase.")
                                    .required("query")
                                    .build())
                            .build(),
                    new WebSearchToolExecutor()
            );
        }
    }

    private void registerPsiTools(@NotNull Project project) {
        // find_symbols — search for symbol definitions by name
        tools.put(
                ToolSpecification.builder()
                        .name("find_symbols")
                        .description(BuiltInToolDescriptions.effective("find_symbols"))
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
                        .description(BuiltInToolDescriptions.effective("document_symbols"))
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
                        .description(BuiltInToolDescriptions.effective("find_references"))
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
                        .description(BuiltInToolDescriptions.effective("find_definition"))
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
                        .description(BuiltInToolDescriptions.effective("find_implementations"))
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("file", "File path relative to project root where the interface/class is defined")
                                .addIntegerProperty("line", "1-based line number of the interface/class/method definition")
                                .addStringProperty("symbol", "Optional: symbol name to disambiguate if multiple definitions are on the same line")
                                .required("file", "line")
                                .build())
                        .build(),
                new FindImplementationsToolExecutor(project)
        );

        // The remaining tools are Java-only: their executor classes reference Java-plugin PSI
        // types (PsiMethod, PsiModifierListOwner, …) in method signatures, so merely linking
        // them throws NoClassDefFoundError in IDEs without the Java plugin (PyCharm, WebStorm,
        // GoLand, …) — see issue #1100. The isJavaAvailable() checks inside their execute()
        // methods cannot help because the class never loads. Skip registration entirely.
        if (!PsiToolUtils.isJavaAvailable()) {
            return;
        }

        // find_callees — outgoing calls from a method (inverse of find_references)
        tools.put(
                ToolSpecification.builder()
                        .name("find_callees")
                        .description(BuiltInToolDescriptions.effective("find_callees"))
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("file", "File path relative to project root where the method is defined")
                                .addIntegerProperty("line", "1-based line number where the method is declared")
                                .addStringProperty("symbol", "Optional: method name to disambiguate if multiple definitions are on the same line")
                                .required("file", "line")
                                .build())
                        .build(),
                new FindCalleesToolExecutor(project)
        );

        // trace_call_chains — bounded traversal of caller/callee edges
        tools.put(
                ToolSpecification.builder()
                        .name("trace_call_chains")
                        .description(BuiltInToolDescriptions.effective("trace_call_chains"))
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("file", "File path relative to project root where the start method is defined")
                                .addIntegerProperty("line", "1-based line number where the start method is declared")
                                .addStringProperty("symbol", "Optional: method name to disambiguate if multiple definitions are on the same line")
                                .addStringProperty("direction", "'callers' (who reaches this method, default) or 'callees' (what this method reaches)")
                                .addStringProperty("target", "Optional: stop and return the chain when a method with this name is reached")
                                .addIntegerProperty("depth", "Maximum chain depth (default 5, hard max 10)")
                                .required("file", "line")
                                .build())
                        .build(),
                new TraceCallChainsToolExecutor(project)
        );

        // calculate_complexity — cyclomatic complexity per method
        tools.put(
                ToolSpecification.builder()
                        .name("calculate_complexity")
                        .description(BuiltInToolDescriptions.effective("calculate_complexity"))
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("file", "File path relative to project root")
                                .addIntegerProperty("line", "Optional: 1-based line of a single method to score; omit to score the whole file")
                                .addIntegerProperty("threshold", "Optional: flag methods whose complexity exceeds this (default 10)")
                                .required("file")
                                .build())
                        .build(),
                new CalculateComplexityToolExecutor(project)
        );

        // find_dead_code — zero-reference symbols (heuristic candidates)
        tools.put(
                ToolSpecification.builder()
                        .name("find_dead_code")
                        .description(BuiltInToolDescriptions.effective("find_dead_code"))
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("file", "File path relative to project root to scan for unreferenced symbols")
                                .required("file")
                                .build())
                        .build(),
                new FindDeadCodeToolExecutor(project)
        );
    }

    private void registerBacklogTools(@NotNull Project project) {
        BacklogTaskToolExecutor taskExecutor = new BacklogTaskToolExecutor(project);
        BacklogDocumentToolExecutor documentExecutor = new BacklogDocumentToolExecutor(project);
        BacklogMilestoneToolExecutor milestoneExecutor = new BacklogMilestoneToolExecutor(project);

        // Task tools (10)
        tools.put(BacklogToolSpecifications.taskCreate(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskList(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskSearch(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskView(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskEdit(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskComplete(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskArchive(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskArchiveDone(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskUnarchive(), taskExecutor);
        tools.put(BacklogToolSpecifications.taskListArchived(), taskExecutor);

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
