package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static com.intellij.openapi.vfs.VfsUtilCore.getRelativePath;
import static com.intellij.openapi.vfs.VfsUtilCore.isAncestor;

@Slf4j
public class ListFilesToolExecutor implements ToolExecutor {

    private static final int MAX_ENTRIES = 500;
    private static final Set<String> SKIP_DIRS = Set.of(
            ".git", "node_modules", "build", "out", "target", ".idea", "bin", ".gradle"
    );

    private final Project project;

    public ListFilesToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String path = ToolArgumentParser.getString(request.arguments(), "path");
            boolean recursive = ToolArgumentParser.getBoolean(request.arguments(), "recursive", false);

            return ReadAction.compute(() -> listFiles(path, recursive));
        } catch (Exception e) {
            log.error("Error listing files", e);
            return "Error: Failed to list files - " + e.getMessage();
        }
    }

    private @NotNull String listFiles(String path, boolean recursive) {
        VirtualFile projectBase = ProjectUtil.guessProjectDir(project);
        if (projectBase == null) {
            return "Error: Project base directory not found.";
        }

        VirtualFile targetDir = resolveTargetDir(path, projectBase);
        if (targetDir == null) {
            return "Error: Directory not found: " + path;
        }
        if (!targetDir.isDirectory()) {
            return "Error: Path is not a directory: " + path;
        }
        if (!isAncestor(projectBase, targetDir, false)) {
            return "Error: Access denied - path is outside the project root.";
        }

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        listDirectory(targetDir, projectBase, recursive, result, count);

        if (count[0] >= MAX_ENTRIES) {
            result.append("\n... (truncated, showing first ").append(MAX_ENTRIES).append(" entries)");
        }

        return result.toString();
    }

    private static VirtualFile resolveTargetDir(String path, @NotNull VirtualFile projectBase) {
        if (path == null || path.isBlank() || path.equals(".")) {
            return projectBase;
        }
        return projectBase.findFileByRelativePath(path);
    }

    private void listDirectory(VirtualFile dir, VirtualFile projectBase, boolean recursive,
                               StringBuilder result, int[] count) {
        VirtualFile[] children = dir.getChildren();
        if (children == null) return;

        for (VirtualFile child : children) {
            if (count[0] >= MAX_ENTRIES) return;

            String relativePath = getRelativePath(child, projectBase);
            if (relativePath == null) continue;

            if (child.isDirectory()) {
                appendDirectory(child, relativePath, projectBase, recursive, result, count);
            } else {
                result.append("[FILE] ").append(relativePath).append("\n");
                count[0]++;
            }
        }
    }

    private void appendDirectory(VirtualFile child, String relativePath, VirtualFile projectBase,
                                 boolean recursive, StringBuilder result, int[] count) {
        if (SKIP_DIRS.contains(child.getName())) return;

        result.append("[DIR]  ").append(relativePath).append("\n");
        count[0]++;
        if (recursive) {
            listDirectory(child, projectBase, true, result, count);
        }
    }
}
