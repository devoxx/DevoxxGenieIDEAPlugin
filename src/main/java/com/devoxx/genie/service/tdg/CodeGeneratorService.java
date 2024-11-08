package com.devoxx.genie.service.tdg;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CodeGeneratorService {

    public static void createClassFromCodeSnippet(@NotNull ChatMessageContext chatMessageContext,
                                                  String selectedText) {
        Project project = chatMessageContext.getProject();

        new Task.Backgroundable(project, "Creating java class", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(true);
                    indicator.setText("Parsing code...");

                    CodeContainer codeContainer = new CodeContainer(selectedText);
                    String packageName = codeContainer.getPackageName();
                    String fileName = codeContainer.getFileName();

                    indicator.setText("Creating class file...");

                    ApplicationManager.getApplication().invokeAndWait(() ->
                        ApplicationManager.getApplication().runWriteAction(() ->
                            createFile(packageName, fileName, project, selectedText)),
                        ModalityState.defaultModalityState());

                } catch (Exception e) {
                    NotificationUtil.sendNotification(project,
                        "Error creating class: " + e.getMessage());
                }
            }
        }.queue();
    }

    private static void createFile(String packageName,
                                   String fileName,
                                   Project project,
                                   String selectedText) {
        try {
            // Find the proper source root for Java files
            VirtualFile sourceRoot = findSourceRoot(project);
            if (sourceRoot == null) {
                NotificationUtil.sendNotification(project,
                    "Error: Could not find source root directory");
                return;
            }

            VirtualFile packageDir = createPackageDirectories(sourceRoot, packageName);
            VirtualFile existingFile = packageDir.findChild(fileName);
            VirtualFile javaFile;

            if (existingFile != null) {
                existingFile.setBinaryContent(
                    selectedText.getBytes(StandardCharsets.UTF_8));
                javaFile = existingFile;
                NotificationUtil.sendNotification(project,
                    "Class updated successfully");
            } else {
                javaFile = packageDir.createChildData(null, fileName);
                javaFile.setBinaryContent(
                    selectedText.getBytes(StandardCharsets.UTF_8));
                NotificationUtil.sendNotification(project,
                    "Class created successfully");
            }

            FileEditorManager.getInstance(project).openFile(javaFile, true);

        } catch (IOException e) {
            NotificationUtil.sendNotification(project,
                "Error creating class: " + e.getMessage());
        }
    }

    private static @Nullable VirtualFile findSourceRoot(Project project) {
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        for (Module module : moduleManager.getModules()) {
            ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
            // Get source roots for the module
            for (VirtualFile root : rootManager.getSourceRoots(false)) {
                // Look for the main source root, typically ending with "src/main/java"
                if (root.getPath().endsWith("src/main/java")) {
                    return root;
                }
            }
            // Fallback to first source root if we can't find main/java
            VirtualFile[] sourceRoots = rootManager.getSourceRoots(false);
            if (sourceRoots.length > 0) {
                return sourceRoots[0];
            }
        }
        return null;
    }

    private static VirtualFile createPackageDirectories(@NotNull VirtualFile sourceRoot,
                                                        @NotNull String packageName) throws IOException {
        if (packageName.isEmpty()) {
            return sourceRoot;
        }

        VirtualFile currentDir = sourceRoot;
        for (String part : packageName.split("\\.")) {
            VirtualFile subDir = currentDir.findChild(part);
            if (subDir == null) {
                subDir = currentDir.createChildDirectory(null, part);
            }
            currentDir = subDir;
        }
        return currentDir;
    }
}
