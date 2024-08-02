package com.devoxx.genie.model.enumarations;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public enum ModelProvider {
    Ollama("Ollama"),
    LMStudio("LMStudio"),
    GPT4All("GPT4All"),
    Jan("Jan"),
    OpenAI("OpenAI"),
    Anthropic("Anthropic"),
    Mistral("Mistral"),
    Groq("Groq"),
    DeepInfra("DeepInfra"),
    Google("Google"),
    Exo("Exo (Experimental)"),
    LLaMA("LLaMA.c++");

    private final String name;

    ModelProvider(String name) {
        this.name = name;
    }

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
}
