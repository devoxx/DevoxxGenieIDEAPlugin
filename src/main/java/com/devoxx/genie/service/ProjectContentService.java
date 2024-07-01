package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.util.WindowContextFormatterUtil;
import com.devoxx.genie.util.DefaultLLMSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;

import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.util.concurrent.CompletableFuture;

public class ProjectContentService {

    private static final Encoding ENCODING = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    public static ProjectContentService getInstance() {
        return ApplicationManager.getApplication().getService(ProjectContentService.class);
    }

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

    public void calculateTokensAndCost(Project project,
                                       int windowContext,
                                       ModelProvider provider,
                                       LanguageModel languageModel) {
        if (!DefaultLLMSettings.isApiBasedProvider(provider)) {
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
