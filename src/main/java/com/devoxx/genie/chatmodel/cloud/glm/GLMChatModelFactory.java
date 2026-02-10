package com.devoxx.genie.chatmodel.cloud.glm;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class GLMChatModelFactory implements ChatModelFactory {

    private final ModelProvider MODEL_PROVIDER = ModelProvider.GLM;

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        return OpenAiChatModel.builder()
            .baseUrl("https://open.bigmodel.cn/api/paas/v4/")
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(customChatModel.getModelName())
            .maxRetries(customChatModel.getMaxRetries())
            .temperature(customChatModel.getTemperature())
            .maxTokens(4_000)
            .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
            .topP(customChatModel.getTopP())
            .listeners(getListener(customChatModel.getProject()))
            .build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        return OpenAiStreamingChatModel.builder()
            .baseUrl("https://open.bigmodel.cn/api/paas/v4/")
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(customChatModel.getModelName())
            .maxTokens(4_000)
            .temperature(customChatModel.getTemperature())
            .topP(customChatModel.getTopP())
            .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
            .listeners(getListener(customChatModel.getProject()))
            .build();
    }

    @Override
    public List<LanguageModel> getModels() {
        return getModels(ModelProvider.GLM);
    }
}
