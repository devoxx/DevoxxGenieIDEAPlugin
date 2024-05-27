package com.devoxx.genie.chatmodel.openai;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.service.SettingsStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class OpenAIChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return OpenAiChatModel.builder()
            .apiKey(getApiKey())
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
        return OpenAiStreamingChatModel.builder()
            .apiKey(getApiKey())
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    @Override
    public String getApiKey() {
        return SettingsStateService.getInstance().getOpenAIKey();
    }

    @Override
    public List<String> getModelNames() {
        return List.of(
            "gpt-4o",
            OpenAiChatModelName.GPT_4.toString(),
            OpenAiChatModelName.GPT_4_32K.toString(),
            OpenAiChatModelName.GPT_4_TURBO_PREVIEW.toString(),
            OpenAiChatModelName.GPT_3_5_TURBO.toString(),
            OpenAiChatModelName.GPT_3_5_TURBO_16K.toString());
    }
}
