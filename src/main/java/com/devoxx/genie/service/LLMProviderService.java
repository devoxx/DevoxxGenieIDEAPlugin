package com.devoxx.genie.service;

import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.devoxx.genie.model.enumarations.ModelProvider.*;

public class LLMProviderService {

    private static final String[] llmProvidersWithKey = {
        Anthropic.getName(),
        DeepInfra.getName(),
        Gemini.getName(),
        Groq.getName(),
        Mistral.getName(),
        OpenAI.getName()
    };

    private static final String[] llmProviders = {
        GPT4All.getName(),
        LMStudio.getName(),
        Ollama.getName(),
        // Jan.getName()
    };

    @NotNull
    public static LLMProviderService getInstance() {
        return ApplicationManager.getApplication().getService(LLMProviderService.class);
    }

    public List<String> getAvailableLLMProviders() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        Map<String, Supplier<String>> providerKeyMap = new HashMap<>();
        providerKeyMap.put(OpenAI.getName(), settings::getOpenAIKey);
        providerKeyMap.put(Anthropic.getName(), settings::getAnthropicKey);
        providerKeyMap.put(Mistral.getName(), settings::getMistralKey);
        providerKeyMap.put(Groq.getName(), settings::getGroqKey);
        providerKeyMap.put(DeepInfra.getName(), settings::getDeepInfraKey);
        providerKeyMap.put(Gemini.getName(), settings::getGeminiKey);

        // Filter out cloud LLM providers that do not have a key
        var providers = Stream.of(llmProvidersWithKey)
            .filter(provider -> Optional.ofNullable(providerKeyMap.get(provider))
                .map(Supplier::get)
                .filter(key -> !key.isBlank())
                .isPresent())
            .collect(Collectors.toList());

        Collections.addAll(providers, llmProviders);
        return providers;
    }

    public int getCurrentModelTokenLimit() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        String provider = settings.getLastSelectedProvider();
        String modelName = settings.getLastSelectedModel();

        if (provider == null || modelName == null) {
            return 8000; // Default to 8000 if no model is selected
        }

        return ChatModelFactoryProvider.getFactoryByProvider(fromString(provider))
            .flatMap(factory -> factory.getModelNames().stream()
                .filter(model -> model.getName().equals(modelName))
                .findFirst())
            .map(LanguageModel::getMaxTokens)
            .orElse(8000);// Default to 8000 if the model is not found
    }
}
