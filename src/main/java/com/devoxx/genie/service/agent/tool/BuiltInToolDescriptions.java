package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for the LLM-facing descriptions of the built-in agent tools, and the
 * place where a user's per-tool description override is resolved.
 *
 * <p>These are the strings actually sent to the model in the {@code ToolSpecification} — not the
 * short labels shown next to the settings checkboxes ({@code AgentSettingsComponent.CORE_AGENT_TOOLS},
 * {@link com.devoxx.genie.service.agent.tool.psi.PsiToolCatalog}). Users can rewrite them from
 * Settings → DevoxxGenie → Agent to steer the agent, for example disabling {@code edit_file} and
 * telling the model to perform all edits through {@code run_command}.
 *
 * <p>Overrides are stored by tool name in {@code DevoxxGenieStateService.toolDescriptionOverrides}
 * and resolved by {@link #effective(String)} at spec-build time. Because a fresh
 * {@code BuiltInToolProvider} is created per prompt, an edited description takes effect on the
 * next prompt without an IDE restart.
 *
 * <p>Scope is deliberately limited to the tools registered by {@link BuiltInToolProvider} and
 * {@link ReadOnlyToolProvider}. MCP and Skills tool descriptions are owned by their servers and
 * are never rewritten here; an override stored under an unknown name is simply ignored.
 */
public final class BuiltInToolDescriptions {

    private static final Map<String, String> DEFAULTS = buildDefaults();

    private BuiltInToolDescriptions() {
    }

    /** Tool names that support a user description override, in settings-panel order. */
    public static @NotNull Set<String> toolNames() {
        return DEFAULTS.keySet();
    }

    /** The shipped description for {@code name}, or {@code null} if the tool is not overridable. */
    public static @Nullable String defaultOf(@NotNull String name) {
        return DEFAULTS.get(name);
    }

    /**
     * The description to send to the LLM: the user's override when one is set and non-blank,
     * otherwise the shipped default. Falls back to the default whenever settings are unavailable
     * (headless unit tests, early startup) so tool registration can never fail because of this.
     */
    public static @Nullable String effective(@NotNull String name) {
        String override = overrideOf(name);
        return override != null ? override : DEFAULTS.get(name);
    }

    /** {@code true} when the user has stored a non-blank override for {@code name}. */
    public static boolean isOverridden(@NotNull String name) {
        return overrideOf(name) != null;
    }

    /** The stored override for {@code name}, trimmed, or {@code null} when absent/blank/unknown. */
    public static @Nullable String overrideOf(@NotNull String name) {
        if (!DEFAULTS.containsKey(name)) {
            // Never let an override for an MCP/Skills tool — or for a built-in tool that has since
            // been renamed or removed — leak into a tool specification.
            return null;
        }
        Map<String, String> overrides = storedOverrides();
        String override = overrides.get(name);
        return override != null && !override.isBlank() ? override.trim() : null;
    }

    /**
     * The persisted override map, never {@code null}. Settings access is guarded because both
     * providers build their specs from this class and some of them run in plain (non-IDE) unit
     * tests where {@code ApplicationManager} has no application.
     */
    private static @NotNull Map<String, String> storedOverrides() {
        try {
            DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
            Map<String, String> overrides = state != null ? state.getToolDescriptionOverrides() : null;
            return overrides != null ? overrides : Collections.emptyMap();
        } catch (Exception | LinkageError e) {
            return Collections.emptyMap();
        }
    }

    private static @NotNull Map<String, String> buildDefaults() {
        Map<String, String> map = new LinkedHashMap<>();

        // --- Core file/shell/web tools ---
        map.put("read_file",
                "Read the contents of a file in the project");
        map.put("write_file",
                "Write content to a file in the project. Creates the file and parent directories if they don't exist.");
        map.put("edit_file",
                "Edit a file by replacing an exact string match with new content. " +
                        "The file must already exist. If the old_string appears multiple times, " +
                        "either provide more context to make it unique, or set replace_all to true.");
        map.put("list_files",
                "List files and directories in the project. Skips common build/VCS directories.");
        map.put("search_files",
                "Search for a regex pattern in project files. Returns matching lines with file paths and line numbers.");
        map.put("run_command",
                "Execute a terminal command in the project directory. Has a 30-second timeout.");
        map.put("fetch_page",
                "Fetch a web page by URL and return its readable text content. " +
                        "HTML tags, CSS, and JavaScript are stripped. " +
                        "Useful for reading documentation, API references, and web pages. " +
                        "Large pages are truncated to 100K characters.");

        // --- Optional feature tools ---
        map.put("run_tests",
                "Run tests in the project. Auto-detects build system (Gradle/Maven/npm/etc.) " +
                        "and executes the appropriate test command. Returns structured results with pass/fail counts. " +
                        "Use this after modifying code to verify changes don't break existing tests. " +
                        "Has a configurable timeout (default 5 minutes).");
        map.put("parallel_explore",
                "Launch multiple sub-agents in parallel to explore different aspects " +
                        "of the codebase simultaneously. Each sub-agent has its own model and read-only " +
                        "tool access (read_file, list_files, search_files). Use this for broad exploration " +
                        "tasks that benefit from investigating multiple angles at once. " +
                        "Provide 2-5 focused exploration queries.");
        map.put("semantic_search",
                "Search the project's semantic (vector) index for content conceptually similar to a query. " +
                        "USE THIS TOOL FIRST for any question about what the project content discusses, mentions, " +
                        "covers, or explains — for example: 'which slides discuss MCP', 'where do we explain RAG', " +
                        "'find anything about authentication', 'what files describe the build process'. " +
                        "Returns ranked file paths, similarity scores, and matching content snippets. " +
                        "Only fall back to `search_files` (regex grep) when you need to locate a known exact string, " +
                        "or when this tool returns no useful hits.");
        map.put("web_search",
                "Search the web for current information, documentation, news, or any topic. " +
                        "Returns ranked results with titles, URLs, and content snippets. " +
                        "Use when the answer requires information beyond the project codebase — " +
                        "e.g. library docs, API references, recent releases, or general knowledge. " +
                        "Requires a Tavily or Google Custom Search API key configured in " +
                        "Settings → DevoxxGenie → Web search.");

        // --- PSI (code intelligence) tools ---
        map.put("find_symbols",
                "Search for symbol definitions (classes, methods, fields) by name in the project. " +
                        "Unlike text search, this uses the IDE's semantic index and only returns actual declarations, not usages. " +
                        "Works across all languages supported by the IDE (Java, Kotlin, Python, JS/TS, Go, etc.).");
        map.put("document_symbols",
                "List all symbol definitions in a file with their kind (class, method, field) " +
                        "and line numbers. Shows the nesting structure (e.g. methods inside classes). " +
                        "Useful for understanding file structure before reading specific sections.");
        map.put("find_references",
                "Find all references (usages) of a symbol defined at a given file and line. " +
                        "Uses the IDE's semantic reference search, which is more accurate than text search " +
                        "because it understands imports, qualified names, and language semantics.");
        map.put("find_definition",
                "Navigate from a symbol usage to its definition. Given a file position where " +
                        "a symbol is used, resolves and returns the location where it is defined. " +
                        "Understands imports, inheritance, and cross-file references.");
        map.put("find_implementations",
                "Find all implementations of an interface, abstract class, or abstract method. " +
                        "Useful for understanding the type hierarchy and finding concrete implementations.");
        map.put("find_callees",
                "List the methods that a given method calls (outgoing edges). " +
                        "This is the inverse of find_references: find_references answers 'who calls X', " +
                        "find_callees answers 'what does X call'. Each call target is resolved through the " +
                        "IDE's semantic index, so overloads, inheritance, and imports are understood — more " +
                        "accurate than grepping the method body with search_files. " +
                        "Currently supports Java; other languages return a clear 'not supported' message.");
        map.put("trace_call_chains",
                "Trace call chains from a start method, walking caller→callee or callee→caller " +
                        "edges up to a bounded depth, and return the path(s). Use this to answer 'how does " +
                        "execution reach X' or 'what chain of calls does X trigger' — questions that need the " +
                        "path between two methods, which a single find_references/find_callees call cannot give. " +
                        "Provide an optional 'target' to stop as soon as a named method is reached. " +
                        "Bounded in depth (default 5, hard max 10) and number of paths. Java only in v1.");
        map.put("calculate_complexity",
                "Compute cyclomatic (McCabe) complexity for Java methods by counting decision " +
                        "points (if/for/while/case/catch/&&/||/ternary). With 'line' it scores a single " +
                        "method; without it, scores every method in the file and flags those over a " +
                        "threshold. Use this to find the riskiest methods to refactor or test — something " +
                        "search_files cannot measure. Java only in v1.");
        map.put("find_dead_code",
                "Report symbols in a file with zero project-scope references — HEURISTIC " +
                        "dead-code CANDIDATES, not certainties. Results require human confirmation before " +
                        "deletion: reflection, serialization, dependency injection, and out-of-project " +
                        "callers can reference a symbol invisibly. Conservatively excludes public members, " +
                        "constructors/main, @Override and any annotated member, and serialization members " +
                        "to keep false positives low. Java only in v1.");

        return Collections.unmodifiableMap(map);
    }
}
