package com.devoxx.genie.chatmodel.cloud.grok;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
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
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return OpenAiChatModel.builder()
                .apiKey(getApiKey(MODEL_PROVIDER))
                .modelName(chatModel.getModelName())
                .defaultRequestParameters(createChatContextParameters(chatModel))
                .maxRetries(chatModel.getMaxRetries())
                .timeout(Duration.ofSeconds(chatModel.getTimeout()))
                .listeners(getListener())
                .baseUrl(GROK_API_ENDPOINT)
                .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(getApiKey(MODEL_PROVIDER))
                .defaultRequestParameters(createChatContextParameters(chatModel))
                .modelName(chatModel.getModelName())
                .timeout(Duration.ofSeconds(chatModel.getTimeout()))
                .listeners(getListener())
                .baseUrl(GROK_API_ENDPOINT)
                .build();
    }

    @Override
    public List<LanguageModel> getModels() {
        return getModels(MODEL_PROVIDER);
    }

    private ChatRequestParameters createChatContextParameters(@NotNull ChatModel chatModel) {
        return ChatRequestParameters.builder()
                .temperature(chatModel.getTemperature())
                .topP(chatModel.getTopP())
                .build();
    }
}
