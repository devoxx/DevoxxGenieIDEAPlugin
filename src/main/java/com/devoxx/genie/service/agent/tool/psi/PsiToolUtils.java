package com.devoxx.genie.service.agent.tool.psi;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
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
     * <p>
     * First tries an exact lookup relative to the project root. When that fails - typically
     * because the LLM guessed the wrong package directory (e.g. {@code .../ui/Foo.java} when the
     * file actually lives under {@code .../controller/Foo.java}) - it falls back to a filename
     * search across the project and picks the candidate whose path best matches the requested one.
     */
    @Nullable
    public static PsiFile resolvePsiFile(@NotNull Project project, @NotNull String relativePath) {
        VirtualFile vf = resolveVirtualFile(project, relativePath);
        if (vf == null) return null;
        return PsiManager.getInstance(project).findFile(vf);
    }

    /**
     * Resolves a relative path to a VirtualFile, with a filename-based fallback. See
     * {@link #resolvePsiFile} for the resolution strategy.
     */
    @Nullable
    public static VirtualFile resolveVirtualFile(@NotNull Project project, @NotNull String relativePath) {
        VirtualFile projectBase = ProjectUtil.guessProjectDir(project);
        if (projectBase == null) return null;

        String normalized = normalizeRelativePath(relativePath);
        if (normalized.isEmpty()) return null;

        // 1. Exact relative-path match (fast path).
        VirtualFile vf = projectBase.findFileByRelativePath(normalized);
        if (vf != null && !vf.isDirectory()) return vf;

        // 2. Fallback: locate by filename and pick the closest path match.
        return resolveByFileName(project, normalized);
    }

    @Nullable
    private static VirtualFile resolveByFileName(@NotNull Project project, @NotNull String normalizedPath) {
        String fileName = fileNameOf(normalizedPath);
        if (fileName.isEmpty()) return null;

        // FilenameIndex requires a built index; bail out gracefully during indexing.
        if (DumbService.getInstance(project).isDumb()) return null;

        try {
            Collection<VirtualFile> matches =
                    FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.projectScope(project));
            return pickBestPathMatch(matches, normalizedPath);
        } catch (IndexNotReadyException e) {
            return null;
        }
    }

    /**
     * Normalizes a user-supplied path: trims whitespace, converts back-slashes, and strips
     * leading {@code ./} and {@code /} segments so it is relative to the project root.
     */
    @NotNull
    static String normalizeRelativePath(@NotNull String path) {
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    /** Returns the last path segment (the file name) of a normalized path. */
    @NotNull
    static String fileNameOf(@NotNull String normalizedPath) {
        int slash = normalizedPath.lastIndexOf('/');
        return slash < 0 ? normalizedPath : normalizedPath.substring(slash + 1);
    }

    /**
     * From a set of files that all share the requested file name, picks the one whose path shares
     * the longest trailing run of path segments with the requested path. This disambiguates when
     * several files have the same name in different packages. Returns null when there are no
     * candidates, and the single candidate when there is exactly one.
     */
    @Nullable
    static VirtualFile pickBestPathMatch(@NotNull Collection<VirtualFile> candidates, @NotNull String requestedPath) {
        if (candidates.isEmpty()) return null;

        List<String> requestedSegments = splitSegments(requestedPath);

        VirtualFile best = null;
        int bestScore = -1;
        for (VirtualFile candidate : candidates) {
            if (candidate == null || candidate.isDirectory()) continue;
            int score = trailingSegmentOverlap(requestedSegments, splitSegments(candidate.getPath()));
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    @NotNull
    private static List<String> splitSegments(@NotNull String path) {
        List<String> segments = new ArrayList<>();
        for (String part : path.split("/")) {
            if (!part.isBlank()) segments.add(part);
        }
        return segments;
    }

    /** Counts how many trailing path segments two paths have in common (file name counts as one). */
    private static int trailingSegmentOverlap(@NotNull List<String> a, @NotNull List<String> b) {
        int overlap = 0;
        int i = a.size() - 1;
        int j = b.size() - 1;
        while (i >= 0 && j >= 0 && a.get(i).equals(b.get(j))) {
            overlap++;
            i--;
            j--;
        }
        return overlap;
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

        // If a symbol name is provided, only return an element that actually matches it. Returning
        // an arbitrary unrelated declaration that merely happens to share the line would produce a
        // wrong definition; instead return null so callers can fall back or report a clear error.
        if (symbolName != null && !symbolName.isBlank()) {
            for (PsiNameIdentifierOwner owner : onLine) {
                if (symbolName.equals(owner.getName())) return owner;
            }
            return null;
        }

        // No symbol requested: return the first declaration found on the line.
        return onLine.get(0);
    }

    /**
     * Finds the first PsiNameIdentifierOwner declared anywhere in the file whose name matches the
     * given symbol. Used as a file-wide fallback when a precise line is unavailable.
     */
    @Nullable
    public static PsiNameIdentifierOwner findNamedElementInFile(@NotNull PsiFile psiFile, @Nullable String symbolName) {
        if (symbolName == null || symbolName.isBlank()) return null;
        for (PsiNameIdentifierOwner owner : PsiTreeUtil.findChildrenOfType(psiFile, PsiNameIdentifierOwner.class)) {
            if (symbolName.equals(owner.getName())) return owner;
        }
        return null;
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

    /**
     * Returns true when the IntelliJ Java plugin (PSI) is present on the runtime classpath.
     * Mirrors the reflection guard in {@code JavaProjectScannerExtension.isJavaAvailable()} so
     * the plugin still loads in non-Java IDEs where {@code com.intellij.modules.java} is absent.
     * Call this before touching any {@code com.intellij.psi.PsiMethod}/{@code PsiClass} API.
     */
    public static boolean isJavaAvailable() {
        try {
            Class.forName("com.intellij.psi.JavaPsiFacade");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Returns true when the given file is a Java source file. Uses the language ID only, so it
     * does not link any Java-plugin class and is safe to call when Java support is absent.
     */
    public static boolean isJavaFile(@NotNull PsiFile psiFile) {
        return "JAVA".equals(psiFile.getLanguage().getID());
    }

    /**
     * Human-readable language name for "not supported" messages (e.g. "Kotlin", "Python").
     */
    @NotNull
    public static String languageName(@NotNull PsiFile psiFile) {
        return psiFile.getLanguage().getDisplayName();
    }
}
