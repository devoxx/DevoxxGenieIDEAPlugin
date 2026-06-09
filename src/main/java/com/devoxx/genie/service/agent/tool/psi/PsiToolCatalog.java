package com.devoxx.genie.service.agent.tool.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Single source of truth for the set of PSI (Program Structure Interface) agent tools and
 * their short, human-facing labels.
 *
 * <p>The Agent settings UI builds one fine-grained enable/disable checkbox per entry here,
 * and {@code BuiltInToolProvider} registers a tool for each of these names. A regression
 * test asserts the two stay in sync, so adding a new PSI tool to the provider without
 * listing it here (leaving it without a settings toggle) fails the build.
 *
 * <p>The descriptions are deliberately short — they are UI labels, not the detailed
 * LLM-facing tool descriptions, which live in {@code BuiltInToolProvider}.
 */
public final class PsiToolCatalog {

    /** A PSI tool's stable name and the short description shown next to its settings checkbox. */
    public record PsiTool(@NotNull String name, @NotNull String description) {
    }

    public static final List<PsiTool> TOOLS = List.of(
            new PsiTool("find_symbols", "Search for symbol definitions by name"),
            new PsiTool("document_symbols", "List the symbol structure of a file"),
            new PsiTool("find_references", "Find all usages of a symbol"),
            new PsiTool("find_definition", "Navigate from a usage to its definition"),
            new PsiTool("find_implementations", "Find implementations of an interface/abstract class"),
            new PsiTool("find_callees", "List the methods a given method calls"),
            new PsiTool("trace_call_chains", "Trace caller/callee call chains between methods"),
            new PsiTool("calculate_complexity", "Compute cyclomatic complexity of Java methods"),
            new PsiTool("find_dead_code", "Report unreferenced symbols (heuristic candidates)")
    );

    public static @NotNull List<String> toolNames() {
        return TOOLS.stream().map(PsiTool::name).toList();
    }

    private PsiToolCatalog() {
    }
}
