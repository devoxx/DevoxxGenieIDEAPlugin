package com.devoxx.genie.chatmodel;

import com.devoxx.genie.chatmodel.cloud.anthropic.AnthropicChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.azureopenai.AzureOpenAIChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.bedrock.BedrockModelFactory;
import com.devoxx.genie.chatmodel.cloud.deepinfra.DeepInfraChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.deepseek.DeepSeekChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.glm.GLMChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.google.GoogleChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.grok.GrokChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.groq.GroqChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.kimi.KimiChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.mistral.MistralChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.nvidia.NvidiaChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.openai.OpenAIChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.openrouter.OpenRouterChatModelFactory;
import com.devoxx.genie.chatmodel.local.acprunners.AcpRunnersChatModelFactory;
import com.devoxx.genie.chatmodel.local.clirunners.CliRunnersChatModelFactory;
import com.devoxx.genie.chatmodel.local.customopenai.CustomOpenAIChatModelFactory;
import com.devoxx.genie.chatmodel.local.exo.ExoChatModelFactory;
import com.devoxx.genie.chatmodel.local.gpt4all.GPT4AllChatModelFactory;
import com.devoxx.genie.chatmodel.local.jan.JanChatModelFactory;
import com.devoxx.genie.chatmodel.local.llamacpp.LlamaChatModelFactory;
import com.devoxx.genie.chatmodel.local.lmstudio.LMStudioChatModelFactory;
import com.devoxx.genie.chatmodel.local.ollama.OllamaChatModelFactory;
import com.devoxx.genie.model.enumarations.ModelProvider;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Resolves the {@link ChatModelFactory} for a given provider.
 * <p>
 * The single source of truth for the set of providers is the {@link ModelProvider} enum; this
 * class only wires each provider to its factory constructor. Suppliers are stored un-invoked so
 * factories are instantiated lazily (and then cached) on first use — no factory is built until its
 * provider is actually requested.
 * <p>
 * Lookups accept <em>either</em> the enum constant name ({@link ModelProvider#name()}, used by
 * {@code ChatModelProvider} and {@code LlmProviderPanel}) <em>or</em> the display name
 * ({@link ModelProvider#getName()}, used by {@code AgentSettingsComponent}). These differ for
 * {@code LLaMA} ("LLaMA.c++"), {@code CLIRunners} ("CLI Runners") and {@code ACPRunners}
 * ("ACP Runners"); accepting both keeps every existing caller working regardless of which key it
 * passes. Matching is case-sensitive.
 */
public final class ChatModelFactoryProvider {

    private ChatModelFactoryProvider() {
    }

    /**
     * Provider → factory-constructor registry. Adding a provider is a single entry here; the
     * {@code ChatModelFactoryProviderTest} completeness check fails if a {@link ModelProvider}
     * constant is added to the enum but not wired here.
     */
    private static final Map<ModelProvider, Supplier<ChatModelFactory>> FACTORY_SUPPLIERS =
            new EnumMap<>(ModelProvider.class);

    static {
        FACTORY_SUPPLIERS.put(ModelProvider.Anthropic, AnthropicChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.AzureOpenAI, AzureOpenAIChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.Bedrock, BedrockModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.CustomOpenAI, CustomOpenAIChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.Exo, ExoChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.DeepInfra, DeepInfraChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.DeepSeek, DeepSeekChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.Google, GoogleChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.Groq, GroqChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.GPT4All, GPT4AllChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.Jan, JanChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.LLaMA, LlamaChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.LMStudio, LMStudioChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.Mistral, MistralChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.Ollama, OllamaChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.OpenAI, OpenAIChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.OpenRouter, OpenRouterChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.Grok, GrokChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.Kimi, KimiChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.GLM, GLMChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.Nvidia, NvidiaChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.CLIRunners, CliRunnersChatModelFactory::new);
        FACTORY_SUPPLIERS.put(ModelProvider.ACPRunners, AcpRunnersChatModelFactory::new);
    }

    /** Lazily-instantiated factory instances, keyed by the resolved provider. */
    private static final Map<ModelProvider, ChatModelFactory> factoryCache = new ConcurrentHashMap<>();

    public static @NotNull Optional<ChatModelFactory> getFactoryByProvider(@NotNull String modelProvider) {
        return resolveProvider(modelProvider)
                .map(provider -> factoryCache.computeIfAbsent(provider, p -> FACTORY_SUPPLIERS.get(p).get()));
    }

    /**
     * Resolve a provider key to its {@link ModelProvider}, matching either the enum constant name
     * or the display name (case-sensitive). Only providers present in {@link #FACTORY_SUPPLIERS}
     * are returned, so the result maps directly to an available factory.
     */
    private static @NotNull Optional<ModelProvider> resolveProvider(@NotNull String key) {
        return FACTORY_SUPPLIERS.keySet().stream()
                .filter(provider -> provider.name().equals(key) || provider.getName().equals(key))
                .findFirst();
    }
}
