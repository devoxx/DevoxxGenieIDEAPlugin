package com.devoxx.genie.chatmodel.gemini;

import com.devoxx.genie.chatmodel.AbstractChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.gemini.GeminiChatModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class GeminiChatModelFactory extends AbstractChatModelFactory {

    public GeminiChatModelFactory() {
        LANGUAGE_MODELS.add(new LanguageModel("gemini-pro", "Gemini Pro", 1_000_000, 0.5d, 1.5d));
        LANGUAGE_MODELS.add(new LanguageModel("gemini-1.5-pro-latest", "Gemini 1.5 Pro", 1_000_000, 7.0d, 21.0d));
        LANGUAGE_MODELS.add(new LanguageModel("gemini-1.5-flash-latest", "Gemini 1.5 Flash", 1_000_000, 0.7d, 2.1d));
    }

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return GeminiChatModel.builder()
            .modelName(chatModel.getModelName())
            .baseUrl("https://generativelanguage.googleapis.com")
            .apiKey(getApiKey())
            .temperature(chatModel.getTemperature())
            .maxTokens(chatModel.getMaxTokens())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    @Override
    public String getApiKey() {
        return DevoxxGenieStateService.getInstance().getGeminiKey().trim();
    }
}
