package com.devoxx.genie.model.enumarations;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public enum ModelProvider {
    Ollama("Ollama"),
    LMStudio("LMStudio"),
    GPT4All("GPT4All"),
    OpenAI("OpenAI"),
    Anthropic("Anthropic"),
    Mistral("Mistral"),
    Groq("Groq"),
    DeepInfra("DeepInfra"),
    Gemini("Gemini"),
    Jan("Jan");

    private final String name;

    ModelProvider(String name) {
        this.name = name;
    }

    public static @NotNull ModelProvider fromString(String name) {
        for (ModelProvider provider : ModelProvider.values()) {
            if (provider.getName().equals(name)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("No enum found with name: [" + name + "]");
    }
}
