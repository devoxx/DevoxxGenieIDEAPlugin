package com.devoxx.genie.service;

import com.devoxx.genie.model.ContentResult;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.ui.util.WindowContextFormatterUtil;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.google.common.util.concurrent.AtomicDouble;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

    public CompletableFuture<ContentResult> getDirectoryContentAndTokens(VirtualFile directory,
                                                                         boolean isTokenCalculation,
                                                                         ModelProvider modelProvider) {
        return CompletableFuture.supplyAsync(() -> {
            AtomicLong totalTokens = new AtomicLong(0);
            StringBuilder content = new StringBuilder();

            Encoding encoding = getEncodingForProvider(modelProvider);
            processDirectoryRecursively(directory, content, totalTokens, isTokenCalculation, encoding);

            return new ContentResult(content.toString(), totalTokens.intValue());
        });
    }

    /**
     * Calculates the number of tokens and estimated cost for a specified Project.
     * @param project The Project to scan for content
     * @param windowContext Integer representing the desired Window Context (ignored in this implementation)
     * @param provider ModelProvider enum value representing the provider to use for cost calculation
     * @param languageModel LanguageModel object representing the model to use for cost calculation
     */
    public void calculateTokensAndCost(Project project,
                                       int windowContext,
                                       ModelProvider provider,
                                       LanguageModel languageModel) {
        if (!DefaultLLMSettingsUtil.isApiBasedProvider(provider)) {
            getProjectContent(project, windowContext, true)
                .thenCompose(projectContent -> CompletableFuture.runAsync(() -> {
                    AtomicInteger tokenCount = new AtomicInteger(ENCODING.countTokens(projectContent));
                    String message = String.format("Project contains %s. " +
                            "Cost calculation is not applicable for local providers. " +
                            "Make sure you select a model with a big enough window context.",
                        WindowContextFormatterUtil.format(tokenCount.get(), "tokens"));
                    NotificationUtil.sendNotification(project, message);
                }));
            return;
        }

        DevoxxGenieSettingsService settings = DevoxxGenieSettingsServiceProvider.getInstance();
        AtomicDouble inputCost = new AtomicDouble(settings.getModelInputCost(provider, languageModel.getModelName()));

        getProjectContent(project, windowContext, true)
            .thenCompose(projectContent -> CompletableFuture.runAsync(() -> {
                int tokenCount = ENCODING.countTokens(projectContent);
                double estimatedInputCost = calculateCost(tokenCount, inputCost.get());
                String message = String.format("Project contains %s. Estimated min. cost using %s %s is $%.5f",
                    WindowContextFormatterUtil.format(tokenCount, "tokens"),
                    provider.getName(),
                    languageModel.getDisplayName(),
                    estimatedInputCost);

                if (tokenCount > languageModel.getContextWindow()) {
                    message += String.format(". Total project size exceeds model's max context of %s tokens.",
                        WindowContextFormatterUtil.format(languageModel.getContextWindow()));
                }

                NotificationUtil.sendNotification(project, message);
            }));
    }

    private Encoding getEncodingForProvider(@NotNull ModelProvider provider) {
        return switch (provider) {
            case OpenAI, Anthropic, Google ->
                Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
            case Mistral, DeepInfra, Groq ->
                // These often use the Llama tokenizer or similar
                Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.R50K_BASE);
            default ->
                // Default to cl100k_base for unknown providers
                Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
        };
    }

    /**
     * Processes a directory recursively, calculating the number of tokens and building a content string.
     * @param directory VirtualFile representing the directory to scan
     * @param content StringBuilder object to hold the content of the scanned files
     * @param totalTokens AtomicLong object to hold the total token count
     * @param isTokenCalculation Boolean flag indicating whether to calculate tokens or not
     */
    private void processDirectoryRecursively(@NotNull VirtualFile directory,
                                             StringBuilder content,
                                             AtomicLong totalTokens,
                                             boolean isTokenCalculation,
                                             Encoding encoding) {
        DevoxxGenieSettingsService settings = DevoxxGenieSettingsServiceProvider.getInstance();

        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                if (!settings.getExcludedDirectories().contains(child.getName())) {
                    processDirectoryRecursively(child, content, totalTokens, isTokenCalculation, encoding);
                }
            } else if (shouldIncludeFile(child, settings)) {
                String fileContent = readFileContent(child);
                if (!isTokenCalculation) {
                    content.append("File: ").append(child.getPath()).append("\n");
                    content.append(fileContent).append("\n\n");
                }
                totalTokens.addAndGet(encoding.countTokens(fileContent));
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

    private double calculateCost(int tokenCount, double tokenCost) {
        return (tokenCount / 1_000_000.0) * tokenCost;
    }

    private void copyToClipboard(String content) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(content), null);
    }
}
