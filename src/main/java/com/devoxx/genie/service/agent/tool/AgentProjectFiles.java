package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public final class AgentProjectFiles {

    private AgentProjectFiles() {
    }

    public static @Nullable VirtualFile root(@NotNull Project project) {
        VirtualFile guessed = ProjectUtil.guessProjectDir(project);
        VirtualFile opened = openedFolder(project);
        return isModuleRootInside(guessed, opened) ? opened : guessed;
    }

    static @NotNull String relativeToRoot(@NotNull VirtualFile root, @NotNull String path) {
        Path candidate;
        try {
            candidate = Path.of(path);
        } catch (InvalidPathException e) {
            return path;
        }
        if (!candidate.isAbsolute()) {
            return path;
        }
        Path rootPath = Path.of(root.getPath());
        Path normalized = candidate.normalize();
        if (!normalized.startsWith(rootPath)) {
            return path;
        }
        return FileUtil.toSystemIndependentName(rootPath.relativize(normalized).toString());
    }

    private static @Nullable VirtualFile openedFolder(@NotNull Project project) {
        String basePath = project.getBasePath();
        return basePath == null ? null : LocalFileSystem.getInstance().findFileByPath(basePath);
    }

    private static boolean isModuleRootInside(@Nullable VirtualFile guessed, @Nullable VirtualFile opened) {
        return guessed != null && opened != null && VfsUtilCore.isAncestor(opened, guessed, true);
    }
}
