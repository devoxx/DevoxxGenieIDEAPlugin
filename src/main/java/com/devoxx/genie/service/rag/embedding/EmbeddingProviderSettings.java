package com.devoxx.genie.service.rag.embedding;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Reads an embedding provider's URL, credential and enabled state from the LLM settings. */
public final class EmbeddingProviderSettings {

    private static final String OPENAI_URL = "https://api.openai.com/v1";

    private EmbeddingProviderSettings() {}

    public static @Nullable String baseUrl(@NotNull EmbeddingProvider provider, @NotNull DevoxxGenieStateService state) {
        return switch (provider) {
            case OLLAMA -> state.getOllamaModelUrl();
            case CUSTOM_OPENAI -> state.getCustomOpenAIUrl();
            case OPENAI -> OPENAI_URL;
            case LM_STUDIO -> state.getLmstudioModelUrl();
            case LLAMA_CPP -> state.getLocalProviderUrl(ModelProvider.LLaMA);
        };
    }

    /** Reads the credential store, which can be slow or show a keychain dialog: never call on the EDT. */
    public static @Nullable String apiKey(@NotNull EmbeddingProvider provider, @NotNull DevoxxGenieStateService state) {
        return switch (provider) {
            case CUSTOM_OPENAI -> state.isCustomOpenAIApiKeyEnabled() ? state.getCustomOpenAIApiKey() : null;
            case OPENAI -> state.getOpenAIKey();
            case OLLAMA, LM_STUDIO, LLAMA_CPP -> null;
        };
    }

    /**
     * Whether the provider can be used according to the LLM settings. Ollama always can: RAG has
     * always embedded through it, even with Ollama hidden from the chat provider list.
     */
    public static boolean isEnabled(@NotNull EmbeddingProvider provider, @NotNull DevoxxGenieStateService state) {
        return switch (provider) {
            case OLLAMA -> true;
            case CUSTOM_OPENAI -> state.isCustomOpenAIUrlEnabled();
            case OPENAI -> state.isOpenAIEnabled();
            case LM_STUDIO -> state.isLmStudioEnabled();
            case LLAMA_CPP -> state.isLocalProviderEnabled(ModelProvider.LLaMA);
        };
    }

    /** Providers for the settings dropdown: the enabled ones, plus the selected one even if disabled. */
    public static @NotNull List<EmbeddingProvider> selectable(@NotNull DevoxxGenieStateService state,
                                                              @NotNull EmbeddingProvider selected) {
        List<EmbeddingProvider> providers = new ArrayList<>();
        for (EmbeddingProvider provider : EmbeddingProvider.values()) {
            if (provider == selected || isEnabled(provider, state)) {
                providers.add(provider);
            }
        }
        return providers;
    }
}
