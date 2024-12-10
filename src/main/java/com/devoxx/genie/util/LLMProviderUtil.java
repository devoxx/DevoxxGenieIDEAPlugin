package com.devoxx.genie.util;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LLMProviderUtil {

    /**
     * Get LLM providers for which an API Key is defined in the settings
     *
     * @return List of LLM providers
     */
    public static List<ModelProvider> getApiKeyEnabledProviders() {
        DevoxxGenieSettingsService settings = DevoxxGenieStateService.getInstance();
        return Arrays.stream(ModelProvider.values())
            .filter(provider -> switch (provider) {
                case OPENAI -> !settings.getOpenAIKey().isEmpty();
                case AZURE_OPEN_AI -> !settings.getAzureOpenAIKey().isEmpty() &&
                        !settings.getAzureOpenAIEndpoint().isEmpty() && !settings.getAzureOpenAIDeployment().isEmpty();
                case ANTHROPIC -> !settings.getAnthropicKey().isEmpty();
                case MISTRAL -> !settings.getMistralKey().isEmpty();
                case GROQ -> !settings.getGroqKey().isEmpty();
                case DEEP_INFRA -> !settings.getDeepInfraKey().isEmpty();
                case DEEP_SEEK -> !settings.getDeepSeekKey().isEmpty();
                case OPEN_ROUTER -> !settings.getOpenRouterKey().isEmpty();
                case GOOGLE -> !settings.getGeminiKey().isEmpty();
                default -> false;
            })
            .collect(Collectors.toList());
    }
}
