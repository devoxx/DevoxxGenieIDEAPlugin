package com.devoxx.genie.chatmodel.deepseek;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class DeepSeekChatModelFactory implements ChatModelFactory {

    private final ModelProvider MODEL_PROVIDER = ModelProvider.DeepSeek;;

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return OpenAiChatModel.builder()
            .baseUrl("https://api.deepseek.com/")
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(chatModel.getModelName())
            .maxRetries(chatModel.getMaxRetries())
            .temperature(chatModel.getTemperature())
            .maxTokens(4_000)   // 8K Beta https://platform.deepseek.com/api-docs/quick_start/pricing
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .topP(chatModel.getTopP())
            .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return OpenAiStreamingChatModel.builder()
            .baseUrl("https://api.deepseek.com/")
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(chatModel.getModelName())
            .maxTokens(4_000)
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    @Override
    public List<LanguageModel> getModels() {
        return getModels(ModelProvider.DeepSeek);
    }
}
