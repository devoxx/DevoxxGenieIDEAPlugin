package com.devoxx.genie.service.agent.tool.psi;

import com.devoxx.genie.service.agent.tool.ToolArgumentParser;
import com.devoxx.genie.util.ReadAccess;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiCatchSection;
import com.intellij.psi.PsiConditionalExpression;
import com.intellij.psi.PsiDoWhileStatement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiForeachStatement;
import com.intellij.psi.PsiIfStatement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiSwitchLabelStatementBase;
import com.intellij.psi.PsiWhileStatement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI-based tool that computes cyclomatic complexity for Java methods by counting
 * decision points over the PSI tree. With a {@code line} it targets a single method;
 * otherwise it scores every method in the file and flags those over a threshold.
 *
 * <p>Complexity counting rule (McCabe), starting at 1 and adding one for each:
 * {@code if}, {@code for}, {@code foreach}, {@code while}, {@code do/while},
 * each non-default {@code case} label, each {@code catch}, the ternary {@code ?:},
 * and each {@code &&} / {@code ||} operator. Decision points inside nested
 * anonymous classes/lambdas are included in the enclosing method's score. Java only in v1.
 */
@Slf4j
public class CalculateComplexityToolExecutor implements ToolExecutor {

    private static final int DEFAULT_THRESHOLD = 10;

    private final Project project;

    public CalculateComplexityToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String file = ToolArgumentParser.getString(request.arguments(), "file");
            int line = ToolArgumentParser.getInt(request.arguments(), "line", -1);
            int threshold = ToolArgumentParser.getInt(request.arguments(), "threshold", DEFAULT_THRESHOLD);

            if (file == null || file.isBlank()) {
                return "Error: 'file' parameter is required.";
            }
            int effectiveThreshold = threshold > 0 ? threshold : DEFAULT_THRESHOLD;

            return ReadAccess.compute(() -> compute(file, line, effectiveThreshold));
        } catch (Exception e) {
            log.error("Error calculating complexity", e);
            return "Error: Failed to calculate complexity - " + e.getMessage();
        }
    }

    private @NotNull String compute(String filePath, int line, int threshold) {
        PsiFile psiFile = PsiToolUtils.resolvePsiFile(project, filePath);
        if (psiFile == null) {
            return "Error: File not found or cannot be parsed: " + filePath;
        }

        if (!PsiToolUtils.isJavaAvailable() || !PsiToolUtils.isJavaFile(psiFile)) {
            return "calculate_complexity is not supported for " + PsiToolUtils.languageName(psiFile)
                    + " in this version (Java only).";
        }

        List<PsiMethod> methods = new ArrayList<>();
        if (line >= 1) {
            PsiNameIdentifierOwner owner = PsiToolUtils.findNamedElementOnLine(psiFile, line, null);
            if (!(owner instanceof PsiMethod method)) {
                return "Error: No method definition found at " + filePath + ":" + line + ".";
            }
            methods.add(method);
        } else {
            methods.addAll(PsiTreeUtil.collectElementsOfType(psiFile, PsiMethod.class));
        }

        if (methods.isEmpty()) {
            return "No methods found in " + filePath + ".";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Cyclomatic complexity for ").append(filePath)
                .append(" (threshold ").append(threshold).append("):\n\n");
        int flagged = 0;
        for (PsiMethod method : methods) {
            int complexity = complexityOf(method);
            boolean over = complexity > threshold;
            if (over) flagged++;
            sb.append("  ").append(over ? "⚠ " : "  ")
                    .append(signature(method)).append("  →  ").append(complexity)
                    .append(over ? "  (over threshold)" : "")
                    .append("\n");
        }
        if (line < 1) {
            sb.append("\n").append(flagged).append(" of ").append(methods.size())
                    .append(" method(s) exceed the threshold of ").append(threshold).append(".");
        }
        return sb.toString();
    }

    /**
     * McCabe cyclomatic complexity over a single method's subtree. Starts at 1 and
     * adds one decision point per branch/loop/case/catch/ternary, plus one per
     * {@code &&}/{@code ||} operator (an N-operand chain contributes N-1).
     */
    static int complexityOf(@NotNull PsiMethod method) {
        int complexity = 1;
        complexity += PsiTreeUtil.collectElementsOfType(method, PsiIfStatement.class).size();
        complexity += PsiTreeUtil.collectElementsOfType(method, PsiForStatement.class).size();
        complexity += PsiTreeUtil.collectElementsOfType(method, PsiForeachStatement.class).size();
        complexity += PsiTreeUtil.collectElementsOfType(method, PsiWhileStatement.class).size();
        complexity += PsiTreeUtil.collectElementsOfType(method, PsiDoWhileStatement.class).size();
        complexity += PsiTreeUtil.collectElementsOfType(method, PsiCatchSection.class).size();
        complexity += PsiTreeUtil.collectElementsOfType(method, PsiConditionalExpression.class).size();

        for (PsiSwitchLabelStatementBase label : PsiTreeUtil.collectElementsOfType(method, PsiSwitchLabelStatementBase.class)) {
            if (!label.isDefaultCase()) complexity++;
        }
        for (PsiPolyadicExpression expr : PsiTreeUtil.collectElementsOfType(method, PsiPolyadicExpression.class)) {
            if (expr.getOperationTokenType() == JavaTokenType.ANDAND
                    || expr.getOperationTokenType() == JavaTokenType.OROR) {
                complexity += expr.getOperands().length - 1;
            }
        }
        return complexity;
    }

    private @NotNull String signature(@NotNull PsiMethod method) {
        String owner = method.getContainingClass() != null && method.getContainingClass().getName() != null
                ? method.getContainingClass().getName() + "."
                : "";
        int paramCount = method.getParameterList().getParametersCount();
        return owner + method.getName() + "(" + paramCount + " param" + (paramCount == 1 ? "" : "s") + ")";
    }
}
