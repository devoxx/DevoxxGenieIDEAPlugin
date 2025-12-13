package com.devoxx.genie.chatmodel.cloud.grok;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class GrokChatModelFactory implements ChatModelFactory {

    private static final ModelProvider MODEL_PROVIDER = ModelProvider.Grok;
    private static final String GROK_API_ENDPOINT = "https://api.x.ai/v1";

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        return OpenAiChatModel.builder()
                .apiKey(getApiKey(MODEL_PROVIDER))
                .modelName(customChatModel.getModelName())
                .defaultRequestParameters(createChatContextParameters(customChatModel))
                .maxRetries(customChatModel.getMaxRetries())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .listeners(getListener())
                .baseUrl(GROK_API_ENDPOINT)
                .build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(getApiKey(MODEL_PROVIDER))
                .defaultRequestParameters(createChatContextParameters(customChatModel))
                .modelName(customChatModel.getModelName())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .listeners(getListener())
                .baseUrl(GROK_API_ENDPOINT)
                .build();
    }

    @Override
    public List<LanguageModel> getModels() {
        return getModels(MODEL_PROVIDER);
    }

    private ChatRequestParameters createChatContextParameters(@NotNull CustomChatModel customChatModel) {
        return ChatRequestParameters.builder()
                .temperature(customChatModel.getTemperature())
                .topP(customChatModel.getTopP())
                .build();
    }
}
