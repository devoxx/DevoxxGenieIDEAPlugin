package com.devoxx.genie.chatmodel;

import com.devoxx.genie.service.SettingsStateService;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.devoxx.genie.model.enumarations.ModelProvider.*;

public class LLMProviderConstant {

    private LLMProviderConstant() {
    }

    protected static final String[] llmProvidersWithKey = {
        Anthropic.getName(),
        DeepInfra.getName(),
        Gemini.getName(),
        Groq.getName(),
        Mistral.getName(),
        OpenAI.getName()
    };

    protected static final String[] llmProviders = {
        GPT4All.getName(),
        LMStudio.getName(),
        Ollama.getName(),
        Jan.getName()
    };

    public static @NotNull List<String> getLLMProviders() {
        SettingsStateService settingState = SettingsStateService.getInstance();
        Map<String, Supplier<String>> providerKeyMap = new HashMap<>();
        providerKeyMap.put(OpenAI.getName(), settingState::getOpenAIKey);
        providerKeyMap.put(Anthropic.getName(), settingState::getAnthropicKey);
        providerKeyMap.put(Mistral.getName(), settingState::getMistralKey);
        providerKeyMap.put(Groq.getName(), settingState::getGroqKey);
        providerKeyMap.put(DeepInfra.getName(), settingState::getDeepInfraKey);
        providerKeyMap.put(Gemini.getName(), settingState::getGeminiKey);

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
}
