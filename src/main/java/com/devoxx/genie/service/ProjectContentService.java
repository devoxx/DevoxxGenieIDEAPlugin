package com.devoxx.genie.service;

import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.projectscanner.ProjectScannerService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.concurrent.CompletableFuture;

/**
 * The ProjectContentService class provides methods to retrieve and
 * process content from projects, directories, or specific files within a project.
 * It also calculates estimated costs for using different models based on the content's size.
 */
public class ProjectContentService {

    public static ProjectContentService getInstance() {
        return ApplicationManager.getApplication().getService(ProjectContentService.class);
    }

    /**
     * Retrieves and processes the content of a specified project, returning it as a string.
     * This method is typically used for token calculations,
     * if required by user settings or provider configurations.
     *
     * @param project                The Project to scan for content
     * @param windowContextMaxTokens Integer representing the desired Window Context (ignored in this implementation)
     * @return String representation of the project's content, optionally copied to clipboard based on configuration flag
     */
    public CompletableFuture<ScanContentResult> getProjectContent(Project project,
                                                                  int windowContextMaxTokens,
                                                                  boolean isTokenCalculation) {
        return ProjectScannerService.getInstance()
            .scanProject(project, null, windowContextMaxTokens, isTokenCalculation)
            .thenApply(content -> {
                if (!isTokenCalculation) {
                    copyToClipboard(content);
                }
                return content;
            });
    }

    /**
     * Retrieves and processes directory contents within a specified Project.
     * Returns string representation of found files content,
     * if required by user settings or provider configurations.
     *
     * @param project    The Project containing the directory to scan for content
     * @param directory  VirtualFile representing the directory to be scanned
     * @param tokenLimit Integer determining maximum number of tokens per file (ignored in this implementation)
     * @return ProjectScanResult object containing the content of the directory and token count
     */
    public CompletableFuture<ScanContentResult> getDirectoryContent(Project project,
                                                                    VirtualFile directory,
                                                                    int tokenLimit,
                                                                    boolean isTokenCalculation) {
        return ProjectScannerService.getInstance()
            .scanProject(project, directory, tokenLimit, isTokenCalculation)
            .thenApply(content -> {
                if (!isTokenCalculation) {
                    copyToClipboard(content);
                }
                return content;
            });
    }

    public CompletableFuture<ScanContentResult> getDirectoryContentAndTokens(VirtualFile directory,
                                                                             boolean isTokenCalculation,
                                                                             ModelProvider modelProvider) {
        return CompletableFuture.supplyAsync(() -> {
            ScanContentResult scanContentResult = new ScanContentResult();

            StringBuilder content = new StringBuilder();

            Encoding encoding = getEncodingForProvider(modelProvider);
            processDirectoryRecursively(directory, content, scanContentResult, isTokenCalculation, encoding);

            return scanContentResult;
        });
    }

    public static Encoding getEncodingForProvider(@NotNull ModelProvider provider) {
        return switch (provider) {
            case OpenAI, Anthropic, Google ->
                Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
            case Mistral, DeepInfra, Groq, DeepSeek, OpenRouter ->
                // These often use the Llama tokenizer or similar
                Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.R50K_BASE);
            default ->
                // Default to cl100k_base for unknown providers
                Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
        };
    }

    /**
     * Processes a directory recursively, calculating the number of tokens and building a content string.
     *
     * @param directory          VirtualFile representing the directory to scan
     * @param content            StringBuilder object to hold the content of the scanned files
     * @param scanContentResult  ScanContentResult object to hold the scan results
     * @param isTokenCalculation Boolean flag indicating whether to calculate tokens or not
     */
    private void processDirectoryRecursively(@NotNull VirtualFile directory,
                                             StringBuilder content,
                                             ScanContentResult scanContentResult,
                                             boolean isTokenCalculation,
                                             Encoding encoding) {
        DevoxxGenieSettingsService settings = DevoxxGenieStateService.getInstance();

        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                if (!settings.getExcludedDirectories().contains(child.getName())) {
                    processDirectoryRecursively(child, content, scanContentResult, isTokenCalculation, encoding);
                } else {
                    scanContentResult.incrementSkippedDirectoryCount();
                }
            } else if (shouldIncludeFile(child, settings)) {
                String fileContent = readFileContent(child);
                scanContentResult.incrementFileCount();
                if (!isTokenCalculation) {
                    content.append("File: ").append(child.getPath()).append("\n");
                    content.append(fileContent).append("\n\n");
                }
                scanContentResult.addTokenCount(encoding.countTokens(fileContent));
            } else {
                scanContentResult.incrementSkippedFileCount();
            }
        }
    }

    private boolean shouldIncludeFile(@NotNull VirtualFile file, DevoxxGenieSettingsService settings) {
        String extension = file.getExtension();
        return extension != null && settings.getIncludedFileExtensions().contains(extension.toLowerCase());
    }

    private @NotNull String readFileContent(VirtualFile file) {
        try {
            return new String(file.contentsToByteArray());
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    private void copyToClipboard(@NotNull ScanContentResult contentResult) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(contentResult.getContent()), null);
    }
}
