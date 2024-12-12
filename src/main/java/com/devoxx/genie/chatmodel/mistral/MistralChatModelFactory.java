package com.devoxx.genie.chatmodel.mistral;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class MistralChatModelFactory implements ChatModelFactory {

    public static final ModelProvider MODEL_PROVIDER = ModelProvider.Mistral;

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return MistralAiChatModel.builder()
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(chatModel.getModelName())
            .maxRetries(chatModel.getMaxRetries())
            .temperature(chatModel.getTemperature())
            .maxTokens(chatModel.getMaxTokens())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .topP(chatModel.getTopP())
            .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return MistralAiStreamingChatModel.builder()
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    @Override
    public List<LanguageModel> getModels() {
        return getModels(MODEL_PROVIDER);
    }
}
