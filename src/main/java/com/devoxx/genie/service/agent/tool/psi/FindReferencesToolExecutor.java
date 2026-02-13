package com.devoxx.genie.service.agent.tool.psi;

import com.devoxx.genie.service.agent.tool.ToolArgumentParser;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * PSI-based tool that finds all references (usages) of a symbol defined at
 * a given file and line. Uses IntelliJ's semantic reference search, which
 * is more accurate than text-based grep.
 */
@Slf4j
public class FindReferencesToolExecutor implements ToolExecutor {

    private static final int MAX_RESULTS = 50;

    private final Project project;

    public FindReferencesToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String file = ToolArgumentParser.getString(request.arguments(), "file");
            int line = ToolArgumentParser.getInt(request.arguments(), "line", -1);
            String symbol = ToolArgumentParser.getString(request.arguments(), "symbol");

            if (file == null || file.isBlank()) {
                return "Error: 'file' parameter is required.";
            }
            if (line < 1) {
                return "Error: 'line' parameter is required (1-based line number).";
            }

            return ReadAction.compute(() -> findReferences(file, line, symbol));
        } catch (Exception e) {
            log.error("Error finding references", e);
            return "Error: Failed to find references - " + e.getMessage();
        }
    }

    private @NotNull String findReferences(String filePath, int line, String symbol) {
        VirtualFile projectBase = PsiToolUtils.getProjectBase(project);
        if (projectBase == null) {
            return "Error: Project base directory not found.";
        }

        PsiFile psiFile = PsiToolUtils.resolvePsiFile(project, filePath);
        if (psiFile == null) {
            return "Error: File not found or cannot be parsed: " + filePath;
        }

        PsiNameIdentifierOwner target = PsiToolUtils.findNamedElementOnLine(psiFile, line, symbol);
        if (target == null) {
            return "Error: No symbol definition found at " + filePath + ":" + line
                    + (symbol != null ? " matching '" + symbol + "'" : "")
                    + ". Ensure the line contains a symbol definition.";
        }

        Collection<PsiReference> refs = ReferencesSearch.search(
                target, GlobalSearchScope.projectScope(project)
        ).findAll();

        if (refs.isEmpty()) {
            return "No references found for '" + target.getName() + "' defined at " + filePath + ":" + line;
        }

        List<String> results = new ArrayList<>();
        for (PsiReference ref : refs) {
            if (results.size() >= MAX_RESULTS) break;

            PsiElement refElement = ref.getElement();
            String location = PsiToolUtils.formatLocation(refElement, projectBase);
            if (location != null) {
                results.add("  " + location);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(refs.size()).append(" reference(s) to '").append(target.getName()).append("':\n\n");
        for (String r : results) {
            sb.append(r).append("\n");
        }
        if (refs.size() > MAX_RESULTS) {
            sb.append("\n... (showing first ").append(MAX_RESULTS).append(" of ").append(refs.size()).append(" results)");
        }
        return sb.toString();
    }
}
