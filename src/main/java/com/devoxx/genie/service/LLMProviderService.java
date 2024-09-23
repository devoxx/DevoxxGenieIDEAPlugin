package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
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
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        providerKeyMap.put(OpenAI, stateService::getOpenAIKey);
        providerKeyMap.put(Anthropic, stateService::getAnthropicKey);
        providerKeyMap.put(Mistral, stateService::getMistralKey);
        providerKeyMap.put(Groq, stateService::getGroqKey);
        providerKeyMap.put(DeepInfra, stateService::getDeepInfraKey);
        providerKeyMap.put(Google, stateService::getGeminiKey);
        providerKeyMap.put(DeepSeek, stateService::getDeepSeekKey);
        providerKeyMap.put(OpenRouter, stateService::getOpenRouterKey);
    }

    @NotNull
    public static LLMProviderService getInstance() {
        return ApplicationManager.getApplication().getService(LLMProviderService.class);
    }

    public List<ModelProvider> getLocalModelProviders() {
        return List.of(GPT4All, LMStudio, Ollama, Exo, LLaMA, Jan);
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
