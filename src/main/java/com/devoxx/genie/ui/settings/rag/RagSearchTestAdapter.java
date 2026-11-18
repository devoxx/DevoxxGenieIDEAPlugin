package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.service.rag.SearchResult;
import com.devoxx.genie.service.rag.SemanticSearchService;
import com.devoxx.genie.ui.settings.common.SearchTestOutcome;
import com.devoxx.genie.ui.settings.common.SearchTestPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Settings → RAG "Test semantic search": searches the current project's index with the saved RAG
 * settings. One row per match (score, file name, folder); the selected match's chunk is previewed
 * and double-clicking opens the file at that chunk.
 */
final class RagSearchTestAdapter implements SearchTestPanel.Adapter<SearchResult> {

    private final Project project;

    RagSearchTestAdapter(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @NotNull String placeholder() {
        return "e.g. where are the chat action buttons created";
    }

    @Override
    public @NotNull String busyMessage() {
        return "Searching the project index…";
    }

    @Override
    public @NotNull Supplier<SearchTestOutcome<SearchResult>> prepare(@NotNull String query) {
        return () -> {
            List<SearchResult> results = SemanticSearchService.getInstance().search(project, query);
            return results.isEmpty()
                    ? SearchTestOutcome.empty(noMatchesMessage(query))
                    : SearchTestOutcome.results(summary(query, results.size()), results);
        };
    }

    /** RAG errors come from the local index and embedding setup, so their message is useful. */
    @Override
    public @NotNull String failureMessage(@NotNull Throwable error) {
        return "Search failed: " + (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
    }

    @Override
    public void renderRow(@NotNull SimpleColoredComponent row, @NotNull SearchResult match) {
        String path = relativePath(project.getBasePath(), match.filePath());
        int slash = path.lastIndexOf('/');
        row.append(formatScore(match.score()) + "   ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
        row.append(path.substring(slash + 1), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        if (slash > 0) {
            row.append("   " + path.substring(0, slash), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }

    @Override
    public @Nullable String rowTooltip(@NotNull SearchResult match) {
        return match.filePath();
    }

    @Override
    public @NotNull String previewTitle(@NotNull SearchResult match) {
        return relativePath(project.getBasePath(), match.filePath());
    }

    @Override
    public @NotNull String previewText(@NotNull SearchResult match) {
        return match.content() == null ? "" : match.content();
    }

    @Override
    public boolean monospacePreview() {
        return true;
    }

    @Override
    public int previewRows() {
        return 12;
    }

    @Override
    public @NotNull String openHint() {
        return "Double-click to open in the editor (it opens behind Settings)";
    }

    @Override
    public void open(@NotNull SearchResult match) {
        VirtualFile file = match.filePath() == null ? null
                : LocalFileSystem.getInstance().findFileByPath(match.filePath().replace('\\', '/'));
        if (file == null || !file.isValid()) {
            NotificationUtil.sendNotification(project,
                    "Cannot open " + match.filePath() + ": the file no longer exists. Re-index the project to refresh RAG results.");
            return;
        }
        int offset = ApplicationManager.getApplication().runReadAction((Computable<Integer>) () -> {
            Document document = FileDocumentManager.getInstance().getDocument(file);
            return document == null ? 0 : chunkOffset(document.getCharsSequence(), match.content());
        });
        new OpenFileDescriptor(project, file, offset).navigate(true);
    }

    /**
     * Where the chunk starts in the file: the whole chunk if it is still there, else its first
     * substantial line (imports and annotations are too common to locate it); 0 when the file has
     * changed since indexing and neither is found.
     */
    static int chunkOffset(@NotNull CharSequence fileText, @Nullable String chunk) {
        if (chunk == null || chunk.isBlank()) return 0;
        String text = fileText.toString();
        int whole = text.indexOf(chunk.strip());
        if (whole >= 0) return whole;
        for (String line : chunk.split("\\R")) {
            String needle = line.strip();
            if (needle.length() >= 20 && !needle.startsWith("import ") && !needle.startsWith("@")) {
                return Math.max(text.indexOf(needle), 0);
            }
        }
        return 0;
    }

    static @NotNull String summary(@NotNull String query, int count) {
        return count + (count == 1 ? " match" : " matches") + " for \"" + query + "\", best first";
    }

    static @NotNull String noMatchesMessage(@NotNull String query) {
        return "No matches for \"" + query + "\". Check that this project is indexed, or lower the minimum score.";
    }

    static @NotNull String formatScore(@Nullable Double score) {
        return score == null ? "-" : String.format(Locale.ROOT, "%.2f", score);
    }

    /** The path below the project root, or the whole path for a file outside the project. */
    static @NotNull String relativePath(@Nullable String basePath, @Nullable String filePath) {
        if (filePath == null) return "";
        String file = filePath.replace('\\', '/');
        if (basePath == null) return file;
        String base = basePath.replace('\\', '/');
        String prefix = base.endsWith("/") ? base : base + "/";
        return file.startsWith(prefix) ? file.substring(prefix.length()) : file;
    }
}
