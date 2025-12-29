package com.devoxx.genie.chatmodel.cloud.google;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GoogleChatModelFactory implements ChatModelFactory {

    private final ModelProvider MODEL_PROVIDER = ModelProvider.Google;;

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        return GoogleAiGeminiChatModel.builder()
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(customChatModel.getModelName())
            .temperature(customChatModel.getTemperature())
            .maxOutputTokens(customChatModel.getMaxTokens())
            .listeners(getListener())
            .build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(getApiKey(MODEL_PROVIDER))
                .modelName(customChatModel.getModelName())
                .temperature(customChatModel.getTemperature())
                .maxOutputTokens(customChatModel.getMaxTokens())
                .listeners(getListener())
                .build();
    }

    @Override
    public List<LanguageModel> getModels() {
        return getModels(MODEL_PROVIDER);
    }
}
