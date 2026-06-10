package com.devoxx.genie.service.agent.tool.psi;

import com.devoxx.genie.service.agent.tool.ToolArgumentParser;
import com.devoxx.genie.util.ReadAccess;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.util.PsiTreeUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PSI-based tool that lists the methods called (outgoing edges) from a method
 * defined at a given file and line. This is the inverse of {@code find_references}:
 * where {@code find_references} answers "who calls X", {@code find_callees} answers
 * "what does X call". Each call target is resolved through the IDE's semantic index,
 * so it understands overloads, inheritance, and imports.
 *
 * <p>v1 supports Java. Other languages return a clear "not supported" message.
 */
@Slf4j
public class FindCalleesToolExecutor implements ToolExecutor {

    private static final int MAX_RESULTS = 50;

    private final Project project;

    public FindCalleesToolExecutor(@NotNull Project project) {
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
                return "Error: 'line' parameter is required (1-based line number of the method).";
            }

            return ReadAccess.compute(() -> findCallees(file, line, symbol));
        } catch (Exception e) {
            log.error("Error finding callees", e);
            return "Error: Failed to find callees - " + e.getMessage();
        }
    }

    private @NotNull String findCallees(String filePath, int line, String symbol) {
        VirtualFile projectBase = PsiToolUtils.getProjectBase(project);
        if (projectBase == null) {
            return "Error: Project base directory not found.";
        }

        PsiFile psiFile = PsiToolUtils.resolvePsiFile(project, filePath);
        if (psiFile == null) {
            return "Error: File not found or cannot be parsed: " + filePath;
        }

        if (!PsiToolUtils.isJavaAvailable() || !PsiToolUtils.isJavaFile(psiFile)) {
            return "find_callees is not supported for " + PsiToolUtils.languageName(psiFile)
                    + " in this version (Java only). Use find_references for caller direction across languages.";
        }

        PsiNameIdentifierOwner target = PsiToolUtils.findNamedElementOnLine(psiFile, line, symbol);
        if (!(target instanceof PsiMethod method)) {
            return "Error: No method definition found at " + filePath + ":" + line
                    + (symbol != null ? " matching '" + symbol + "'" : "")
                    + ". Ensure the line contains a method declaration.";
        }

        return collectCallees(method, filePath, line, projectBase);
    }

    private @NotNull String collectCallees(@NotNull PsiMethod method,
                                           String filePath,
                                           int line,
                                           @NotNull VirtualFile projectBase) {
        // Preserve first-seen order while deduping by resolved target.
        Map<PsiMethod, String> calleeToLocation = new LinkedHashMap<>();

        for (PsiCallExpression call : PsiTreeUtil.collectElementsOfType(method, PsiCallExpression.class)) {
            if (calleeToLocation.size() >= MAX_RESULTS) break;

            PsiMethod callee = call.resolveMethod();
            if (callee == null || calleeToLocation.containsKey(callee)) continue;

            String location = PsiToolUtils.formatLocation(
                    callee.getNameIdentifier() != null ? callee.getNameIdentifier() : callee, projectBase);
            String signature = describe(callee);
            calleeToLocation.put(callee, location != null
                    ? signature + "  " + location
                    : signature + "  (no source — library/binary)");
        }

        if (calleeToLocation.isEmpty()) {
            return "No resolved method calls found in '" + method.getName() + "' at " + filePath + ":" + line + ".";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("'").append(method.getName()).append("' calls ")
                .append(calleeToLocation.size()).append(" distinct method(s):\n\n");
        for (String entry : calleeToLocation.values()) {
            sb.append("  ").append(entry).append("\n");
        }
        if (calleeToLocation.size() >= MAX_RESULTS) {
            sb.append("\n... (truncated at ").append(MAX_RESULTS).append(" results)");
        }
        return sb.toString();
    }

    private @NotNull String describe(@NotNull PsiMethod callee) {
        PsiMethod containing = callee;
        String owner = (containing.getContainingClass() != null)
                ? containing.getContainingClass().getName() + "."
                : "";
        return owner + callee.getName() + "()";
    }
}
