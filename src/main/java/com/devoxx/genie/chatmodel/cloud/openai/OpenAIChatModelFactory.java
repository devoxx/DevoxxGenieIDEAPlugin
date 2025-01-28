package com.devoxx.genie.chatmodel.cloud.openai;

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

public class OpenAIChatModelFactory implements ChatModelFactory {

    private static final ModelProvider MODEL_PROVIDER = ModelProvider.OpenAI;

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        boolean isO1 = chatModel.getModelName().startsWith("o1-");

        final var builder = OpenAiChatModel.builder()
                .apiKey(getApiKey(MODEL_PROVIDER))
                .modelName(chatModel.getModelName())
                .maxRetries(chatModel.getMaxRetries())
                .temperature(isO1 ? 1.0 : chatModel.getTemperature())
                .timeout(Duration.ofSeconds(chatModel.getTimeout()))
                .topP(isO1 ? 1.0 : chatModel.getTopP());

        return builder.build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        boolean isO1 = chatModel.getModelName().startsWith("o1-");
        final var builder = OpenAiStreamingChatModel.builder()
                .apiKey(getApiKey(MODEL_PROVIDER))
                .modelName(chatModel.getModelName())
                .temperature(isO1 ? 1.0 : chatModel.getTemperature())
                .topP(isO1 ? 1.0 : chatModel.getTopP())
                .timeout(Duration.ofSeconds(chatModel.getTimeout()));

        return builder.build();
    }

    @Override
    public List<LanguageModel> getModels() {
        return getModels(MODEL_PROVIDER);
    }
}
