package com.devoxx.genie.chatmodel;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.chatmodel.anthropic.AnthropicChatModelFactory;
import com.devoxx.genie.chatmodel.deepinfra.DeepInfraChatModelFactory;
import com.devoxx.genie.chatmodel.groq.GroqChatModelFactory;
import com.devoxx.genie.chatmodel.mistral.MistralChatModelFactory;
import com.devoxx.genie.chatmodel.ollama.OllamaChatModelFactory;
import com.devoxx.genie.chatmodel.openai.OpenAIChatModelFactory;
import com.devoxx.genie.chatmodel.gemini.GeminiChatModelFactory;
import com.devoxx.genie.chatmodel.jan.JanChatModelFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ChatModelFactoryProvider {

    /**
     * The chat language model factory suppliers.
     */
    private static final Map<ModelProvider, Supplier<ChatModelFactory>> FACTORY_SUPPLIERS = Map.of(
        ModelProvider.Ollama, OllamaChatModelFactory::new,
        ModelProvider.OpenAI, OpenAIChatModelFactory::new,
        ModelProvider.Anthropic, AnthropicChatModelFactory::new,
        ModelProvider.Mistral, MistralChatModelFactory::new,
        ModelProvider.Groq, GroqChatModelFactory::new,
        ModelProvider.DeepInfra, DeepInfraChatModelFactory::new,
        ModelProvider.Gemini, GeminiChatModelFactory::new,
        ModelProvider.Jan, JanChatModelFactory::new
        );

    /**
     * Get the factory by provider.
     * @param provider the provider
     * @return the factory
     */
    public static @NotNull Optional<ChatModelFactory> getFactoryByProvider(@NotNull ModelProvider provider) {
        return Optional.ofNullable(FACTORY_SUPPLIERS.get(provider)).map(Supplier::get);
    }
}
