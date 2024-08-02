package com.devoxx.genie.chatmodel.google;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.gemini.GeminiChatModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class GoogleChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return GeminiChatModel.builder()
            .modelName(chatModel.getModelName())
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

    @Override
    public List<LanguageModel> getModels() {
        return getModels(ModelProvider.Google);
    }
}
