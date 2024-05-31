package com.devoxx.genie.chatmodel.mistral;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.service.SettingsStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

import static dev.langchain4j.model.mistralai.MistralAiChatModelName.*;

public class MistralChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return MistralAiChatModel.builder()
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
        return MistralAiStreamingChatModel.builder()
            .apiKey(getApiKey())
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    @Override
    public String getApiKey() {
        return SettingsStateService.getInstance().getMistralKey().trim();
    }

    @Override
    public List<String> getModelNames() {
        return List.of(
            OPEN_MISTRAL_7B.toString(),
            OPEN_MIXTRAL_8x7B.toString(),
            MISTRAL_SMALL_LATEST.toString(),
            MISTRAL_MEDIUM_LATEST.toString(),
            MISTRAL_LARGE_LATEST.toString()
        );
    }
}
