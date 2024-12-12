package com.devoxx.genie.ui.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
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

    private static final Logger LOG = Logger.getInstance(FileTypeIconUtil.class);

    private FileTypeIconUtil() {
    }

    public static Icon getFileTypeIcon(VirtualFile virtualFile) {
        Future<Icon> iconFuture = AppExecutorUtil.getAppExecutorService().submit(() ->
                ApplicationManager.getApplication().runReadAction((Computable<Icon>) () -> {
                    Icon interfaceIcon = getIcon(virtualFile);
                    if (interfaceIcon != null) {
                        LOG.debug("Found icon for file: " + virtualFile.getPath());
                        return interfaceIcon;
                    }
                    String fileTypeName = virtualFile.getFileType().getName();
                    LOG.debug("Using default icon for file type: " + fileTypeName);
                    return fileTypeName.equals("UNKNOWN") ? CodeSnippetIcon : ClassIcon;
                }));

        try {
            return iconFuture.get(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error getting icon for file: " + virtualFile.getPath(), e);
            return ClassIcon;
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
                LOG.error("Error reading file content: " + virtualFile.getPath(), e);
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
