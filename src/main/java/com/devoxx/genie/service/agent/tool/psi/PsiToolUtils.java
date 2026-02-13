package com.devoxx.genie.service.agent.tool.psi;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Shared utilities for PSI-based agent tools.
 */
public final class PsiToolUtils {

    private PsiToolUtils() {
    }

    /**
     * Resolves a relative path to a PsiFile within the project.
     */
    @Nullable
    public static PsiFile resolvePsiFile(@NotNull Project project, @NotNull String relativePath) {
        VirtualFile projectBase = ProjectUtil.guessProjectDir(project);
        if (projectBase == null) return null;

        VirtualFile vf = projectBase.findFileByRelativePath(relativePath);
        if (vf == null || vf.isDirectory()) return null;

        return PsiManager.getInstance(project).findFile(vf);
    }

    /**
     * Converts a 1-based line number to a character offset in the document.
     */
    public static int lineToOffset(@NotNull PsiFile psiFile, int line) {
        Document doc = PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
        if (doc == null || line < 1 || line > doc.getLineCount()) return -1;
        return doc.getLineStartOffset(line - 1);
    }

    /**
     * Returns the end offset of a 1-based line.
     */
    public static int lineEndOffset(@NotNull PsiFile psiFile, int line) {
        Document doc = PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
        if (doc == null || line < 1 || line > doc.getLineCount()) return -1;
        return doc.getLineEndOffset(line - 1);
    }

    /**
     * Finds the first PsiNameIdentifierOwner on a given line, optionally matching a symbol name.
     */
    @Nullable
    public static PsiNameIdentifierOwner findNamedElementOnLine(@NotNull PsiFile psiFile, int line, @Nullable String symbolName) {
        int startOffset = lineToOffset(psiFile, line);
        int endOffset = lineEndOffset(psiFile, line);
        if (startOffset < 0 || endOffset < 0) return null;

        // Collect all named elements whose name identifier is on this line
        Collection<PsiNameIdentifierOwner> allNamed = PsiTreeUtil.findChildrenOfType(psiFile, PsiNameIdentifierOwner.class);
        List<PsiNameIdentifierOwner> onLine = new ArrayList<>();

        for (PsiNameIdentifierOwner owner : allNamed) {
            PsiElement nameId = owner.getNameIdentifier();
            if (nameId == null) continue;
            int nameOffset = nameId.getTextOffset();
            if (nameOffset >= startOffset && nameOffset <= endOffset) {
                onLine.add(owner);
            }
        }

        if (onLine.isEmpty()) return null;

        // If symbol name is provided, find the matching one
        if (symbolName != null && !symbolName.isBlank()) {
            for (PsiNameIdentifierOwner owner : onLine) {
                if (symbolName.equals(owner.getName())) return owner;
            }
        }

        // Return the first one found on the line
        return onLine.get(0);
    }

    /**
     * Finds a PsiElement at a specific file offset, navigating to the most specific element.
     */
    @Nullable
    public static PsiElement findElementAt(@NotNull PsiFile psiFile, int offset) {
        return psiFile.findElementAt(offset);
    }

    /**
     * Determines the kind of a PSI element (class, method, field, etc.) using class name heuristics.
     * Works across languages without requiring language-specific PSI dependencies.
     */
    @NotNull
    public static String getElementKind(@NotNull PsiNameIdentifierOwner owner) {
        String className = owner.getClass().getSimpleName().toLowerCase();

        if (className.contains("class") || className.contains("interface")
                || className.contains("enum") || className.contains("trait")
                || className.contains("struct") || className.contains("object")) {
            return "class";
        }
        if (className.contains("method") || className.contains("function")
                || className.contains("constructor")) {
            return "method";
        }
        if (className.contains("field") || className.contains("variable")
                || className.contains("property") || className.contains("parameter")
                || className.contains("constant")) {
            return "field";
        }

        return "symbol";
    }

    /**
     * Returns the 1-based line number of a PsiElement.
     */
    public static int getLineNumber(@NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (file == null) return -1;
        Document doc = PsiDocumentManager.getInstance(element.getProject()).getDocument(file);
        if (doc == null) return -1;
        return doc.getLineNumber(element.getTextOffset()) + 1;
    }

    /**
     * Formats a PsiElement's location as "relativePath:line".
     */
    @Nullable
    public static String formatLocation(@NotNull PsiElement element, @NotNull VirtualFile projectBase) {
        PsiFile file = element.getContainingFile();
        if (file == null) return null;
        VirtualFile vf = file.getVirtualFile();
        if (vf == null) return null;

        String relativePath = VfsUtil.getRelativePath(vf, projectBase);
        if (relativePath == null) relativePath = vf.getPath();

        int line = getLineNumber(element);
        return relativePath + ":" + line;
    }

    /**
     * Returns the project base directory.
     */
    @Nullable
    public static VirtualFile getProjectBase(@NotNull Project project) {
        return ProjectUtil.guessProjectDir(project);
    }
}
