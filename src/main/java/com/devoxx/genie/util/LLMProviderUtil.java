package com.devoxx.genie.util;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.service.DevoxxGenieSettingsServiceProvider;

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
        DevoxxGenieSettingsService settings = DevoxxGenieSettingsServiceProvider.getInstance();
        return Arrays.stream(ModelProvider.values())
            .filter(provider -> switch (provider) {
                case OpenAI -> !settings.getOpenAIKey().isEmpty();
                case Anthropic -> !settings.getAnthropicKey().isEmpty();
                case Mistral -> !settings.getMistralKey().isEmpty();
                case Groq -> !settings.getGroqKey().isEmpty();
                case DeepInfra -> !settings.getDeepInfraKey().isEmpty();
                case DeepSeek -> !settings.getDeepSeekKey().isEmpty();
                case OpenRouter -> !settings.getOpenRouterKey().isEmpty();
                case Google -> !settings.getGeminiKey().isEmpty();
                default -> false;
            })
            .collect(Collectors.toList());
    }
}
