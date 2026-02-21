package com.devoxx.genie.service.agent.tool.psi;

import com.devoxx.genie.service.agent.tool.ToolArgumentParser;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiReference;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI-based tool that navigates from a symbol usage to its definition.
 * Given a file, line, and optional column, resolves the symbol reference
 * at that location and returns the definition's location.
 */
@Slf4j
public class FindDefinitionToolExecutor implements ToolExecutor {

    private final Project project;

    public FindDefinitionToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String file = ToolArgumentParser.getString(request.arguments(), "file");
            int line = ToolArgumentParser.getInt(request.arguments(), "line", -1);
            int column = ToolArgumentParser.getInt(request.arguments(), "column", -1);
            String symbol = ToolArgumentParser.getString(request.arguments(), "symbol");

            if (file == null || file.isBlank()) {
                return "Error: 'file' parameter is required.";
            }
            if (line < 1) {
                return "Error: 'line' parameter is required (1-based line number).";
            }

            return ReadAction.compute(() -> findDefinition(file, line, column, symbol));
        } catch (Exception e) {
            log.error("Error finding definition", e);
            return "Error: Failed to find definition - " + e.getMessage();
        }
    }

    private @NotNull String findDefinition(String filePath, int line, int column, String symbol) {
        VirtualFile projectBase = PsiToolUtils.getProjectBase(project);
        if (projectBase == null) {
            return "Error: Project base directory not found.";
        }

        PsiFile psiFile = PsiToolUtils.resolvePsiFile(project, filePath);
        if (psiFile == null) {
            return "Error: File not found or cannot be parsed: " + filePath;
        }

        // Try to find the element at the specific position
        PsiElement resolved = resolveAtPosition(psiFile, line, column, symbol);

        if (resolved == null) {
            return "Error: Could not resolve symbol at " + filePath + ":" + line
                    + (symbol != null ? " for '" + symbol + "'" : "")
                    + ". Ensure the position points to a symbol reference or usage.";
        }

        // Get the navigation element (the actual definition)
        PsiElement definition = resolved.getNavigationElement();
        if (definition == null) {
            definition = resolved;
        }

        String location = PsiToolUtils.formatLocation(definition, projectBase);
        if (location == null) {
            return "Error: Could not determine location of the definition.";
        }

        String name = (definition instanceof PsiNameIdentifierOwner owner) ? owner.getName() : definition.getText();
        String kind = (definition instanceof PsiNameIdentifierOwner owner)
                ? PsiToolUtils.getElementKind(owner)
                : "symbol";

        StringBuilder sb = new StringBuilder();
        sb.append("Definition of '").append(name != null ? name : "<unnamed>").append("':\n\n");
        sb.append("  [").append(kind).append("] ").append(location);
        return sb.toString();
    }

    /**
     * Tries to resolve a symbol at the given position using multiple strategies:
     * 1. If column is given, find element at exact offset and resolve its reference
     * 2. Walk elements on the line looking for resolvable references
     * 3. Check if a named element on the line is itself the definition
     */
    @Nullable
    private PsiElement resolveAtPosition(@NotNull PsiFile psiFile, int line, int column, @Nullable String symbol) {
        PsiElement fromColumn = resolveAtColumn(psiFile, line, column);
        if (fromColumn != null) return fromColumn;

        PsiElement fromLine = resolveBySearchingLine(psiFile, line, symbol);
        if (fromLine != null) return fromLine;

        return PsiToolUtils.findNamedElementOnLine(psiFile, line, symbol);
    }

    /** Strategy 1: resolve the element at the exact column offset. */
    @Nullable
    private PsiElement resolveAtColumn(@NotNull PsiFile psiFile, int line, int column) {
        if (column <= 0) return null;
        int lineOffset = PsiToolUtils.lineToOffset(psiFile, line);
        if (lineOffset < 0) return null;
        PsiElement element = psiFile.findElementAt(lineOffset + column - 1);
        if (element == null) return null;
        return resolveElement(element);
    }

    /** Strategy 2: walk all elements on the line and return the first resolvable reference. */
    @Nullable
    private PsiElement resolveBySearchingLine(@NotNull PsiFile psiFile, int line, @Nullable String symbol) {
        int startOffset = PsiToolUtils.lineToOffset(psiFile, line);
        int endOffset = PsiToolUtils.lineEndOffset(psiFile, line);
        if (startOffset < 0 || endOffset < 0) return null;

        for (int offset = startOffset; offset <= endOffset; offset++) {
            PsiElement element = psiFile.findElementAt(offset);
            if (element == null) continue;

            // Skip if looking for a specific symbol and this doesn't match
            if (symbol != null && !symbol.equals(element.getText())) continue;

            PsiElement resolved = resolveElement(element);
            if (resolved != null) return resolved;

            // Skip to end of this element to avoid processing it multiple times
            offset = element.getTextRange().getEndOffset() - 1;
        }
        return null;
    }

    /**
     * Tries to resolve the given element's reference to its target definition.
     */
    @Nullable
    private PsiElement resolveElement(@NotNull PsiElement element) {
        // Check the element's own reference
        PsiReference ref = element.getReference();
        if (ref != null) {
            PsiElement target = ref.resolve();
            if (target != null && target != element) return target;
        }

        // Check the parent's reference (e.g., for qualified names)
        PsiElement parent = element.getParent();
        if (parent != null) {
            PsiReference parentRef = parent.getReference();
            if (parentRef != null) {
                PsiElement target = parentRef.resolve();
                if (target != null && target != parent) return target;
            }
        }

        return null;
    }
}
