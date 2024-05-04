package com.devoxx.genie.ui.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;

import javax.swing.*;

import static com.devoxx.genie.action.AddSnippetAction.CODE_SNIPPET;
import static com.devoxx.genie.ui.util.DevoxxGenieIcons.*;

/**
 * Utility class for getting the icon for a file type
 *
 * @link <a href="https://jetbrains.design/intellij/resources/icons_list">IDEA icons</a>
 */
public class FileTypeIconUtil {

    private FileTypeIconUtil() {
    }

    /**
     * Returns the icon for the file type, currently only Java files are supported
     *
     * @param project     The current project
     * @param virtualFile The file
     * @return The icon
     */
    public static Icon getFileTypeIcon(Project project, VirtualFile virtualFile) {
        final Icon[] icon = new Icon[1];
        ApplicationManager.getApplication().invokeAndWait(() -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (psiFile instanceof PsiJavaFile javaFile) {
                PsiClass[] psiClasses = javaFile.getClasses();
                if (psiClasses.length > 0) {
                    // Assuming the file contains only one top-level class
                    PsiClass psiClass = psiClasses[0];
                    if (psiClass.isInterface()) {
                        icon[0] = InterfaceIcon;
                    } else if (psiClass.isEnum()) {
                        icon[0] = EnumIcon;
                    } else {
                        icon[0] = ClassIcon;
                    }
                }
            } else {
                if (virtualFile.getFileType().getName().equals("UNKNOWN")) {
                    icon[0] = CodeSnippetIcon;
                } else {
                    icon[0] = ClassIcon;
                }
            }
        });
        return icon[0];
    }
}
