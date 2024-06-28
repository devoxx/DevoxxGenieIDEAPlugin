package com.devoxx.genie.chatmodel.anthropic;

import com.devoxx.genie.chatmodel.AbstractChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import static dev.langchain4j.model.anthropic.AnthropicChatModelName.*;

public class AnthropicChatModelFactory extends AbstractChatModelFactory {

    public AnthropicChatModelFactory() {
        LANGUAGE_MODELS.add(new LanguageModel("claude-3-5-sonnet-20240620", "Claude 3.5 Sonnet", 200_000,  3.0d, 15.0d));
        LANGUAGE_MODELS.add(new LanguageModel(CLAUDE_3_OPUS_20240229.toString(), "Claude 3 Opus", 200_000,  15.0d, 75.0d));
        LANGUAGE_MODELS.add(new LanguageModel(CLAUDE_3_SONNET_20240229.toString(), "Claude 3 Sonnet",200_000,  3.0d, 15.0d));
        LANGUAGE_MODELS.add(new LanguageModel(CLAUDE_3_HAIKU_20240307.toString(), "Claude 3 Haiku",200_000,  0.25d, 1.25d));
        LANGUAGE_MODELS.add(new LanguageModel(CLAUDE_2_1.toString(), "Claude 2.1",100_000,  8.0d, 24.0d));
        LANGUAGE_MODELS.add(new LanguageModel(CLAUDE_2.toString(), "Claude 2.0",100_000,  8.0d, 24.0d));
        LANGUAGE_MODELS.add(new LanguageModel(CLAUDE_INSTANT_1_2.toString(), "Claude 1.2",100_000,  0.8d, 2.4d));
    }

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
        return DevoxxGenieStateService.getInstance().getAnthropicKey().trim();
    }
}
