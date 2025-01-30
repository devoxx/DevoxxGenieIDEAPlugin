package com.devoxx.genie.util;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class ImageUtil {

    public static boolean isImageFile(@NotNull VirtualFile file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") ||
                name.endsWith(".jpeg") ||
                name.endsWith(".png") ||
                name.endsWith(".gif") ||
                name.endsWith(".bmp");
    }

    public static @NotNull String getImageMimeType(@NotNull VirtualFile file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (name.endsWith(".png")) {
            return "image/png";
        } else if (name.endsWith(".gif")) {
            return "image/gif";
        } else if (name.endsWith(".bmp")) {
            return "image/bmp";
        } else {
            return "image";
        }
    }
}
