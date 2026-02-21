package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.spec.AcpToolConfig;
import com.devoxx.genie.model.spec.CliToolConfig;
import com.devoxx.genie.service.models.LLMModelRegistryService;
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
        providerKeyMap.put(OpenAI, () -> DevoxxGenieStateService.getInstance().getOpenAIKey());
        providerKeyMap.put(Anthropic, () -> DevoxxGenieStateService.getInstance().getAnthropicKey());
        providerKeyMap.put(Mistral, () -> DevoxxGenieStateService.getInstance().getMistralKey());
        providerKeyMap.put(Groq, () -> DevoxxGenieStateService.getInstance().getGroqKey());
        providerKeyMap.put(DeepInfra, () -> DevoxxGenieStateService.getInstance().getDeepInfraKey());
        providerKeyMap.put(Google, () -> DevoxxGenieStateService.getInstance().getGeminiKey());
        providerKeyMap.put(DeepSeek, () -> DevoxxGenieStateService.getInstance().getDeepSeekKey());
        providerKeyMap.put(OpenRouter, () -> DevoxxGenieStateService.getInstance().getOpenRouterKey());
        providerKeyMap.put(Grok, () -> DevoxxGenieStateService.getInstance().getGrokKey());
        providerKeyMap.put(Kimi, () -> DevoxxGenieStateService.getInstance().getKimiKey());
        providerKeyMap.put(GLM, () -> DevoxxGenieStateService.getInstance().getGlmKey());
        providerKeyMap.put(AzureOpenAI, () -> DevoxxGenieStateService.getInstance().getAzureOpenAIKey());
        providerKeyMap.put(Bedrock, () -> DevoxxGenieStateService.getInstance().getAwsSecretKey());
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
        providers.addAll(getCliRunnersProvider());
        providers.addAll(getAcpRunnersProvider());

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

    private @NotNull List<ModelProvider> getAcpRunnersProvider() {
        boolean hasEnabledAcpTool = DevoxxGenieStateService.getInstance().getAcpTools().stream()
                .anyMatch(AcpToolConfig::isEnabled);
        if (hasEnabledAcpTool) {
            return List.of(ModelProvider.ACPRunners);
        }
        return List.of();
    }

    private @NotNull List<ModelProvider> getCliRunnersProvider() {
        boolean hasEnabledCliTool = DevoxxGenieStateService.getInstance().getCliTools().stream()
                .anyMatch(CliToolConfig::isEnabled);
        if (hasEnabledCliTool) {
            return List.of(ModelProvider.CLIRunners);
        }
        return List.of();
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
