package com.devoxx.genie.chatmodel;

import com.devoxx.genie.chatmodel.anthropic.AnthropicChatModelFactory;
import com.devoxx.genie.chatmodel.gemini.GeminiChatModelFactory;
import com.devoxx.genie.chatmodel.gpt4all.GPT4AllChatModelFactory;
import com.devoxx.genie.chatmodel.groq.GroqChatModelFactory;
import com.devoxx.genie.chatmodel.jan.JanChatModelFactory;
import com.devoxx.genie.chatmodel.lmstudio.LMStudioChatModelFactory;
import com.devoxx.genie.chatmodel.mistral.MistralChatModelFactory;
import com.devoxx.genie.chatmodel.ollama.OllamaChatModelFactory;
import com.devoxx.genie.chatmodel.openai.OpenAIChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Setter
public class ChatModelProvider {

    private final Map<ModelProvider, ChatModelFactory> factories = new HashMap<>();

    public ChatModelProvider() {
        factories.put(ModelProvider.Ollama, new OllamaChatModelFactory());
        factories.put(ModelProvider.LMStudio, new LMStudioChatModelFactory());
        factories.put(ModelProvider.GPT4All, new GPT4AllChatModelFactory());
        factories.put(ModelProvider.OpenAI, new OpenAIChatModelFactory());
        factories.put(ModelProvider.Mistral, new MistralChatModelFactory());
        factories.put(ModelProvider.Anthropic, new AnthropicChatModelFactory());
        factories.put(ModelProvider.Groq, new GroqChatModelFactory());
        factories.put(ModelProvider.Gemini, new GeminiChatModelFactory());
        factories.put(ModelProvider.Jan, new JanChatModelFactory());
    }

    /**
     * Get the chat language model for selected model provider.
     *
     * @param chatMessageContext the chat message context
     * @return the chat language model
     */
    public ChatLanguageModel getChatLanguageModel(@NotNull ChatMessageContext chatMessageContext) {
        ChatModel chatModel = initChatModel(chatMessageContext);
        return getFactory(chatMessageContext).createChatModel(chatModel);
    }

    /**
     * Get the streaming chat language model for selected model provider.
     *
     * @param chatMessageContext the chat message context
     * @return the streaming chat language model
     */
    public StreamingChatLanguageModel getStreamingChatLanguageModel(@NotNull ChatMessageContext chatMessageContext) {
        ChatModel chatModel = initChatModel(chatMessageContext);
        return getFactory(chatMessageContext).createStreamingChatModel(chatModel);
    }

    /**
     * Get the chat model factory for the selected model provider.
     *
     * @param chatMessageContext the chat message context
     * @return the chat model factory
     */
    private @NotNull ChatModelFactory getFactory(@NotNull ChatMessageContext chatMessageContext) {
        ModelProvider provider = ModelProvider.fromString(chatMessageContext.getLlmProvider());
        ChatModelFactory factory = factories.get(provider);
        if (factory == null) {
            throw new IllegalArgumentException("No factory for provider: " + provider);
        }
        return factory;
    }

    /**
     * Initialize chat model settings by default or by user settings.
     *
     * @return the chat model
     */
    public @NotNull ChatModel initChatModel(@NotNull ChatMessageContext chatMessageContext) {
        ChatModel chatModel = new ChatModel();
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        setMaxOutputTokens(stateService, chatModel);

        chatModel.setTemperature(stateService.getTemperature());
        chatModel.setMaxRetries(stateService.getMaxRetries());
        chatModel.setTopP(stateService.getTopP());
        chatModel.setTimeout(stateService.getTimeout());
        chatModel.setModelName(chatMessageContext.getModelName());
        return chatModel;
    }

    /**
     * Set max output tokens.
     * Some extra work because of the settings state that didn't like the integer input field.
     *
     * @param settingsState the settings state
     * @param chatModel     the chat model
     */
    private static void setMaxOutputTokens(@NotNull DevoxxGenieStateService settingsState, ChatModel chatModel) {
        Integer maxOutputTokens = settingsState.getMaxOutputTokens();
        if (maxOutputTokens == null) {
            chatModel.setMaxTokens(Constant.MAX_OUTPUT_TOKENS);
        } else {
            try {
                chatModel.setMaxTokens(maxOutputTokens);
            } catch (NumberFormatException e) {
                chatModel.setMaxTokens(Constant.MAX_OUTPUT_TOKENS);
            }
        }
    }
}
