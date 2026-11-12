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
    LLaMA("llama.cpp", Type.LOCAL, "LLaMA.c++"),
    LMStudio("LMStudio", Type.LOCAL),
    Nativ("Nativ", Type.LOCAL),
    RapidMLX("Rapid-MLX", Type.LOCAL),
    Exo("Exo", Type.LOCAL),
    Ollama("Ollama", Type.LOCAL),
    CLIRunners("CLI Runners", Type.LOCAL),
    ACPRunners("ACP Runners", Type.LOCAL),

    OpenAI("OpenAI", Type.CLOUD),
    Anthropic("Anthropic", Type.CLOUD),
    Mistral("Mistral", Type.CLOUD),
    Groq("Groq", Type.CLOUD),
    DeepInfra("DeepInfra", Type.CLOUD),
    Google("Google", Type.CLOUD),
    OpenRouter("OpenRouter", Type.ROUTER),
    OrcaRouter("OrcaRouter", Type.ROUTER),
    Requesty("Requesty", Type.ROUTER),
    DemonRoute("DemonRoute", Type.ROUTER),
    DeepSeek("DeepSeek", Type.CLOUD),
    Grok("Grok", Type.CLOUD),
    Kimi("Kimi", Type.CLOUD),
    GLM("GLM", Type.CLOUD),
    Nvidia("NVIDIA", Type.CLOUD),
    Cloudflare("Cloudflare", Type.ROUTER),

    AzureOpenAI("AzureOpenAI", Type.OPTIONAL),
    Bedrock("Bedrock", Type.OPTIONAL);

    public enum Type {
        LOCAL, // Local Providers
        CLOUD, // Cloud Providers
        ROUTER, // LLM routers (OpenRouter, Cloudflare AI Gateway, ...)
        OPTIONAL // Optional Providers(Need to be enabled from settings, due to inconvenient setup)
    }

    private final String name;
    private final Type type;
    /** Former display names, still found in saved settings (selected provider, sub-agent configs). */
    private final List<String> legacyNames;

    ModelProvider(String name, Type type, String... legacyNames) {
        this.name = name;
        this.type = type;
        this.legacyNames = List.of(legacyNames);
    }

    /** Whether {@code text} is this provider's display name or one of its former names, ignoring case. */
    public boolean hasName(String text) {
        return name.equalsIgnoreCase(text) || legacyNames.stream().anyMatch(legacy -> legacy.equalsIgnoreCase(text));
    }

    /** Whether {@code text} is exactly one of this provider's former display names. */
    public boolean isLegacyName(String text) {
        return legacyNames.contains(text);
    }

    @Override
    public String toString() {
        return name;
    }

    public static @NotNull ModelProvider fromString(String text) {
        for (ModelProvider provider : ModelProvider.values()) {
            if (provider.hasName(text)) {
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
