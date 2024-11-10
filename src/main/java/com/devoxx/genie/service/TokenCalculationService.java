package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.util.WindowContextFormatterUtil;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
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
            contentFuture = projectContentService.getDirectoryContent(project, directory, maxTokens, true);
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
                "%s contains %s tokens using the %s tokenizer.  " +
                    "It includes %d files (skipped %d files and %d directories).",
                directory != null ? "'" + directory.getName() + "' directory" : "Project",
                WindowContextFormatterUtil.format(result.getTokenCount()),
                selectedProvider.getName(),
                result.getFileCount(),
                result.getSkippedFileCount(),
                result.getSkippedDirectoryCount());
            NotificationUtil.sendNotification(project, message);
        });
    }

    private void showCostAndScanInfo(@NotNull Project project,
                                     @NotNull ModelProvider selectedProvider,
                                     @NotNull LanguageModel languageModel,
                                     CompletableFuture<ScanContentResult> contentFuture) {
        if (!DefaultLLMSettingsUtil.isApiKeyBasedProvider(selectedProvider)) {
            contentFuture.thenAccept(scanResult -> {
                String defaultMessage = getDefaultMessage(scanResult);
                NotificationUtil.sendNotification(project, defaultMessage);
            });
        } else {
            showInfoForCloudProvider(project, selectedProvider, languageModel, contentFuture);
        }
    }

    private void showInfoForCloudProvider(@NotNull Project project,
                                          @NotNull ModelProvider selectedProvider,
                                          @NotNull LanguageModel languageModel,
                                          @NotNull CompletableFuture<ScanContentResult> contentFuture) {
        Optional<Double> inputCost = LLMModelRegistryService.getInstance().getModels()
            .stream()
            .filter(model -> model.getProvider().getName().equals(selectedProvider.getName()) &&
                            model.getModelName().equals(languageModel.getModelName()))
            .findFirst()
            .map(LanguageModel::getInputCost);

        inputCost.ifPresentOrElse(aDouble -> contentFuture.thenAccept(scanResult -> {
            double estimatedInputCost = calculateCost(scanResult.getTokenCount(), aDouble);
            String message;
            if (scanResult.getSkippedFileCount() > 0 || scanResult.getSkippedDirectoryCount() > 0) {
                message = String.format("%s Estimated cost using %s %s is $%.5f",
                    getDefaultMessage(scanResult),
                    selectedProvider.getName(),
                    languageModel.getDisplayName(),
                    estimatedInputCost
                );
            } else {
                message = String.format(
                    "Project contains %s in %d file%s.  Estimated cost using %s %s is $%.5f",
                    WindowContextFormatterUtil.format(scanResult.getTokenCount(), "tokens"),
                    scanResult.getFileCount(),
                    scanResult.getFileCount() > 1 ? "s" : "",
                    selectedProvider.getName(),
                    languageModel.getDisplayName(),
                    estimatedInputCost
                );
            }

            if (scanResult.getTokenCount() > languageModel.getContextWindow()) {
                message += String.format(". Total project size exceeds model's max context of %s tokens.",
                    WindowContextFormatterUtil.format(languageModel.getContextWindow()));
            }
            NotificationUtil.sendNotification(project, message);
        }), () -> {
            String message = "No input cost found for the selected model.";
            NotificationUtil.sendNotification(project, message);
        });
    }

    private @NotNull String getDefaultMessage(@NotNull ScanContentResult scanResult) {
        return String.format(
            "%s from %d files, skipped " +
                (scanResult.getSkippedFileCount() > 0 ? scanResult.getSkippedFileCount() + " files " : "") +
                (scanResult.getSkippedFileCount() > 0 && scanResult.getSkippedDirectoryCount() > 0 ? " and " : "") +
                (scanResult.getSkippedDirectoryCount() > 0 ? scanResult.getSkippedDirectoryCount() + " directories" : "")
                + ".  ",
            WindowContextFormatterUtil.format(scanResult.getTokenCount(), "tokens"),
            scanResult.getFileCount());
    }

    private double calculateCost(int tokenCount, double tokenCost) {
        return (tokenCount / 1_000_000.0) * tokenCost;
    }
}
