package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.devoxx.genie.model.enumarations.ModelProvider.*;

public class LLMProviderService {

    private static final EnumMap<ModelProvider, Supplier<String>> providerKeyMap = new EnumMap<>(ModelProvider.class);

    static {
        providerKeyMap.put(OpenAI, DevoxxGenieSettingsServiceProvider.getInstance()::getOpenAIKey);
        providerKeyMap.put(Anthropic, DevoxxGenieSettingsServiceProvider.getInstance()::getAnthropicKey);
        providerKeyMap.put(Mistral, DevoxxGenieSettingsServiceProvider.getInstance()::getMistralKey);
        providerKeyMap.put(Groq, DevoxxGenieSettingsServiceProvider.getInstance()::getGroqKey);
        providerKeyMap.put(DeepInfra, DevoxxGenieSettingsServiceProvider.getInstance()::getDeepInfraKey);
        providerKeyMap.put(Google, DevoxxGenieSettingsServiceProvider.getInstance()::getGeminiKey);
        providerKeyMap.put(DeepSeek, DevoxxGenieSettingsServiceProvider.getInstance()::getDeepSeekKey);
        providerKeyMap.put(OpenRouter, DevoxxGenieSettingsServiceProvider.getInstance()::getOpenRouterKey);
    }

    @NotNull
    public static LLMProviderService getInstance() {
        return ApplicationManager.getApplication().getService(LLMProviderService.class);
    }

    public List<ModelProvider> getLocalModelProviders() {
        return List.of(GPT4All, LMStudio, Ollama, Exo, LLaMA);
    }

    /**
     * Get LLM providers for which an API Key is defined in the settings
     *
     * @return List of LLM providers
     */
    public List<ModelProvider> getModelProvidersWithApiKeyConfigured() {
        return LLMModelRegistryService.getInstance().getModels()
            .stream()
            .filter(LanguageModel::isApiKeyUsed)
            .map(LanguageModel::getProvider)
            .distinct()
            .filter(provider -> Optional.ofNullable(providerKeyMap.get(provider))
                .map(Supplier::get)
                .filter(key -> !key.isBlank())
                .isPresent())
            .collect(Collectors.toList());
    }
}
