package com.devoxx.genie.service;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.application.ReadAction;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.concurrent.CompletableFuture;

import com.knuddels.jtokkit.Encodings;

public class ProjectScannerService {

    private static final Encoding ENCODING = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    public static ProjectScannerService getInstance() {
        return ApplicationManager.getApplication().getService(ProjectScannerService.class);
    }

    /**
     * Scan the project and return the project source tree and file contents.
     * @param project the project
     * @param maxTokens the maximum number of tokens
     * @param isTokenCalculation whether the scan is for token calculation
     * @return the project context
     */
    public CompletableFuture<String> scanProject(Project project, int maxTokens, boolean isTokenCalculation) {
        CompletableFuture<String> future = new CompletableFuture<>();

        ReadAction.nonBlocking(() -> {
                StringBuilder result = new StringBuilder();
                result.append("Project Structure:\n");
                result.append(generateSourceTree(project));
                result.append("\n\nFile Contents:\n");

                ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
                VirtualFile projectDir = project.getBaseDir();

                StringBuilder fullContent = new StringBuilder(result);

                VfsUtilCore.visitChildrenRecursively(projectDir, new VirtualFileVisitor<Void>() {
                    @Override
                    public boolean visitFile(@NotNull VirtualFile file) {
                        if (shouldExcludeDirectory(file)) {
                            return false;
                        }
                        if (fileIndex.isInContent(file) && shouldIncludeFile(file)) {
                            String header = "\n--- " + file.getPath() + " ---\n";
                            fullContent.append(header);

                            try {
                                String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
                                fullContent.append(content).append("\n");
                            } catch (Exception e) {
                                String errorMsg = "Error reading file: " + e.getMessage() + "\n";
                                fullContent.append(errorMsg);
                            }
                        }
                        return true;
                    }
                });

                return truncateToTokens(project, fullContent.toString(), maxTokens, isTokenCalculation);
            }).inSmartMode(project)
            .finishOnUiThread(ModalityState.defaultModalityState(), future::complete)
            .submit(AppExecutorUtil.getAppExecutorService());

        return future;
    }

    /**
     * Truncate the project context to a maximum number of tokens.
     * If the project context exceeds the limit, truncate it and append a message.
     * @param project the project
     * @param text the project context
     * @param maxTokens the maximum number of tokens
     * @param isTokenCalculation whether the scan is for token calculation
     */
    private String truncateToTokens(Project project,
                                    String text,
                                    int maxTokens,
                                    boolean isTokenCalculation) {
        NumberFormat formatter = NumberFormat.getInstance();
        IntArrayList tokens = ENCODING.encode(text);
        if (tokens.size() <= maxTokens) {
            if (!isTokenCalculation) {
                NotificationUtil.sendNotification(project, "Added. Project context " + formatter.format(tokens.size()) + " tokens is within window context limit of " +
                    formatter.format(maxTokens) + " tokens");
            }
            return text;
        }
        IntArrayList truncatedTokens = new IntArrayList(maxTokens);
        for (int i = 0; i < maxTokens; i++) {
            truncatedTokens.add(tokens.get(i));
        }

        if (!isTokenCalculation) {
            NotificationUtil.sendNotification(project, "Project context truncated due to token limit, was " +
                formatter.format(tokens.size()) + " tokens but limit is " + formatter.format(maxTokens) + " tokens. " +
                "You can exclude directories or files in the settings page.");
        }
        String truncatedContent = ENCODING.decode(truncatedTokens);
        return isTokenCalculation ? truncatedContent : truncatedContent + "\n--- Project context truncated due to token limit ---\n";
    }

    /**
     * Generate a tree structure of the project source files.
     * @param project the project
     * @return the tree structure
     */
    private @NotNull String generateSourceTree(@NotNull Project project) {
        VirtualFile projectDir = project.getBaseDir();
        return generateSourceTreeRecursive(projectDir, 0);
    }

    /**
     * Generate a tree structure of the project source files recursively.
     * @param dir   the directory
     * @param depth the depth
     * @return the tree structure
     */
    private @NotNull String generateSourceTreeRecursive(VirtualFile dir, int depth) {
        StringBuilder result = new StringBuilder();
        String indent = "  ".repeat(depth);

        if (shouldExcludeDirectory(dir)) {
            return "";  // Skip excluded directories
        }

        result.append(indent).append(dir.getName()).append("/\n");

        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                result.append(generateSourceTreeRecursive(child, depth + 1));
            } else if (shouldIncludeFile(child)) {
                result.append(indent).append("  ").append(child.getName()).append("\n");
            }
        }

        return result.toString();
    }

    private boolean shouldExcludeDirectory(@NotNull VirtualFile file) {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        return file.isDirectory() && settings.getExcludedDirectories().contains(file.getName());
    }

    private boolean shouldIncludeFile(@NotNull VirtualFile file) {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        String extension = file.getExtension();
        return extension != null && settings.getIncludedFileExtensions().contains(extension.toLowerCase());
    }
}
