package com.devoxx.genie.service.agent.tool.psi;

import com.devoxx.genie.service.agent.tool.ToolArgumentParser;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.UsageSearchContext;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI-based tool that searches for symbol definitions (classes, methods, fields)
 * by name across the project. Unlike text search, this only returns actual
 * declarations, not usages.
 */
@Slf4j
public class FindSymbolsToolExecutor implements ToolExecutor {

    private static final int MAX_RESULTS = 50;

    private final Project project;

    public FindSymbolsToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String name = ToolArgumentParser.getString(request.arguments(), "name");
            String kind = ToolArgumentParser.getString(request.arguments(), "kind");

            if (name == null || name.isBlank()) {
                return "Error: 'name' parameter is required.";
            }

            return ReadAction.compute(() -> findSymbols(name, kind));
        } catch (Exception e) {
            log.error("Error finding symbols", e);
            return "Error: Failed to find symbols - " + e.getMessage();
        }
    }

    private @NotNull String findSymbols(String name, String kind) {
        VirtualFile projectBase = PsiToolUtils.getProjectBase(project);
        if (projectBase == null) {
            return "Error: Project base directory not found.";
        }

        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        List<String> results = new ArrayList<>();

        PsiSearchHelper helper = PsiSearchHelper.getInstance(project);
        helper.processElementsWithWord(
                (element, offsetInElement) -> processFoundElement(element, results, kind, projectBase),
                scope, name, UsageSearchContext.IN_CODE, true
        );

        if (results.isEmpty()) {
            return "No symbol definitions found matching: " + name;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(results.size()).append(" symbol definition(s):\n\n");
        for (String r : results) {
            sb.append(r).append("\n");
        }
        if (results.size() >= MAX_RESULTS) {
            sb.append("\n... (truncated at ").append(MAX_RESULTS).append(" results)");
        }
        return sb.toString();
    }

    private boolean processFoundElement(@NotNull PsiElement element,
                                        @NotNull List<String> results,
                                        String kind,
                                        @NotNull VirtualFile projectBase) {
        PsiElement parent = element.getParent();
        if (!(parent instanceof PsiNameIdentifierOwner owner)) {
            return results.size() < MAX_RESULTS;
        }
        PsiElement nameId = owner.getNameIdentifier();
        if (nameId == null || !nameId.getTextRange().equals(element.getTextRange())) {
            return results.size() < MAX_RESULTS;
        }
        if (matchesKind(owner, kind)) {
            String formatted = formatSymbol(owner, projectBase);
            if (formatted != null) {
                results.add(formatted);
            }
        }
        return results.size() < MAX_RESULTS;
    }

    private boolean matchesKind(@NotNull PsiNameIdentifierOwner owner, String kind) {
        if (kind == null || kind.isBlank()) return true;
        String elementKind = PsiToolUtils.getElementKind(owner);
        return kind.equalsIgnoreCase(elementKind);
    }

    private String formatSymbol(@NotNull PsiNameIdentifierOwner owner, @NotNull VirtualFile projectBase) {
        String location = PsiToolUtils.formatLocation(owner, projectBase);
        if (location == null) return null;

        String elementKind = PsiToolUtils.getElementKind(owner);
        String symbolName = owner.getName();

        return String.format("  [%s] %s  %s", elementKind, symbolName, location);
    }
}
