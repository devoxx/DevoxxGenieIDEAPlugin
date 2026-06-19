package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class EditFileToolExecutor implements ToolExecutor {

    private final Project project;

    public EditFileToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String path = ToolArgumentParser.getString(request.arguments(), "path");
            String oldString = ToolArgumentParser.getString(request.arguments(), "old_string");
            String newString = ToolArgumentParser.getString(request.arguments(), "new_string");
            boolean replaceAll = ToolArgumentParser.getBoolean(request.arguments(), "replace_all", false);

            if (path == null || path.isBlank()) {
                return "Error: 'path' parameter is required.";
            }
            if (oldString == null) {
                return "Error: 'old_string' parameter is required.";
            }
            if (oldString.isEmpty()) {
                return "Error: 'old_string' must not be empty.";
            }
            if (newString == null) {
                return "Error: 'new_string' parameter is required.";
            }
            if (oldString.equals(newString)) {
                return "Error: 'old_string' and 'new_string' are identical — no change needed.";
            }

            // Reject obvious path traversal
            if (path.contains("..")) {
                return "Error: Access denied - path traversal is not allowed.";
            }

            AtomicReference<String> result = new AtomicReference<>();

            ApplicationManager.getApplication().invokeAndWait(() ->
                WriteCommandAction.runWriteCommandAction(project, () ->
                    result.set(editFile(path, oldString, newString, replaceAll))
                )
            );

            return result.get();
        } catch (Exception e) {
            log.error("Error editing file", e);
            return "Error: Failed to edit file - " + e.getMessage();
        }
    }

    @NotNull String editFile(@NotNull String path, @NotNull String oldString,
                             @NotNull String newString, boolean replaceAll) {
        try {
            VirtualFile projectBase = getProjectBaseDir();
            if (projectBase == null) {
                return "Error: Project base directory not found.";
            }

            VirtualFile file = findFile(projectBase, path);
            if (file == null || !file.exists()) {
                return "Error: File not found: " + path;
            }
            if (file.isDirectory()) {
                return "Error: Path is a directory, not a file: " + path;
            }

            // Security check
            if (!isAncestor(projectBase, file)) {
                return "Error: Access denied - path is outside the project root.";
            }

            String rawContent = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);

            // Normalize line endings to LF before matching. LLMs emit old_string with LF
            // even when the file on disk uses CRLF (typical on Windows), which would
            // otherwise make any multi-line old_string impossible to match (issue #1144).
            // We detect the file's original separator so we can restore it on write and
            // avoid silently rewriting the whole file's line endings.
            String lineSeparator = detectLineSeparator(rawContent);
            String content = normalizeLineEndings(rawContent);
            String normalizedOld = normalizeLineEndings(oldString);
            String normalizedNew = normalizeLineEndings(newString);

            // Count occurrences
            int count = countOccurrences(content, normalizedOld);
            if (count == 0) {
                return "Error: The specified old_string was not found in " + path;
            }
            if (count > 1 && !replaceAll) {
                return "Error: Found " + count + " occurrences of old_string in " + path +
                        ". Provide more surrounding context to make the match unique, " +
                        "or set replace_all to true.";
            }

            // Perform replacement (in LF space)
            String newContent;
            if (replaceAll) {
                newContent = content.replace(normalizedOld, normalizedNew);
            } else {
                int idx = content.indexOf(normalizedOld);
                newContent = content.substring(0, idx) + normalizedNew +
                        content.substring(idx + normalizedOld.length());
            }

            // Restore the file's original line separator before writing.
            String outContent = "\n".equals(lineSeparator)
                    ? newContent
                    : newContent.replace("\n", lineSeparator);

            file.setBinaryContent(outContent.getBytes(StandardCharsets.UTF_8));

            if (replaceAll && count > 1) {
                return "Successfully replaced " + count + " occurrences in " + path;
            } else {
                return "Successfully edited " + path;
            }
        } catch (Exception e) {
            log.error("Error in edit command action", e);
            return "Error: Failed to edit file - " + e.getMessage();
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

    /** Converts CRLF and lone CR line endings to LF so matching is line-ending agnostic. */
    static @NotNull String normalizeLineEndings(@NotNull String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n');
    }

    /**
     * Detects the dominant line separator used by the file content so it can be preserved
     * on write. Returns "\r\n" if any CRLF is present, "\r" for lone CR (classic Mac),
     * otherwise "\n".
     */
    static @NotNull String detectLineSeparator(@NotNull String content) {
        if (content.contains("\r\n")) {
            return "\r\n";
        }
        if (content.indexOf('\r') != -1) {
            return "\r";
        }
        return "\n";
    }

    static int countOccurrences(@NotNull String content, @NotNull String search) {
        int count = 0;
        int idx = 0;
        while ((idx = content.indexOf(search, idx)) != -1) {
            count++;
            idx += search.length();
        }
        return count;
    }
}
