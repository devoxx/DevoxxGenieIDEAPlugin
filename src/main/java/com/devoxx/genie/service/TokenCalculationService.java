package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.util.WindowContextFormatterUtil;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.google.common.util.concurrent.AtomicDouble;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class TokenCalculationService {

    private final ProjectContentService projectContentService;

    public TokenCalculationService() {
        projectContentService = ProjectContentService.getInstance();
    }

    public void calculateTokensAndCost(@NotNull Project project,
                                       VirtualFile directory,
                                       int maxTokens,
                                       @NotNull ModelProvider selectedProvider,
                                       LanguageModel selectedLanguageModel,
                                       boolean showCost) {

        CompletableFuture<ScanContentResult> contentFuture;
        if (directory != null) {
            contentFuture = projectContentService.getDirectoryContentAndTokens(directory, true, selectedProvider);
        } else {
            contentFuture = projectContentService.getProjectContent(project, maxTokens, true);
        }

        if (showCost) {
            showCostAndScanInfo(project, selectedProvider, selectedLanguageModel, contentFuture);
        } else {
            showOnlyScanInfo(project, directory, selectedProvider, contentFuture);
        }
    }

    private static void showOnlyScanInfo(@NotNull Project project,
                                         VirtualFile directory,
                                         @NotNull ModelProvider selectedProvider,
                                         @NotNull CompletableFuture<ScanContentResult> contentFuture) {
        contentFuture.thenAccept(result -> {
            String message = String.format(
                "The %s scan includes %d files (skipped %d files and %d directories), " +
                "resulting in approximately %s tokens (processed using the %s tokenizer).",
                directory != null ? "'" + directory.getName() + "' directory" : "project",
                result.getFileCount(),
                result.getSkippedFileCount(),
                result.getSkippedDirectoryCount(),
                WindowContextFormatterUtil.format(result.getTokenCount()),
                selectedProvider.getName());
            NotificationUtil.sendNotification(project, message);
        });
    }

    private void showCostAndScanInfo(@NotNull Project project,
                                     @NotNull ModelProvider selectedProvider,
                                     @NotNull LanguageModel languageModel,
                                     CompletableFuture<ScanContentResult> contentFuture) {
        if (!DefaultLLMSettingsUtil.isApiBasedProvider(selectedProvider)) {
            showInfoForLocalProvider(project, contentFuture);
        } else {
            showInfoForCloudProvider(project, selectedProvider, languageModel, contentFuture);
        }
    }

    private void showInfoForCloudProvider(@NotNull Project project,
                                          @NotNull ModelProvider selectedProvider,
                                          @NotNull LanguageModel languageModel,
                                          @NotNull CompletableFuture<ScanContentResult> contentFuture) {
        DevoxxGenieSettingsService settings = DevoxxGenieSettingsServiceProvider.getInstance();
        AtomicDouble inputCost = new AtomicDouble(settings.getModelInputCost(selectedProvider, languageModel.getModelName()));

        contentFuture.thenAccept(scanResult -> {
            double estimatedInputCost = calculateCost(scanResult.getTokenCount(), inputCost.get());
            String message = String.format(
                "Project contains %s tokens in %d files (skipped %d files and %d directories)." +
                "Estimated min. cost using %s %s is $%.5f",
                WindowContextFormatterUtil.format(scanResult.getTokenCount(), "tokens"),
                scanResult.getFileCount(),
                scanResult.getSkippedFileCount(),
                scanResult.getSkippedDirectoryCount(),
                selectedProvider.getName(),
                languageModel.getDisplayName(),
                estimatedInputCost);

            if (scanResult.getTokenCount() > languageModel.getContextWindow()) {
                message += String.format(". Total project size exceeds model's max context of %s tokens.",
                    WindowContextFormatterUtil.format(languageModel.getContextWindow()));
            }

            NotificationUtil.sendNotification(project, message);
        });
    }

    private static void showInfoForLocalProvider(@NotNull Project project,
                                                 @NotNull CompletableFuture<ScanContentResult> contentFuture) {
        contentFuture.thenAccept(scanResult -> {
                    String message = String.format("Project contains %s in %d files " +
                            "(skipped %d files and %d directories).  " +
                            "Cost calculation is not applicable for local providers. " +
                            "Make sure you select a model with a big enough window context.",
                        WindowContextFormatterUtil.format(scanResult.getTokenCount(), "tokens"),
                        scanResult.getFileCount(),
                        scanResult.getSkippedFileCount(),
                        scanResult.getSkippedDirectoryCount());
                    NotificationUtil.sendNotification(project, message);
                });
    }

    private double calculateCost(int tokenCount, double tokenCost) {
        return (tokenCount / 1_000_000.0) * tokenCost;
    }

}
