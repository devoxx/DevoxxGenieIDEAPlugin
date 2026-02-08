package com.devoxx.genie.model.enumarations;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ModelProvider {
    CustomOpenAI("CustomOpenAI", Type.LOCAL),
    GPT4All("GPT4All", Type.LOCAL),
    Jan("Jan", Type.LOCAL),
    LLaMA("LLaMA.c++", Type.LOCAL),
    LMStudio("LMStudio", Type.LOCAL),
    Ollama("Ollama", Type.LOCAL),

    OpenAI("OpenAI", Type.CLOUD),
    Anthropic("Anthropic", Type.CLOUD),
    Mistral("Mistral", Type.CLOUD),
    Groq("Groq", Type.CLOUD),
    DeepInfra("DeepInfra", Type.CLOUD),
    Google("Google", Type.CLOUD),
    OpenRouter("OpenRouter", Type.CLOUD),
    DeepSeek("DeepSeek", Type.CLOUD),
    Grok("Grok", Type.CLOUD),
    Kimi("Kimi", Type.CLOUD),
    GLM("GLM", Type.CLOUD),

    AzureOpenAI("AzureOpenAI", Type.OPTIONAL),
    Bedrock("Bedrock", Type.OPTIONAL);

    public enum Type {
        LOCAL, // Local Providers
        CLOUD, // Cloud Providers
        OPTIONAL // Optional Providers(Need to be enabled from settings, due to inconvenient setup)
    }

    private final String name;
    private final Type type;

    ModelProvider(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return name;
    }

    public static @NotNull ModelProvider fromString(String text) {
        for (ModelProvider provider : ModelProvider.values()) {
            if (provider.name.equalsIgnoreCase(text)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }

    public static List<ModelProvider> fromType(ModelProvider.Type type) {
        return Arrays.stream(ModelProvider.values())
                .filter(provider -> provider.type == type)
                .toList();
    }
}
