package com.devoxx.genie.chatmodel;

import com.devoxx.genie.chatmodel.cloud.anthropic.AnthropicChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.azureopenai.AzureOpenAIChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.bedrock.BedrockModelFactory;
import com.devoxx.genie.chatmodel.cloud.deepinfra.DeepInfraChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.deepseek.DeepSeekChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.google.GoogleChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.grok.GrokChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.glm.GLMChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.kimi.KimiChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.groq.GroqChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.mistral.MistralChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.openai.OpenAIChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.openrouter.OpenRouterChatModelFactory;
import com.devoxx.genie.chatmodel.local.customopenai.CustomOpenAIChatModelFactory;
import com.devoxx.genie.chatmodel.local.gpt4all.GPT4AllChatModelFactory;
import com.devoxx.genie.chatmodel.local.jan.JanChatModelFactory;
import com.devoxx.genie.chatmodel.local.acprunners.AcpRunnersChatModelFactory;
import com.devoxx.genie.chatmodel.local.clirunners.CliRunnersChatModelFactory;
import com.devoxx.genie.chatmodel.local.llamacpp.LlamaChatModelFactory;
import com.devoxx.genie.chatmodel.local.lmstudio.LMStudioChatModelFactory;
import com.devoxx.genie.chatmodel.local.ollama.OllamaChatModelFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ChatModelFactoryProvider {

    private ChatModelFactoryProvider() {
        throw new IllegalStateException("Utility class");
    }

    private static final Map<String, ChatModelFactory> factoryCache = new ConcurrentHashMap<>();

    public static @NotNull Optional<ChatModelFactory> getFactoryByProvider(@NotNull String modelProvider) {
        return Optional.ofNullable(factoryCache.computeIfAbsent(modelProvider, ChatModelFactoryProvider::createFactory));
    }

    /**
     * Get the factory by provider.
     *
     * @param modelProvider the model provider
     * @return the factory
     */
    private static @Nullable ChatModelFactory createFactory(@NotNull String modelProvider) {
        return switch (modelProvider) {
            case "Anthropic" -> new AnthropicChatModelFactory();
            case "AzureOpenAI" -> new AzureOpenAIChatModelFactory();
            case "Bedrock" -> new BedrockModelFactory();
            case "CustomOpenAI" -> new CustomOpenAIChatModelFactory();
            case "DeepInfra" -> new DeepInfraChatModelFactory();
            case "DeepSeek" -> new DeepSeekChatModelFactory();
            case "Google" -> new GoogleChatModelFactory();
            case "Groq" -> new GroqChatModelFactory();
            case "GPT4All" -> new GPT4AllChatModelFactory();
            case "Jan" -> new JanChatModelFactory();
            case "LLaMA" -> new LlamaChatModelFactory();
            case "LMStudio" -> new LMStudioChatModelFactory();
            case "Mistral" -> new MistralChatModelFactory();
            case "Ollama" -> new OllamaChatModelFactory();
            case "OpenAI" -> new OpenAIChatModelFactory();
            case "OpenRouter" -> new OpenRouterChatModelFactory();
            case "Grok" -> new GrokChatModelFactory();
            case "Kimi" -> new KimiChatModelFactory();
            case "GLM" -> new GLMChatModelFactory();
            case "CLI Runners" -> new CliRunnersChatModelFactory();
            case "ACP Runners" -> new AcpRunnersChatModelFactory();
            default -> null;
        };
    }
}
