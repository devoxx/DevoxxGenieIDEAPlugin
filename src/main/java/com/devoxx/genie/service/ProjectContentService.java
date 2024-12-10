package com.devoxx.genie.service;

import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.projectscanner.ProjectScannerService;
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
     * @param isTokenCalculation     Whether this invocation is only used for calculating the tokens
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
     * @param tokenLimit Integer determining maximum number of tokens per file
     * @param isTokenCalculation Whether this invocation is only used for calculating the tokens
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

    public static Encoding getEncodingForProvider(@NotNull ModelProvider provider) {
        return switch (provider) {
            case OPENAI, ANTHROPIC, GOOGLE, AZURE_OPEN_AI ->
                Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
            case MISTRAL, DEEP_INFRA, GROQ, DEEP_SEEK, OPEN_ROUTER ->
                // These often use the Llama tokenizer or similar
                Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.R50K_BASE);
            default ->
                // Default to cl100k_base for unknown providers
                Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
        };
    }

    private void copyToClipboard(@NotNull ScanContentResult contentResult) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(contentResult.getContent()), null);
    }
}
