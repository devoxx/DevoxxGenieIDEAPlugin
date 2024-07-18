package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.devoxx.genie.model.enumarations.ModelProvider.*;
import static com.devoxx.genie.model.enumarations.ModelProvider.Gemini;

public class LLMProviderService {

    @NotNull
    public static LLMProviderService getInstance() {
        return ApplicationManager.getApplication().getService(LLMProviderService.class);
    }

    public List<ModelProvider> getLocalModelProviders() {
        return List.of(GPT4All, LMStudio, Ollama);
    }

    /**
     * Get LLM providers for which an API Key is defined in the settings
     * @return List of LLM providers
     */
    public List<ModelProvider> getModelProvidersWithApiKeyConfigured() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        Map<ModelProvider, Supplier<String>> providerKeyMap = new HashMap<>();
        providerKeyMap.put(OpenAI, settings::getOpenAIKey);
        providerKeyMap.put(Anthropic, settings::getAnthropicKey);
        providerKeyMap.put(Mistral, settings::getMistralKey);
        providerKeyMap.put(Groq, settings::getGroqKey);
        providerKeyMap.put(DeepInfra, settings::getDeepInfraKey);
        providerKeyMap.put(Gemini, settings::getGeminiKey);

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

    public int getCurrentModelWindowContext() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        String lastSelectedProvider = settings.getSelectedProvider();
        String lastSelectedLanguageModel = settings.getSelectedLanguageModel();

        if (lastSelectedLanguageModel == null) {
            return 8000; // Default to 8000 if no model is selected
        }

        return LLMModelRegistryService.getInstance()
            .getModels()
            .stream()
            .filter(model -> model.getProvider().getName().equals(lastSelectedProvider) && model.getModelName().equals(lastSelectedLanguageModel))
            .findFirst()
            .map(LanguageModel::getContextWindow).orElse(8000);
    }
}
