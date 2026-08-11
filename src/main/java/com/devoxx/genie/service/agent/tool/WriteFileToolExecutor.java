package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.service.agent.AgentFileChangeTracker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class WriteFileToolExecutor implements ToolExecutor {

    private final Project project;

    public WriteFileToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String path = ToolArgumentParser.getString(request.arguments(), "path");
            String content = ToolArgumentParser.getString(request.arguments(), "content");

            if (path == null || path.isBlank()) {
                return "Error: 'path' parameter is required.";
            }
            if (content == null) {
                return "Error: 'content' parameter is required.";
            }
            if (path.contains("..")) {
                return "Error: Access denied - path traversal is not allowed.";
            }

            AtomicReference<String> result = new AtomicReference<>();
            ApplicationManager.getApplication().invokeAndWait(() ->
                WriteCommandAction.runWriteCommandAction(project, () ->
                    result.set(writeFile(path, content))
                )
            );
            return result.get();
        } catch (Exception e) {
            log.error("Error writing file", e);
            return "Error: Failed to write file - " + e.getMessage();
        }
    }

    @NotNull String writeFile(@NotNull String path, @NotNull String content) {
        try {
            VirtualFile projectBase = getProjectBaseDir();
            if (projectBase == null) {
                return "Error: Project base directory not found.";
            }

            VirtualFile parentDir = resolveParentDir(path, projectBase);
            if (parentDir == null) {
                return "Error: Failed to create parent directories for: " + path;
            }
            if (!isAncestor(projectBase, parentDir)) {
                return "Error: Access denied - path is outside the project root.";
            }

            String fileName = extractFileName(path);
            VirtualFile file = parentDir.findChild(fileName);
            boolean isNewFile = file == null;
            if (isNewFile) {
                file = parentDir.createChildData(this, fileName);
            }

            // Snapshot for the post-run change review (issue #705). A new file has no "before",
            // which the tracker renders as a diff against an empty side.
            recordChange(path, file, isNewFile ? null : readExistingContent(file));

            file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
            return "Successfully wrote " + content.length() + " characters to " + path;
        } catch (Exception e) {
            log.error("Error in write command action", e);
            return "Error: Failed to write file - " + e.getMessage();
        }
    }

    /** Reads the content a write is about to replace; null when it cannot be read. */
    @Nullable String readExistingContent(@NotNull VirtualFile file) {
        try {
            return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("Could not read existing content of {}", file.getPath(), e);
            return null;
        }
    }

    /**
     * Hands the pre-write content to the change tracker. Never fails the write: the review
     * panel is a convenience, so a tracker problem must not stop the agent from working.
     */
    void recordChange(@NotNull String path, @NotNull VirtualFile file, @Nullable String rawContent) {
        try {
            AgentFileChangeTracker.getInstance(project).recordBeforeWrite(path, file, rawContent);
        } catch (Exception e) {
            log.debug("Could not record agent file change for {}", path, e);
        }
    }

    VirtualFile getProjectBaseDir() {
        return ProjectUtil.guessProjectDir(project);
    }

    boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) {
        return VfsUtilCore.isAncestor(ancestor, descendant, false);
    }

    VirtualFile resolveParentDir(@NotNull String path, @NotNull VirtualFile projectBase)
            throws IOException {
        if (!path.contains("/")) {
            return projectBase;
        }
        String parentPath = path.substring(0, path.lastIndexOf('/'));
        return VfsUtil.createDirectoryIfMissing(projectBase, parentPath);
    }

    static @NotNull String extractFileName(@NotNull String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
