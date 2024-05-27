package com.devoxx.genie.chatmodel.gemini;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.gemini.GeminiChatModel;
import com.devoxx.genie.service.SettingsStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class GeminiChatModelFactory implements ChatModelFactory {

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
        return SettingsStateService.getInstance().getGeminiKey();
    }

    @Override
    public List<String> getModelNames() {
        return List.of(
            "gemini-pro",
            "gemini-1.5-pro-latest",
            "gemini-1.5-flash-latest"
        );
    }
}
