package com.devoxx.genie.util;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class FileTypeUtil {

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
}
