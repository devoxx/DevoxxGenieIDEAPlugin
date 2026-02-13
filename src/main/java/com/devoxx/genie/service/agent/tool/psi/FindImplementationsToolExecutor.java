package com.devoxx.genie.service.agent.tool.psi;

import com.devoxx.genie.service.agent.tool.ToolArgumentParser;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI-based tool that finds implementations of an interface, abstract class, or
 * abstract method. Uses IntelliJ's DefinitionsScopedSearch which works across
 * languages (Java, Kotlin, etc.).
 */
@Slf4j
public class FindImplementationsToolExecutor implements ToolExecutor {

    private static final int MAX_RESULTS = 50;

    private final Project project;

    public FindImplementationsToolExecutor(@NotNull Project project) {
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

            return ReadAction.compute(() -> findImplementations(file, line, symbol));
        } catch (Exception e) {
            log.error("Error finding implementations", e);
            return "Error: Failed to find implementations - " + e.getMessage();
        }
    }

    private @NotNull String findImplementations(String filePath, int line, String symbol) {
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
                    + ". Ensure the line contains a class, interface, or method definition.";
        }

        List<String> results = new ArrayList<>();
        for (PsiElement impl : DefinitionsScopedSearch.search(target).findAll()) {
            if (results.size() >= MAX_RESULTS) break;

            String location = PsiToolUtils.formatLocation(impl, projectBase);
            if (location == null) continue;

            String name = (impl instanceof PsiNameIdentifierOwner owner) ? owner.getName() : impl.getText();
            String kind = (impl instanceof PsiNameIdentifierOwner owner)
                    ? PsiToolUtils.getElementKind(owner) : "symbol";

            results.add(String.format("  [%s] %s  %s", kind, name, location));
        }

        if (results.isEmpty()) {
            return "No implementations found for '" + target.getName() + "' defined at " + filePath + ":" + line;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(results.size()).append(" implementation(s) of '").append(target.getName()).append("':\n\n");
        for (String r : results) {
            sb.append(r).append("\n");
        }
        if (results.size() >= MAX_RESULTS) {
            sb.append("\n... (truncated at ").append(MAX_RESULTS).append(" results)");
        }
        return sb.toString();
    }
}
