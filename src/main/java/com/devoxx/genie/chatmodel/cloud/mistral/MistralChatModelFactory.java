package com.devoxx.genie.chatmodel.cloud.mistral;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class MistralChatModelFactory implements ChatModelFactory {

    public static final ModelProvider MODEL_PROVIDER = ModelProvider.Mistral;

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        return MistralAiChatModel.builder()
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(customChatModel.getModelName())
            .maxRetries(customChatModel.getMaxRetries())
            .temperature(customChatModel.getTemperature())
            .maxTokens(customChatModel.getMaxTokens())
            .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
            .topP(customChatModel.getTopP())
            .build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        return MistralAiStreamingChatModel.builder()
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(customChatModel.getModelName())
            .temperature(customChatModel.getTemperature())
            .topP(customChatModel.getTopP())
            .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
            .build();
    }

    @Override
    public List<LanguageModel> getModels() {
        return getModels(MODEL_PROVIDER);
    }
}
