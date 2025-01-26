package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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
        providerKeyMap.put(AzureOpenAI, stateService::getAzureOpenAIKey);
        providerKeyMap.put(Bedrock, stateService::getAwsAccessKey);
    }

    @NotNull
    public static LLMProviderService getInstance() {
        return ApplicationManager.getApplication().getService(LLMProviderService.class);
    }

    public List<ModelProvider> getAvailableModelProviders() {
        List<ModelProvider> providers = new ArrayList<>();
        providers.addAll(getModelProvidersWithApiKeyConfigured());
        providers.addAll(getLocalModelProviders());
        providers.addAll(getOptionalProviders());

        return providers;
    }

    private List<ModelProvider> getLocalModelProviders() {
        return ModelProvider.fromType(Type.LOCAL);
    }

    /**
     * Get LLM providers for which an API Key is defined in the settings
     *
     * @return List of LLM providers
     */
    private List<ModelProvider> getModelProvidersWithApiKeyConfigured() {
        return LLMModelRegistryService.getInstance().getModels()
            .stream()
            .filter(LanguageModel::isApiKeyUsed)
            .map(LanguageModel::getProvider)
            .distinct()
            .filter(provider -> Optional.ofNullable(providerKeyMap.get(provider))
                .map(Supplier::get)
                .filter(key -> !key.isBlank())
                .isPresent())
            .toList();
    }

    private @NotNull List<ModelProvider> getOptionalProviders() {
        List<ModelProvider> optionalModelProviders = new ArrayList<>();

        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getShowAzureOpenAIFields())) {
            optionalModelProviders.add(AzureOpenAI);
        }

        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getShowAwsFields())) {
            optionalModelProviders.add(Bedrock);
        }


        return optionalModelProviders;
    }

    /**
     * Get the API key for the specified model provider.
     *
     * @param provider The model provider for which to retrieve the API key.
     * @return The API key as a string, or an empty string if not configured.
     */
    public String getApiKey(ModelProvider provider) {
        return Optional.ofNullable(providerKeyMap.get(provider))
                .map(Supplier::get)
                .filter(key -> !key.isBlank())
                .orElse("");
    }
}
