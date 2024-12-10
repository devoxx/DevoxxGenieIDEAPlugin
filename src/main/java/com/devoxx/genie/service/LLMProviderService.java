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
import java.util.stream.Collectors;

import static com.devoxx.genie.model.enumarations.ModelProvider.*;

public class LLMProviderService {

    private static final EnumMap<ModelProvider, Supplier<String>> providerKeyMap = new EnumMap<>(ModelProvider.class);

    static {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        providerKeyMap.put(OPENAI, stateService::getOpenAIKey);
        providerKeyMap.put(ANTHROPIC, stateService::getAnthropicKey);
        providerKeyMap.put(MISTRAL, stateService::getMistralKey);
        providerKeyMap.put(GROQ, stateService::getGroqKey);
        providerKeyMap.put(DEEP_INFRA, stateService::getDeepInfraKey);
        providerKeyMap.put(GOOGLE, stateService::getGeminiKey);
        providerKeyMap.put(DEEP_SEEK, stateService::getDeepSeekKey);
        providerKeyMap.put(OPEN_ROUTER, stateService::getOpenRouterKey);
        providerKeyMap.put(AZURE_OPEN_AI, stateService::getAzureOpenAIKey);
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
            .collect(Collectors.toList());
    }

    private List<ModelProvider> getOptionalProviders() {
        List<ModelProvider> optionalModelProviders = new ArrayList<>();

        if (DevoxxGenieStateService.getInstance().getShowAzureOpenAIFields()) {
            optionalModelProviders.add(AZURE_OPEN_AI);
        }

        return optionalModelProviders;
    }
}
