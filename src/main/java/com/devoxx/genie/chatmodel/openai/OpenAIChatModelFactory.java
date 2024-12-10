package com.devoxx.genie.chatmodel.openai;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.jgoodies.common.base.Strings;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class OpenAIChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        boolean isO1 = chatModel.getModelName().startsWith("o1-");

        final var builder = OpenAiChatModel.builder()
                .apiKey(getApiKey())
                .modelName(chatModel.getModelName())
                .maxRetries(chatModel.getMaxRetries())
                .temperature(isO1 ? 1.0 : chatModel.getTemperature())
                .timeout(Duration.ofSeconds(chatModel.getTimeout()))
                .topP(isO1 ? 1.0 : chatModel.getTopP());

        if (Strings.isNotBlank(DevoxxGenieStateService.getInstance().getCustomOpenAIUrl())) {
            builder.baseUrl(DevoxxGenieStateService.getInstance().getCustomOpenAIUrl());
        }

        return builder.build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        boolean isO1 = chatModel.getModelName().startsWith("o1-");
        final var builder = OpenAiStreamingChatModel.builder()
                .apiKey(getApiKey())
                .modelName(chatModel.getModelName())
                .temperature(isO1 ? 1.0 : chatModel.getTemperature())
                .topP(isO1 ? 1.0 : chatModel.getTopP())
                .timeout(Duration.ofSeconds(chatModel.getTimeout()));

        if (Strings.isNotBlank(DevoxxGenieStateService.getInstance().getCustomOpenAIUrl())) {
            builder.baseUrl(DevoxxGenieStateService.getInstance().getCustomOpenAIUrl());
        }
        return builder.build();
    }

    @Override
    public String getApiKey() {
        return DevoxxGenieStateService.getInstance().getOpenAIKey().trim();
    }

    @Override
    public List<LanguageModel> getModels() {
        return getModels(ModelProvider.OPENAI);
    }
}
