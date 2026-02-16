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

            String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);

            // Count occurrences
            int count = countOccurrences(content, oldString);
            if (count == 0) {
                return "Error: The specified old_string was not found in " + path;
            }
            if (count > 1 && !replaceAll) {
                return "Error: Found " + count + " occurrences of old_string in " + path +
                        ". Provide more surrounding context to make the match unique, " +
                        "or set replace_all to true.";
            }

            // Perform replacement
            String newContent;
            if (replaceAll) {
                newContent = content.replace(oldString, newString);
            } else {
                int idx = content.indexOf(oldString);
                newContent = content.substring(0, idx) + newString +
                        content.substring(idx + oldString.length());
            }

            file.setBinaryContent(newContent.getBytes(StandardCharsets.UTF_8));

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
