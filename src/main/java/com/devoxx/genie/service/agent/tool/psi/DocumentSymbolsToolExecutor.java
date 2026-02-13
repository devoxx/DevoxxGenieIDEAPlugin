package com.devoxx.genie.service.agent.tool.psi;

import com.devoxx.genie.service.agent.tool.ToolArgumentParser;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI-based tool that lists all symbol definitions in a file with their kind
 * (class, method, field) and line numbers, preserving nesting structure.
 */
@Slf4j
public class DocumentSymbolsToolExecutor implements ToolExecutor {

    private static final int MAX_SYMBOLS = 200;

    private final Project project;

    public DocumentSymbolsToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String path = ToolArgumentParser.getString(request.arguments(), "file");
            if (path == null || path.isBlank()) {
                return "Error: 'file' parameter is required.";
            }

            return ReadAction.compute(() -> listSymbols(path));
        } catch (Exception e) {
            log.error("Error listing document symbols", e);
            return "Error: Failed to list symbols - " + e.getMessage();
        }
    }

    private @NotNull String listSymbols(String path) {
        PsiFile psiFile = PsiToolUtils.resolvePsiFile(project, path);
        if (psiFile == null) {
            return "Error: File not found or cannot be parsed: " + path;
        }

        List<String> symbols = new ArrayList<>();
        collectSymbols(psiFile, symbols, 0);

        if (symbols.isEmpty()) {
            return "No symbols found in: " + path;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Symbols in ").append(path).append(":\n\n");
        for (String s : symbols) {
            sb.append(s).append("\n");
        }
        if (symbols.size() >= MAX_SYMBOLS) {
            sb.append("\n... (truncated at ").append(MAX_SYMBOLS).append(" symbols)");
        }
        return sb.toString();
    }

    private void collectSymbols(@NotNull PsiElement element, @NotNull List<String> results, int depth) {
        if (results.size() >= MAX_SYMBOLS) return;

        if (element instanceof PsiNameIdentifierOwner owner && owner.getName() != null) {
            String indent = "  ".repeat(depth);
            String kind = PsiToolUtils.getElementKind(owner);
            int line = PsiToolUtils.getLineNumber(owner);
            results.add(String.format("%s[%s] %s (line %d)", indent, kind, owner.getName(), line));
        }

        for (PsiElement child : element.getChildren()) {
            if (results.size() >= MAX_SYMBOLS) return;
            int nextDepth = (element instanceof PsiNameIdentifierOwner) ? depth + 1 : depth;
            collectSymbols(child, results, nextDepth);
        }
    }
}
