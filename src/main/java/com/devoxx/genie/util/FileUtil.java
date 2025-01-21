package com.devoxx.genie.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class FileUtil {

    /**
     * Get the file type of the file which represents the programming language (if any) of the file.
     *
     * @param virtualFile the virtual file
     * @return the file type
     */
    public static @NotNull String getFileType(VirtualFile virtualFile) {
        if (virtualFile != null) {
            return virtualFile.getFileType().getName();
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Get the relative path of the file from the project base path.
     * @param project the project
     * @param file the file
     * @return the relative path
     */
    public static String getRelativePath(@NotNull Project project, @NotNull VirtualFile file) {
        String projectBasePath = project.getBasePath();
        String filePath = file.getPath();

        if (projectBasePath != null && filePath.startsWith(projectBasePath)) {
            String relativePath = filePath.substring(projectBasePath.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            return relativePath;
        }

        return filePath;
    }
}
