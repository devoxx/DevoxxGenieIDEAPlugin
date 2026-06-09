package com.devoxx.genie.service.agent.tool.psi;

import com.devoxx.genie.service.agent.tool.ToolArgumentParser;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * PSI-based tool that reports symbols in a file with zero project-scope references —
 * <strong>heuristic dead-code candidates, not certainties</strong>. Reflection,
 * serialization, dependency-injection frameworks, and external (out-of-project)
 * callers can all reference a symbol invisibly, so every result must be confirmed
 * by a human before deletion.
 *
 * <p>To keep false positives low (false positives erode trust), the following are
 * conservatively excluded: {@code public} members (potential API), constructors and
 * {@code main}, {@code @Override} and any annotated member (frameworks reach these
 * reflectively), and serialization members ({@code serialVersionUID},
 * {@code readObject}/{@code writeObject}/{@code readResolve}/{@code writeReplace}).
 * Java only in v1.
 */
@Slf4j
public class FindDeadCodeToolExecutor implements ToolExecutor {

    private static final int MAX_RESULTS = 50;
    private static final Set<String> SERIALIZATION_METHODS =
            Set.of("readObject", "writeObject", "readResolve", "writeReplace", "readObjectNoData");

    private final Project project;

    public FindDeadCodeToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String file = ToolArgumentParser.getString(request.arguments(), "file");
            if (file == null || file.isBlank()) {
                return "Error: 'file' parameter is required.";
            }
            return ReadAction.compute(() -> findDeadCode(file));
        } catch (Exception e) {
            log.error("Error finding dead code", e);
            return "Error: Failed to find dead code - " + e.getMessage();
        }
    }

    private @NotNull String findDeadCode(String filePath) {
        VirtualFile projectBase = PsiToolUtils.getProjectBase(project);
        if (projectBase == null) {
            return "Error: Project base directory not found.";
        }

        PsiFile psiFile = PsiToolUtils.resolvePsiFile(project, filePath);
        if (psiFile == null) {
            return "Error: File not found or cannot be parsed: " + filePath;
        }

        if (!PsiToolUtils.isJavaAvailable() || !PsiToolUtils.isJavaFile(psiFile)) {
            return "find_dead_code is not supported for " + PsiToolUtils.languageName(psiFile)
                    + " in this version (Java only).";
        }

        List<String> candidates = new ArrayList<>();

        for (PsiMethod method : PsiTreeUtil.collectElementsOfType(psiFile, PsiMethod.class)) {
            if (candidates.size() >= MAX_RESULTS) break;
            if (isExcludedMethod(method)) continue;
            if (isUnreferenced(method)) {
                candidates.add(format("method", method.getName(), method, projectBase));
            }
        }
        for (PsiField field : PsiTreeUtil.collectElementsOfType(psiFile, PsiField.class)) {
            if (candidates.size() >= MAX_RESULTS) break;
            if (isExcludedField(field)) continue;
            if (isUnreferenced(field)) {
                candidates.add(format("field", field.getName(), field, projectBase));
            }
        }
        for (PsiClass clazz : PsiTreeUtil.collectElementsOfType(psiFile, PsiClass.class)) {
            if (candidates.size() >= MAX_RESULTS) break;
            if (isExcludedClass(clazz)) continue;
            if (isUnreferenced(clazz)) {
                candidates.add(format("class", clazz.getName(), clazz, projectBase));
            }
        }

        if (candidates.isEmpty()) {
            return "No dead-code candidates found in " + filePath
                    + " (every analysed symbol has at least one project-scope reference, or was conservatively excluded).";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Dead-code CANDIDATES in ").append(filePath)
                .append(" — heuristic only; confirm before deleting. Reflection, serialization, DI, ")
                .append("and out-of-project callers are invisible to this search:\n\n");
        for (String c : candidates) {
            sb.append("  ").append(c).append("\n");
        }
        if (candidates.size() >= MAX_RESULTS) {
            sb.append("\n... (truncated at ").append(MAX_RESULTS).append(" candidates)");
        }
        return sb.toString();
    }

    private boolean isUnreferenced(@NotNull PsiMember member) {
        return ReferencesSearch.search(member, GlobalSearchScope.projectScope(project)).findFirst() == null;
    }

    private boolean isExcludedMethod(@NotNull PsiMethod method) {
        if (method.getName().isBlank()) return true;
        if (method.isConstructor()) return true;
        if (method.hasModifierProperty(PsiModifier.PUBLIC)) return true;
        if (isAnnotated(method)) return true;                              // @Override and any framework annotation
        if (SERIALIZATION_METHODS.contains(method.getName())) return true;
        return "main".equals(method.getName());
    }

    private boolean isExcludedField(@NotNull PsiField field) {
        if (field.getName().isBlank()) return true;
        if (field.hasModifierProperty(PsiModifier.PUBLIC)) return true;
        if (isAnnotated(field)) return true;
        return "serialVersionUID".equals(field.getName());
    }

    private boolean isExcludedClass(@NotNull PsiClass clazz) {
        if (clazz.getName() == null) return true;
        if (clazz.hasModifierProperty(PsiModifier.PUBLIC)) return true;
        return isAnnotated(clazz);
    }

    private boolean isAnnotated(@NotNull PsiModifierListOwner owner) {
        return owner.getModifierList() != null && owner.getModifierList().getAnnotations().length > 0;
    }

    private @NotNull String format(@NotNull String kind, String name, @NotNull PsiMember member,
                                   @NotNull VirtualFile projectBase) {
        String location = PsiToolUtils.formatLocation(
                member instanceof com.intellij.psi.PsiNameIdentifierOwner owner && owner.getNameIdentifier() != null
                        ? owner.getNameIdentifier() : member,
                projectBase);
        return "[" + kind + "] " + name + (location != null ? "  " + location : "");
    }
}
