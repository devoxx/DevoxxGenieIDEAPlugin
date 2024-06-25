package com.devoxx.genie.ui.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.devoxx.genie.ui.util.DevoxxGenieIcons.*;

public class FileTypeIconUtil {

    private FileTypeIconUtil() {
    }

    public static Icon getFileTypeIcon(Project project, VirtualFile virtualFile) {
        Future<Icon> iconFuture = AppExecutorUtil.getAppExecutorService().submit(() ->
            ApplicationManager.getApplication().runReadAction((Computable<Icon>) () -> {
                Icon interfaceIcon = getIcon(project, virtualFile);
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

    private static @Nullable Icon getIcon(Project project, VirtualFile virtualFile) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile instanceof PsiJavaFile javaFile) {
            PsiClass[] psiClasses = javaFile.getClasses();
            if (psiClasses.length > 0) {
                PsiClass psiClass = psiClasses[0];
                if (psiClass.isInterface()) {
                    return InterfaceIcon;
                } else if (psiClass.isEnum()) {
                    return EnumIcon;
                } else {
                    return ClassIcon;
                }
            }
        }
        return null;
    }
}
