package com.devoxx.genie.service;

import com.devoxx.genie.controller.listener.TokenCalculationListener;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.util.WindowContextFormatterUtil;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
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
                                       boolean showCost,
                                       TokenCalculationListener listener) {

        CompletableFuture<ScanContentResult> contentFuture;
        if (directory != null) {
            contentFuture = projectContentService.getDirectoryContent(project, directory, maxTokens, true);
        } else {
            contentFuture = projectContentService.getProjectContent(project, maxTokens, true);
        }

        if (showCost) {
            showCostAndScanInfo(project, selectedProvider, selectedLanguageModel, contentFuture, listener);
        } else {
            showOnlyScanInfo(directory, selectedProvider, contentFuture, listener);
        }
    }

    private static void showOnlyScanInfo(VirtualFile directory,
                                         @NotNull ModelProvider selectedProvider,
                                         @NotNull CompletableFuture<ScanContentResult> contentFuture,
                                         TokenCalculationListener listener) {
        contentFuture.thenAccept(result -> {
            // Format tokens with K suffix (e.g., 8K)
            String formattedTokens = String.format("%.0f", result.getTokenCount() / 1000.0);
            
            // Main message with the requested format
            StringBuilder message = new StringBuilder();
            if (result.getSkippedDirectoryCount() > 0) {
                message.append(String.format(
                    "'%s' directory contains %sK tokens using the %s tokenizer.\n" +
                    "It includes %d files, skipped %d files and %d directories.",
                    directory != null ? directory.getName() : "Project",
                    formattedTokens,
                    selectedProvider.getName(),
                    result.getFileCount(),
                    result.getSkippedFileCount(),
                    result.getSkippedDirectoryCount()));
            } else {
                message.append(String.format(
                    "'%s' directory contains %sK tokens using the %s tokenizer.\n" +
                    "It includes %d files, skipped %d files.",
                    directory != null ? directory.getName() : "Project",
                    formattedTokens,
                    selectedProvider.getName(),
                    result.getFileCount(),
                    result.getSkippedFileCount()));
            }

            // Extract and collect skipped file extensions
            Set<String> skippedExtensions = new HashSet<>();
            Set<String> skippedDirs = new HashSet<>();
            
            if (result.getSkippedFiles() != null && !result.getSkippedFiles().isEmpty()) {
                for (Map.Entry<String, String> entry : result.getSkippedFiles().entrySet()) {
                    String path = entry.getKey();
                    String reason = entry.getValue();
                    
                    // Collect skipped extensions
                    if (path.contains(".")) {
                        String extension = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
                        skippedExtensions.add(extension);
                    }
                    
                    // Collect skipped directories
                    if (reason.contains("excluded by settings") || reason.contains("not in project content")) {
                        File file = new File(path);
                        String parentDir = file.getParent();
                        if (parentDir != null) {
                            String dirName = new File(parentDir).getName();
                            skippedDirs.add(dirName);
                        }
                    }
                }
            }
            
            // Add skipped file extensions to the message
            if (!skippedExtensions.isEmpty()) {
                message.append(String.format("\nSkipped files extensions : %s", String.join(", ", skippedExtensions)));
            }
            
            // Add skipped directories to the message
            if (!skippedDirs.isEmpty() && result.getSkippedDirectoryCount() > 0) {
                message.append(String.format("\nSkipped directories : %s", String.join(", ", skippedDirs)));
            }
            
            listener.onTokenCalculationComplete(message.toString());
        });
    }

    private void showCostAndScanInfo(@NotNull Project project,
                                     @NotNull ModelProvider selectedProvider,
                                     @NotNull LanguageModel languageModel,
                                     CompletableFuture<ScanContentResult> contentFuture,
                                     TokenCalculationListener listener) {
        if (!DefaultLLMSettingsUtil.isApiKeyBasedProvider(selectedProvider)) {
            contentFuture.thenAccept(scanResult -> {
                String defaultMessage = getDefaultMessage(scanResult);
                listener.onTokenCalculationComplete(defaultMessage);
            });
        } else {
            showInfoForCloudProvider(project, selectedProvider, languageModel, contentFuture, listener);
        }
    }

    private void showInfoForCloudProvider(@NotNull Project project,
                                          @NotNull ModelProvider selectedProvider,
                                          @NotNull LanguageModel languageModel,
                                          @NotNull CompletableFuture<ScanContentResult> contentFuture,
                                          TokenCalculationListener listener) {
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
                message = getEstimatedCostMessage(selectedProvider, languageModel, scanResult, estimatedInputCost);
            } else {
                message = getTotalFilesAndEstimatedCostMessage(selectedProvider, languageModel, scanResult, estimatedInputCost);
            }

            if (scanResult.getTokenCount() > languageModel.getInputMaxTokens()) {
                message += String.format(". Total project size exceeds model's max context of %s tokens.",
                    WindowContextFormatterUtil.format(languageModel.getInputMaxTokens()));
            }
            listener.onTokenCalculationComplete(message);
        }), () -> {
            String message = "No input cost found for the selected model.";
            NotificationUtil.sendNotification(project, message);
        });
    }

    private @NotNull String getEstimatedCostMessage(@NotNull ModelProvider selectedProvider, @NotNull LanguageModel languageModel, ScanContentResult scanResult, double estimatedInputCost) {
        String message;
        message = String.format("%s Estimated cost using %s %s is $%.5f",
            getDefaultMessage(scanResult),
            selectedProvider.getName(),
            languageModel.getDisplayName(),
                estimatedInputCost
        );
        return message;
    }

    private static @NotNull String getTotalFilesAndEstimatedCostMessage(@NotNull ModelProvider selectedProvider, @NotNull LanguageModel languageModel, @NotNull ScanContentResult scanResult, double estimatedInputCost) {
        String message;
        message = String.format(
            "Project contains %s in %d file%s.  Estimated cost using %s %s is $%.5f",
            WindowContextFormatterUtil.format(scanResult.getTokenCount(), "tokens"),
            scanResult.getFileCount(),
            scanResult.getFileCount() > 1 ? "s" : "",
            selectedProvider.getName(),
            languageModel.getDisplayName(),
                estimatedInputCost
        );
        return message;
    }

    private @NotNull String getDefaultMessage(@NotNull ScanContentResult scanResult) {
        StringBuilder message = new StringBuilder(
            WindowContextFormatterUtil.format(scanResult.getTokenCount(), "tokens") +
            " from " + scanResult.getFileCount() + " files");
            
        // Add skipped information only if there's anything skipped
        if (scanResult.getSkippedFileCount() > 0 || scanResult.getSkippedDirectoryCount() > 0) {
            message.append(", skipped ");
            
            // Add skipped files if any
            if (scanResult.getSkippedFileCount() > 0) {
                message.append(scanResult.getSkippedFileCount()).append(" files");
            }
            
            // Add "and" if both files and directories are skipped
            if (scanResult.getSkippedFileCount() > 0 && scanResult.getSkippedDirectoryCount() > 0) {
                message.append(" and ");
            }
            
            // Add skipped directories only if there are any
            if (scanResult.getSkippedDirectoryCount() > 0) {
                message.append(scanResult.getSkippedDirectoryCount()).append(" directories");
            }
        }
        
        message.append(".  ");
        return message.toString();
    }

    private double calculateCost(int tokenCount, double tokenCost) {
        return (tokenCount / 1_000_000.0) * tokenCost;
    }
}
