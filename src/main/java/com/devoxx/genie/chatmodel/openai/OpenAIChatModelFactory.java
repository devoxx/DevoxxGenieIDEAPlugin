package com.devoxx.genie.chatmodel.openai;

import com.devoxx.genie.chatmodel.AbstractChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class OpenAIChatModelFactory extends AbstractChatModelFactory {

    public OpenAIChatModelFactory() {
        LANGUAGE_MODELS.add(new LanguageModel(OpenAiChatModelName.GPT_4_O.toString(), "GPT 4o", 128_000, 5d, 15d));
        LANGUAGE_MODELS.add(new LanguageModel(OpenAiChatModelName.GPT_4_TURBO_PREVIEW.toString(), "GPT 4 Turbo", 128_000, 10d, 30d));
        LANGUAGE_MODELS.add(new LanguageModel(OpenAiChatModelName.GPT_4.toString(), "GPT 4", 8_000, 30d, 60d));
        LANGUAGE_MODELS.add(new LanguageModel(OpenAiChatModelName.GPT_3_5_TURBO.toString(), "GPT 3.5", 16_000, 0.5d, 1.5d));
    }

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
        return DevoxxGenieStateService.getInstance().getOpenAIKey().trim();
    }
}
