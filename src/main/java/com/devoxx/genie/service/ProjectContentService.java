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
        ProjectScannerService instance = ProjectScannerService.getInstance();
        ScanContentResult scanContentResult = instance.scanProject(project, null, windowContextMaxTokens, isTokenCalculation);
        return CompletableFuture.completedFuture(scanContentResult)
            .thenApply(content ->
                    getScanContentResult(isTokenCalculation, content));
    }

    private ScanContentResult getScanContentResult(boolean isTokenCalculation, ScanContentResult content) {
        if (!isTokenCalculation) {
            copyToClipboard(content);
        }
        return content;
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
        ProjectScannerService instance = ProjectScannerService.getInstance();
        ScanContentResult scanContentResult = instance.scanProject(project, directory, tokenLimit, isTokenCalculation);
        return CompletableFuture.completedFuture(scanContentResult)
            .thenApply(content ->
                    getScanContentResult(isTokenCalculation, content));
    }

    /**
     * Retrieves and processes the content of a specified file within a Project.
     * @param provider ModelProvider enum value representing the model provider to use
     * @return Encoding object representing the encoding to use for the specified provider
     */
    public static Encoding getEncodingForProvider(@NotNull ModelProvider provider) {
        return switch (provider) {
            case OpenAI, Anthropic, Google, AzureOpenAI ->
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
     * Copies the content of a ScanContentResult object to the system clipboard.
     * @param contentResult ScanContentResult object containing the content to copy
     */
    private void copyToClipboard(@NotNull ScanContentResult contentResult) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(contentResult.getContent()), null);
    }
}
