package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.devoxx.genie.service.agent.tool.EditFileToolExecutor.countOccurrences;
import static com.devoxx.genie.service.agent.tool.EditFileToolExecutor.normalizeLineEndings;

/**
 * Builds a before/after preview for the file-mutating agent tools so the approval dialog can
 * render a real diff instead of a raw JSON argument dump (issue #705).
 *
 * <p>The file resolution and replacement logic mirror {@link EditFileToolExecutor} and
 * {@link WriteFileToolExecutor} exactly, so what the user approves is what actually gets
 * written. Both sides of the preview are line-ending normalized: the executor restores the
 * file's original separator on write, so comparing raw content against normalized content
 * would mark every line of a CRLF file as changed.
 *
 * <p>Any tool this does not understand, and any condition under which the edit would fail
 * (file missing, {@code old_string} not found or ambiguous, malformed arguments), yields an
 * empty result. The approval dialog then falls back to showing the raw arguments — a preview
 * is a convenience and must never block approval.
 */
@Slf4j
public class AgentDiffPreviewFactory {

    /** A previewable change: the file's current content versus what the tool would write. */
    public record DiffPreview(@NotNull String path, @NotNull String before, @NotNull String after) {
    }

    private final Project project;

    public AgentDiffPreviewFactory(@NotNull Project project) {
        this.project = project;
    }

    /**
     * Builds the preview for a tool call. Must be called under a read action — it touches VFS.
     *
     * @return the preview, or empty when the tool is not previewable or the change cannot be resolved
     */
    public @NotNull Optional<DiffPreview> create(@NotNull String toolName, @Nullable String arguments) {
        if (arguments == null) {
            return Optional.empty();
        }
        try {
            return switch (toolName) {
                case "edit_file" -> createEditPreview(arguments);
                case "write_file" -> createWritePreview(arguments);
                default -> Optional.empty();
            };
        } catch (Exception e) {
            log.debug("Could not build diff preview for tool {}", toolName, e);
            return Optional.empty();
        }
    }

    private @NotNull Optional<DiffPreview> createEditPreview(@NotNull String arguments) {
        String path = ToolArgumentParser.getString(arguments, "path");
        String oldString = ToolArgumentParser.getString(arguments, "old_string");
        String newString = ToolArgumentParser.getString(arguments, "new_string");
        boolean replaceAll = ToolArgumentParser.getBoolean(arguments, "replace_all", false);

        if (path == null || path.isBlank() || oldString == null || oldString.isEmpty() || newString == null) {
            return Optional.empty();
        }

        String before = readContent(path);
        if (before == null) {
            return Optional.empty();
        }

        String normalizedOld = normalizeLineEndings(oldString);
        String normalizedNew = normalizeLineEndings(newString);

        // Mirror the executor's guard rails: if the edit would be rejected there, there is
        // nothing meaningful to preview.
        int count = countOccurrences(before, normalizedOld);
        if (count == 0 || (count > 1 && !replaceAll)) {
            return Optional.empty();
        }

        String after;
        if (replaceAll) {
            after = before.replace(normalizedOld, normalizedNew);
        } else {
            int idx = before.indexOf(normalizedOld);
            after = before.substring(0, idx) + normalizedNew
                    + before.substring(idx + normalizedOld.length());
        }

        return Optional.of(new DiffPreview(path, before, after));
    }

    private @NotNull Optional<DiffPreview> createWritePreview(@NotNull String arguments) {
        String path = ToolArgumentParser.getString(arguments, "path");
        String content = ToolArgumentParser.getString(arguments, "content");

        if (path == null || path.isBlank() || content == null) {
            return Optional.empty();
        }

        // A missing file is the normal "create new file" case: diff against an empty side.
        String before = readContent(path);
        return Optional.of(new DiffPreview(path, before == null ? "" : before,
                normalizeLineEndings(content)));
    }

    /**
     * Reads the file's current content, normalized to LF. Returns null when the path does not
     * resolve to a readable file inside the project.
     */
    private @Nullable String readContent(@NotNull String path) {
        if (path.contains("..")) {
            return null;
        }

        VirtualFile projectBase = getProjectBaseDir();
        if (projectBase == null) {
            return null;
        }

        VirtualFile file = findFile(projectBase, path);
        if (file == null || !file.exists() || file.isDirectory() || !isAncestor(projectBase, file)) {
            return null;
        }

        try {
            return normalizeLineEndings(new String(file.contentsToByteArray(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.debug("Could not read {} for diff preview", path, e);
            return null;
        }
    }

    VirtualFile getProjectBaseDir() {
        return ProjectUtil.guessProjectDir(project);
    }

    VirtualFile findFile(VirtualFile projectBase, String path) {
        return projectBase.findFileByRelativePath(path);
    }

    boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) {
        return VfsUtilCore.isAncestor(ancestor, descendant, false);
    }
}
