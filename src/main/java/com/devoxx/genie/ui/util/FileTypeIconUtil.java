package com.devoxx.genie.ui.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.*;

public class FileTypeIconUtil {

    private FileTypeIconUtil() {
    }

    public static Icon getFileTypeIcon(VirtualFile virtualFile) {
        Future<Icon> iconFuture = AppExecutorUtil.getAppExecutorService().submit(() ->
            ApplicationManager.getApplication().runReadAction((Computable<Icon>) () -> {
                Icon interfaceIcon = getIcon(virtualFile);
                if (interfaceIcon != null) return interfaceIcon;
                return virtualFile.getFileType().getName().equals("UNKNOWN") ? CodeSnippetIcon : ClassIcon;
            }));

        try {
            return iconFuture.get(100, TimeUnit.MILLISECONDS); // Adjust timeout as needed
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // Log the error if needed
            return ClassIcon; // Return a default icon in case of any error
        }
    }

    private static @Nullable Icon getIcon(VirtualFile virtualFile) {
        if (virtualFile != null && virtualFile.getExtension() != null) {
            try {
                if (virtualFile.getExtension().equalsIgnoreCase("java")) {
                    String content = new String(virtualFile.contentsToByteArray());
                    if (content.contains(" interface ")) {
                        return InterfaceIcon;
                    } else if (content.contains(" enum ")) {
                        return EnumIcon;
                    } else if (content.contains(" class ")) {
                        return ClassIcon;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
