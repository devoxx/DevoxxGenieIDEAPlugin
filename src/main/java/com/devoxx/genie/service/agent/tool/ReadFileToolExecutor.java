package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

@Slf4j
public class ReadFileToolExecutor implements ToolExecutor {

    private final Project project;

    public ReadFileToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String path = ToolArgumentParser.getString(request.arguments(), "path");
            if (path == null || path.isBlank()) {
                return "Error: 'path' parameter is required.";
            }

            return ReadAction.compute(() -> readFile(path));
        } catch (Exception e) {
            log.error("Error reading file", e);
            return "Error: Failed to read file - " + e.getMessage();
        }
    }

    @NotNull String readFile(@NotNull String path) {
        try {
            VirtualFile projectBase = getProjectBaseDir();
            if (projectBase == null) {
                return "Error: Project base directory not found.";
            }

            VirtualFile file = findFile(projectBase, path);
            if (file == null) {
                return "Error: File not found: " + path;
            }

            if (!isAncestor(projectBase, file)) {
                return "Error: Access denied - path is outside the project root.";
            }

            if (file.isDirectory()) {
                return "Error: Path is a directory, not a file: " + path;
            }

            byte[] content = file.contentsToByteArray();
            return new String(content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error reading file content", e);
            return "Error: Failed to read file - " + e.getMessage();
        }
    }

    VirtualFile getProjectBaseDir() {
        VirtualFile[] contentRoots = ProjectRootManager.getInstance(project).getContentRoots();
        return contentRoots.length > 0 ? contentRoots[0] : null;
    }

    VirtualFile findFile(VirtualFile projectBase, String path) {
        return projectBase.findFileByRelativePath(path);
    }

    boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) {
        return VfsUtilCore.isAncestor(ancestor, descendant, false);
    }
}
