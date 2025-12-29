package com.devoxx.genie.chatmodel.cloud.anthropic;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class AnthropicChatModelFactory implements ChatModelFactory {

    private static final ModelProvider MODEL_PROVIDER = ModelProvider.Anthropic;

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {

        return AnthropicChatModel.builder()
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(customChatModel.getModelName())
            .temperature(customChatModel.getTemperature())
            .maxTokens(customChatModel.getMaxTokens())
            .maxRetries(customChatModel.getMaxRetries())
            .listeners(getListener())
            .build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        return AnthropicStreamingChatModel.builder()
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(customChatModel.getModelName())
            .temperature(customChatModel.getTemperature())
            .maxTokens(customChatModel.getMaxTokens())
            .listeners(getListener())
            .build();
    }

    @Override
    public List<LanguageModel> getModels() {
        return getModels(MODEL_PROVIDER);
    }
}
