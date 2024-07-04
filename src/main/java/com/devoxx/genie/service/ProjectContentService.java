package com.devoxx.genie.service;

import com.devoxx.genie.model.ContentResult;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.util.WindowContextFormatterUtil;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;

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

    private static final Encoding ENCODING = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    public static ProjectContentService getInstance() {
        return ApplicationManager.getApplication().getService(ProjectContentService.class);
    }

    /**
     * Retrieves and processes the content of a specified project, returning it as a string.
     * This method is typically used for token calculations,
     * if required by user settings or provider configurations.
     * @param project The Project to scan for content
     * @param windowContext Integer representing the desired Window Context (ignored in this implementation)
     * @return String representation of the project's content, optionally copied to clipboard based on configuration flag
     */
    public CompletableFuture<String> getProjectContent(Project project,
                                                       int windowContext,
                                                       boolean isTokenCalculation) {
        return ProjectScannerService.getInstance()
            .scanProject(project, null, windowContext, isTokenCalculation)
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
     * @param project The Project containing the directory to scan for content
     * @param directory VirtualFile representing the directory to be scanned
     * @param tokenLimit Integer determining maximum number of tokens per file (ignored in this implementation)
     * @return String representation of files' content, optionally copied to clipboard based on configuration flag
     */
    public CompletableFuture<String> getDirectoryContent(Project project,
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

    /**
     * Retrieves and processes the content of a specified directory within a Project.
     * Also calculates number of tokens in this content if required by user settings or provider configurations.
     * @param project The Project containing the directory to be scanned
     * @param directory VirtualFile representing the directory to scan for content
     * @return ContentResult object holding both content and token count, optionally copied to clipboard based on flag
     */
    public CompletableFuture<ContentResult> getDirectoryContentAndTokens(Project project,
                                                                         VirtualFile directory,
                                                                         int tokenLimit,
                                                                         boolean isTokenCalculation) {
        return ProjectScannerService.getInstance()
            .scanProject(project, directory, tokenLimit, isTokenCalculation)
            .thenApply(content -> {
                int tokenCount = ENCODING.countTokens(content);
                return new ContentResult(content, tokenCount);
            });
    }

    public void calculateTokensAndCost(Project project,
                                       int windowContext,
                                       ModelProvider provider,
                                       LanguageModel languageModel) {
        if (!DefaultLLMSettingsUtil.isApiBasedProvider(provider)) {
            getProjectContent(project, windowContext, true)
                .thenAccept(projectContent -> {
                    int tokenCount = ENCODING.countTokens(projectContent);
                    String message = String.format("Project contains %s. Cost calculation is not applicable for local providers.",
                        WindowContextFormatterUtil.format(tokenCount, "tokens"));
                    NotificationUtil.sendNotification(project, message);
                });
            return;
        }

        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        double inputCost = settings.getModelInputCost(provider, languageModel.getModelName());

        getProjectContent(project, windowContext, true)
            .thenAccept(projectContent -> {
                int tokenCount = ENCODING.countTokens(projectContent);
                double estimatedInputCost = calculateCost(tokenCount, inputCost);
                String message = String.format("Project contains %s. Estimated min. cost using %s is $%.6f",
                    WindowContextFormatterUtil.format(tokenCount, "tokens"),
                    languageModel.getDisplayName(),
                    estimatedInputCost);
                NotificationUtil.sendNotification(project, message);
            });
    }

    private double calculateCost(int tokenCount, double tokenCost) {
        return (tokenCount / 1_000_000.0) * tokenCost;
    }

    private void copyToClipboard(String content) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(content), null);
    }
}
