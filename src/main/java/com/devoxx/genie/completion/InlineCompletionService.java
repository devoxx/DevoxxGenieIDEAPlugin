package com.devoxx.genie.completion;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application-level service that bridges the IntelliJ inline completion API
 * with the FIM provider backends (Ollama, LM Studio).
 *
 * Reads settings from {@link DevoxxGenieStateService}, builds a {@link FimRequest},
 * delegates to the appropriate {@link FimProvider}, and caches results.
 */
public final class InlineCompletionService {

    private static final Logger LOG = LoggerFactory.getLogger(InlineCompletionService.class);

    private final CompletionCache cache = new CompletionCache();
    private final OllamaFimProvider ollamaProvider = new OllamaFimProvider();
    private final LMStudioFimProvider lmStudioProvider = new LMStudioFimProvider();

    public static @NotNull InlineCompletionService getInstance() {
        return ApplicationManager.getApplication().getService(InlineCompletionService.class);
    }

    /**
     * Get a completion for the given prefix/suffix context.
     * Checks the cache first, then delegates to the configured FIM provider.
     * Called from a background thread (Dispatchers.IO).
     *
     * @param prefix text before the cursor
     * @param suffix text after the cursor
     * @return completion text, or null if unavailable
     */
    public @Nullable String getCompletion(@NotNull String prefix, @NotNull String suffix) {
        String cached = cache.get(prefix, suffix);
        if (cached != null) {
            return cached;
        }

        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        String providerName = state.getInlineCompletionProvider();
        if (providerName == null || providerName.isBlank()) {
            return null;
        }

        String modelName = state.getInlineCompletionModel();
        if (modelName == null || modelName.isBlank()) {
            return null;
        }

        FimProvider provider = getProvider(providerName);
        if (provider == null) {
            LOG.debug("Unknown inline completion provider: {}", providerName);
            return null;
        }

        String baseUrl = getBaseUrl(providerName, state);

        FimRequest request = FimRequest.builder()
                .prefix(prefix)
                .suffix(suffix)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(state.getInlineCompletionTemperature() != null
                        ? state.getInlineCompletionTemperature() : 0.0)
                .maxTokens(state.getInlineCompletionMaxTokens() != null
                        ? state.getInlineCompletionMaxTokens() : 64)
                .timeoutMs(state.getInlineCompletionTimeoutMs() != null
                        ? state.getInlineCompletionTimeoutMs() : 5000)
                .build();

        FimResponse response = provider.generate(request);
        if (response == null || response.getCompletionText().isEmpty()) {
            return null;
        }

        String completionText = response.getCompletionText();
        cache.put(prefix, suffix, completionText);

        LOG.debug("FIM completion from {} in {}ms", providerName, response.getDurationMs());
        return completionText;
    }

    /**
     * Cancel any active completion request across all providers.
     */
    public void cancelActiveRequests() {
        ollamaProvider.cancelActiveCall();
        lmStudioProvider.cancelActiveCall();
    }

    /**
     * Clear the completion cache.
     */
    public void clearCache() {
        cache.clear();
    }

    private @Nullable FimProvider getProvider(@NotNull String providerName) {
        return switch (providerName) {
            case "Ollama" -> ollamaProvider;
            case "LMStudio" -> lmStudioProvider;
            default -> null;
        };
    }

    private static @NotNull String getBaseUrl(@NotNull String providerName,
                                              @NotNull DevoxxGenieStateService state) {
        return switch (providerName) {
            case "Ollama" -> state.getOllamaModelUrl() != null
                    ? state.getOllamaModelUrl() : "http://localhost:11434/";
            case "LMStudio" -> state.getLmstudioModelUrl() != null
                    ? state.getLmstudioModelUrl() : "http://localhost:1234/v1/";
            default -> "";
        };
    }
}
