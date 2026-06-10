package com.devoxx.genie.service.agent.tool.psi;

import com.devoxx.genie.service.agent.tool.ToolArgumentParser;
import com.devoxx.genie.util.ReadAccess;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * PSI-based tool that traces call chains from a start method, walking either
 * caller→callee or callee→caller edges up to a bounded depth, and returns the
 * paths found. Optionally stops as soon as an optional target symbol is reached.
 *
 * <p>Rather than the UI-bound {@code com.intellij.ide.hierarchy.call} APIs (which
 * expect a browser/descriptor context and are fragile headlessly inside a
 * {@code ReadAction}), this walks the same PSI primitives the other PSI tools use:
 * {@code ReferencesSearch} for the caller direction and method-body call resolution
 * for the callee direction. Java only in v1.
 *
 * <p>Bounded in every dimension to stay responsive and avoid context blow-up:
 * depth (default 5, hard max 10), number of paths ({@link #MAX_PATHS}), and total
 * nodes expanded ({@link #NODE_BUDGET}).
 */
@Slf4j
public class TraceCallChainsToolExecutor implements ToolExecutor {

    private static final int DEFAULT_DEPTH = 5;
    private static final int MAX_DEPTH = 10;
    private static final int MAX_PATHS = 20;
    private static final int NODE_BUDGET = 500;

    private final Project project;

    public TraceCallChainsToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String file = ToolArgumentParser.getString(request.arguments(), "file");
            int line = ToolArgumentParser.getInt(request.arguments(), "line", -1);
            String symbol = ToolArgumentParser.getString(request.arguments(), "symbol");
            String target = ToolArgumentParser.getString(request.arguments(), "target");
            String direction = ToolArgumentParser.getString(request.arguments(), "direction");
            int depth = ToolArgumentParser.getInt(request.arguments(), "depth", DEFAULT_DEPTH);

            if (file == null || file.isBlank()) {
                return "Error: 'file' parameter is required.";
            }
            if (line < 1) {
                return "Error: 'line' parameter is required (1-based line number of the start method).";
            }

            boolean callees = "callees".equalsIgnoreCase(direction);
            int boundedDepth = Math.max(1, Math.min(depth, MAX_DEPTH));

            return ReadAccess.compute(() -> trace(file, line, symbol, target, callees, boundedDepth));
        } catch (Exception e) {
            log.error("Error tracing call chains", e);
            return "Error: Failed to trace call chains - " + e.getMessage();
        }
    }

    private @NotNull String trace(String filePath, int line, String symbol, String target,
                                  boolean callees, int maxDepth) {
        VirtualFile projectBase = PsiToolUtils.getProjectBase(project);
        if (projectBase == null) {
            return "Error: Project base directory not found.";
        }

        PsiFile psiFile = PsiToolUtils.resolvePsiFile(project, filePath);
        if (psiFile == null) {
            return "Error: File not found or cannot be parsed: " + filePath;
        }

        if (!PsiToolUtils.isJavaAvailable() || !PsiToolUtils.isJavaFile(psiFile)) {
            return "trace_call_chains is not supported for " + PsiToolUtils.languageName(psiFile)
                    + " in this version (Java only).";
        }

        PsiNameIdentifierOwner owner = PsiToolUtils.findNamedElementOnLine(psiFile, line, symbol);
        if (!(owner instanceof PsiMethod start)) {
            return "Error: No method definition found at " + filePath + ":" + line
                    + (symbol != null ? " matching '" + symbol + "'" : "")
                    + ". Ensure the line contains a method declaration.";
        }

        String targetName = (target != null && !target.isBlank()) ? target.trim() : null;
        Trace trace = new Trace(callees, maxDepth, targetName, projectBase);
        List<String> path = new ArrayList<>();
        path.add(label(start, projectBase));
        trace.walk(start, path, new LinkedHashSet<>(List.of(start)), 1);

        return trace.render(start, callees);
    }

    /**
     * Holds mutable traversal state so the recursive DFS stays readable and the caps
     * are enforced in one place.
     */
    private final class Trace {
        private final boolean callees;
        private final int maxDepth;
        private final String targetName;
        private final VirtualFile projectBase;
        private final List<List<String>> paths = new ArrayList<>();
        private int nodesExpanded = 0;
        private boolean budgetExhausted = false;
        private boolean targetReached = false;

        Trace(boolean callees, int maxDepth, String targetName, VirtualFile projectBase) {
            this.callees = callees;
            this.maxDepth = maxDepth;
            this.targetName = targetName;
            this.projectBase = projectBase;
        }

        void walk(@NotNull PsiMethod current, @NotNull List<String> pathSoFar,
                  @NotNull Set<PsiMethod> onPath, int depth) {
            if (paths.size() >= MAX_PATHS || budgetExhausted) return;

            if (targetName != null && targetName.equals(current.getName()) && depth > 1) {
                paths.add(new ArrayList<>(pathSoFar));
                targetReached = true;
                return;
            }

            if (depth >= maxDepth) {
                if (targetName == null) paths.add(new ArrayList<>(pathSoFar)); // record terminal chains only when not target-seeking
                return;
            }

            if (++nodesExpanded > NODE_BUDGET) {
                budgetExhausted = true;
                return;
            }

            List<PsiMethod> neighbors = callees ? callees(current) : callers(current);
            if (neighbors.isEmpty()) {
                if (targetName == null) paths.add(new ArrayList<>(pathSoFar)); // leaf chain
                return;
            }

            for (PsiMethod next : neighbors) {
                if (paths.size() >= MAX_PATHS || budgetExhausted) return;
                if (onPath.contains(next)) continue; // avoid cycles
                pathSoFar.add(label(next, projectBase));
                onPath.add(next);
                walk(next, pathSoFar, onPath, depth + 1);
                onPath.remove(next);
                pathSoFar.remove(pathSoFar.size() - 1);
            }
        }

        @NotNull String render(@NotNull PsiMethod start, boolean callees) {
            String arrow = callees ? "calls →" : "called by ←";
            if (paths.isEmpty()) {
                return "No " + (callees ? "callee" : "caller") + " chains found from '"
                        + start.getName() + "' within depth " + maxDepth + ".";
            }
            StringBuilder sb = new StringBuilder();
            if (targetName != null) {
                sb.append(targetReached
                        ? "Found " + paths.size() + " chain(s) from '" + start.getName() + "' reaching '" + targetName + "':\n\n"
                        : "No chain from '" + start.getName() + "' reached '" + targetName + "' within depth " + maxDepth + ".\n");
                if (!targetReached) return sb.toString();
            } else {
                sb.append("Traced ").append(paths.size()).append(" ")
                        .append(callees ? "callee" : "caller").append(" chain(s) from '")
                        .append(start.getName()).append("' (").append(arrow).append("), depth ≤ ")
                        .append(maxDepth).append(":\n\n");
            }
            int i = 1;
            for (List<String> p : paths) {
                sb.append(i++).append(". ").append(String.join(callees ? "  →  " : "  ←  ", p)).append("\n");
            }
            if (paths.size() >= MAX_PATHS) {
                sb.append("\n... (truncated at ").append(MAX_PATHS).append(" paths)");
            }
            if (budgetExhausted) {
                sb.append("\n(note: traversal stopped early after expanding ").append(NODE_BUDGET)
                        .append(" nodes — results may be partial)");
            }
            return sb.toString();
        }
    }

    private @NotNull List<PsiMethod> callees(@NotNull PsiMethod method) {
        Set<PsiMethod> result = new LinkedHashSet<>();
        for (PsiCallExpression call : PsiTreeUtil.collectElementsOfType(method, PsiCallExpression.class)) {
            PsiMethod callee = call.resolveMethod();
            if (callee != null) result.add(callee);
        }
        return new ArrayList<>(result);
    }

    private @NotNull List<PsiMethod> callers(@NotNull PsiMethod method) {
        Set<PsiMethod> result = new LinkedHashSet<>();
        for (PsiReference ref : ReferencesSearch.search(method, GlobalSearchScope.projectScope(project)).findAll()) {
            PsiElement el = ref.getElement();
            PsiMethod caller = PsiTreeUtil.getParentOfType(el, PsiMethod.class);
            if (caller != null) result.add(caller);
        }
        return new ArrayList<>(result);
    }

    private @NotNull String label(@NotNull PsiMethod method, @NotNull VirtualFile projectBase) {
        String owner = method.getContainingClass() != null ? method.getContainingClass().getName() + "." : "";
        PsiElement anchor = method.getNameIdentifier() != null ? method.getNameIdentifier() : method;
        String location = PsiToolUtils.formatLocation(anchor, projectBase);
        return owner + method.getName() + "()" + (location != null ? " (" + location + ")" : "");
    }
}
