package com.devoxx.genie.service;

import com.devoxx.genie.service.settings.SettingsStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.intellij.openapi.application.ActionsKt.runReadAction;

public class PSIAnalyzerService {

    public static PSIAnalyzerService getInstance() {
        return ApplicationManager.getApplication().getService(PSIAnalyzerService.class);
    }

    /**
     * Analyze the PSI tree of a file and return a list of related classes.
     * @param project the project
     * @param file the virtual file
     * @return list of PSI classes
     */
    public Optional<Set<PsiClass>> analyze(Project project, VirtualFile file) {

        Set<PsiClass> relatedClasses = new HashSet<>();

        PsiFile psiFile = runReadAction(() -> PsiManager.getInstance(project).findFile(file));
        if (!(psiFile instanceof PsiJavaFile javaFile)) {
            return Optional.empty();
        }

        for (PsiClass psiClass : runReadAction(javaFile::getClasses)) {
            if (SettingsStateService.getInstance().getAstParentClass()) {
                extractBaseClass(psiClass, relatedClasses);
            }
            if (SettingsStateService.getInstance().getAstClassReference()) {
                extractReferenceClasses(psiClass, relatedClasses);
            }
            if (SettingsStateService.getInstance().getAstFieldReference()) {
                PsiField[] fields = runReadAction(psiClass::getFields);
                extractPSIFields(fields, relatedClasses);
            }
        }

        return Optional.of(relatedClasses);
    }

    /**
     * Extract the base class of a PsiClass and add it to the list of related classes.
     * @param psiClass the PsiClass
     * @param relatedClasses the list of related classes
     */
    private void extractBaseClass(@NotNull PsiClass psiClass, Set<PsiClass> relatedClasses) {
        runReadAction(() -> {
            InheritanceUtil.getSuperClasses(psiClass, relatedClasses, false);
            return null;
        });
    }

    /**
     * Extract all referenced classes in a PsiClass and add them to the list of related classes.
     * @param psiClass the PsiClass
     * @param relatedClasses the list of related classes
     */
    private void extractReferenceClasses(PsiClass psiClass, Set<PsiClass> relatedClasses) {
        PsiClass[] referencedClasses = PsiTreeUtil.getChildrenOfType(psiClass, PsiClass.class);
        if (referencedClasses != null) {
            Collections.addAll(relatedClasses, referencedClasses);
        }
    }

    /**
     * Extract all fields in a PsiClass and add the corresponding classes to the list of related classes.
     * @param psiFields     the PsiFields
     * @param relatedClasses the list of related classes
     */
    private void extractPSIFields(@NotNull PsiField @NotNull [] psiFields,
                                  Set<PsiClass> relatedClasses) {
        for (PsiField field : psiFields) {
            // Get the type of the field
            PsiType fieldType = runReadAction(field::getType);
            // If the type is a class type, add the corresponding PsiClass to the list
            if (fieldType instanceof PsiClassType) {
                PsiClass fieldClass = runReadAction(() -> ((PsiClassType) fieldType).resolve());
                if (fieldClass != null) {
                    relatedClasses.add(fieldClass);
                }
            }
        }
    }
}
