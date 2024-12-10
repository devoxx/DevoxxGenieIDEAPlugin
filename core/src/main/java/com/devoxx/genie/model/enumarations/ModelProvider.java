package com.devoxx.genie.model.enumarations;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ModelProvider {
    OPENAI("OpenAI", Type.CLOUD),
    ANTHROPIC("Anthropic", Type.CLOUD),
    MISTRAL("Mistral", Type.CLOUD),
    GROQ("Groq", Type.CLOUD),
    DEEP_INFRA("DeepInfra", Type.CLOUD),
    GOOGLE("Google", Type.CLOUD),
    LLAMA("LLaMA.c++", Type.LOCAL),
    OPEN_ROUTER("OpenRouter", Type.CLOUD),
    DEEP_SEEK("DeepSeek", Type.CLOUD),
    AZURE_OPEN_AI("AzureOpenAI", Type.CLOUD),
    OLLAMA("Ollama", Type.LOCAL),
    LMSTUDIO("LMStudio", Type.LOCAL),
    GPT_4_ALL("GPT4All", Type.LOCAL),
    JAN("Jan", Type.LOCAL),
    JLAMA("Jlama (Experimental /w REST API)", Type.LOCAL),
    EXO("Exo (Experimental)", Type.LOCAL),
    CUSTOM_OPEN_AI("CustomOpenAI", Type.OPTIONAL);

    public enum Type {
        LOCAL, // Local Providers
        CLOUD, // Cloud Providers
        OPTIONAL // Optional Providers (Need to be enabled from settings, due to inconvenient setup)
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
