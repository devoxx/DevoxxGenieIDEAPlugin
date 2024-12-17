package com.devoxx.genie.chatmodel;

import com.devoxx.genie.chatmodel.anthropic.AnthropicChatModelFactory;
import com.devoxx.genie.chatmodel.azureopenai.AzureOpenAIChatModelFactory;
import com.devoxx.genie.chatmodel.customopenai.CustomOpenAIChatModelFactory;
import com.devoxx.genie.chatmodel.deepinfra.DeepInfraChatModelFactory;
import com.devoxx.genie.chatmodel.deepseek.DeepSeekChatModelFactory;
import com.devoxx.genie.chatmodel.google.GoogleChatModelFactory;
import com.devoxx.genie.chatmodel.gpt4all.GPT4AllChatModelFactory;
import com.devoxx.genie.chatmodel.groq.GroqChatModelFactory;
import com.devoxx.genie.chatmodel.jan.JanChatModelFactory;
import com.devoxx.genie.chatmodel.lmstudio.LMStudioChatModelFactory;
import com.devoxx.genie.chatmodel.mistral.MistralChatModelFactory;
import com.devoxx.genie.chatmodel.ollama.OllamaChatModelFactory;
import com.devoxx.genie.chatmodel.openai.OpenAIChatModelFactory;
import com.devoxx.genie.chatmodel.openrouter.OpenRouterChatModelFactory;
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
            case "CustomOpenAI" -> new CustomOpenAIChatModelFactory();
            case "DeepInfra" -> new DeepInfraChatModelFactory();
            case "DeepSeek" -> new DeepSeekChatModelFactory();
            case "Google" -> new GoogleChatModelFactory();
            case "Groq" -> new GroqChatModelFactory();
            case "GPT4All" -> new GPT4AllChatModelFactory();
            case "Jan" -> new JanChatModelFactory();
            case "LMStudio" -> new LMStudioChatModelFactory();
            case "Mistral" -> new MistralChatModelFactory();
            case "Ollama" -> new OllamaChatModelFactory();
            case "OpenAI" -> new OpenAIChatModelFactory();
            case "OpenRouter" -> new OpenRouterChatModelFactory();
            default -> null;
        };
    }
}
