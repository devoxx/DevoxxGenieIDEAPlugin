package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.devoxx.genie.model.enumarations.ModelProvider.*;

public class LLMProviderService {

    @NotNull
    public static LLMProviderService getInstance() {
        return ApplicationManager.getApplication().getService(LLMProviderService.class);
    }

    public List<ModelProvider> getLocalModelProviders() {
        return List.of(GPT4All, LMStudio, Ollama, Exo, LLaMA);
    }

    /**
     * Get LLM providers for which an API Key is defined in the settings
     * @return List of LLM providers
     */
    public List<ModelProvider> getModelProvidersWithApiKeyConfigured() {
        DevoxxGenieSettingsService settings = DevoxxGenieSettingsServiceProvider.getInstance();
        Map<ModelProvider, Supplier<String>> providerKeyMap = new HashMap<>();
        providerKeyMap.put(OpenAI, settings::getOpenAIKey);
        providerKeyMap.put(Anthropic, settings::getAnthropicKey);
        providerKeyMap.put(Mistral, settings::getMistralKey);
        providerKeyMap.put(Groq, settings::getGroqKey);
        providerKeyMap.put(DeepInfra, settings::getDeepInfraKey);
        providerKeyMap.put(Google, settings::getGeminiKey);

        // Filter out cloud LLM providers that do not have a key
        List<ModelProvider> providersWithRequiredKey = LLMModelRegistryService.getInstance().getModels()
            .stream()
            .filter(LanguageModel::isApiKeyUsed)
            .map(LanguageModel::getProvider)
            .distinct()
            .toList();

        return providersWithRequiredKey
            .stream()
            .filter(provider -> Optional.ofNullable(providerKeyMap.get(provider))
                .map(Supplier::get)
                .filter(key -> !key.isBlank())
                .isPresent())
            .distinct()
            .collect(Collectors.toList());
    }
}
