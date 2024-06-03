package com.devoxx.genie.chatmodel.anthropic;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.service.settings.SettingsStateService;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static dev.langchain4j.model.anthropic.AnthropicChatModelName.*;

public class AnthropicChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return AnthropicChatModel.builder()
            .apiKey(getApiKey())
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .maxTokens(chatModel.getMaxTokens())
            .maxRetries(chatModel.getMaxRetries())
            .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return AnthropicStreamingChatModel.builder()
            .apiKey(getApiKey())
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .maxTokens(chatModel.getMaxTokens())
            .build();
    }

    @Override
    public String getApiKey() {
        return SettingsStateService.getInstance().getAnthropicKey().trim();
    }

    @Override
    public List<String> getModelNames() {
        return List.of(
            CLAUDE_3_OPUS_20240229.toString(),
            CLAUDE_3_SONNET_20240229.toString(),
            CLAUDE_3_HAIKU_20240307.toString(),
            CLAUDE_2_1.toString(),
            CLAUDE_2.toString(),
            CLAUDE_INSTANT_1_2.toString()
        );
    }
}
