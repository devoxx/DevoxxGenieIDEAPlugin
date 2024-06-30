package com.devoxx.genie.service;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
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
import java.text.NumberFormat;
import java.util.concurrent.CompletableFuture;

public class ProjectContentService {

    private static final Encoding ENCODING = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    public static ProjectContentService getInstance() {
        return ApplicationManager.getApplication().getService(ProjectContentService.class);
    }

    public CompletableFuture<String> getProjectContent(Project project, int tokenLimit, boolean isTokenCalculation) {
        return ProjectScannerService.getInstance()
            .scanProject(project, null, tokenLimit, isTokenCalculation)
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

    public void calculateTokensAndCost(Project project, int tokenLimit, ModelProvider provider, String modelName) {
        if (!DefaultLLMSettings.isApiBasedProvider(provider)) {
            getProjectContent(project, tokenLimit, true)
                .thenAccept(projectContent -> {
                    int tokenCount = ENCODING.countTokens(projectContent);
                    String message = String.format("Project contains %s tokens. Cost calculation is not applicable for local providers.",
                        NumberFormat.getInstance().format(tokenCount));
                    NotificationUtil.sendNotification(project, message);
                });
            return;
        }

        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        double inputCost = settings.getModelInputCost(provider, modelName);

        getProjectContent(project, tokenLimit, true)
            .thenAccept(projectContent -> {
                int tokenCount = ENCODING.countTokens(projectContent);
                double estimatedInputCost = calculateCost(tokenCount, inputCost);
                String message = String.format("Project contains %s tokens. Estimated min. cost using %s: $%.4f",
                    NumberFormat.getInstance().format(tokenCount),
                    modelName,
                    estimatedInputCost);
                NotificationUtil.sendNotification(project, message);
            });
    }

    private double calculateCost(int tokenCount, double costPer1000Tokens) {
        return (tokenCount / 1_000_000.0) * costPer1000Tokens;
    }

    private void copyToClipboard(String content) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(content), null);
    }
}
