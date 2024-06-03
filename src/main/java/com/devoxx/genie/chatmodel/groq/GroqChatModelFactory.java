package com.devoxx.genie.chatmodel.groq;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.service.settings.SettingsStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class GroqChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return OpenAiChatModel.builder()
            .baseUrl("https://api.groq.com/openai/v1")
            .apiKey(getApiKey())
            .modelName(chatModel.getModelName())
            .maxRetries(chatModel.getMaxRetries())
            .maxTokens(chatModel.getMaxTokens())
            .temperature(chatModel.getTemperature())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .topP(chatModel.getTopP())
            .build();
    }

//    Streaming gives error for Groq model provider
//    @Override
//    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
//        return OpenAiStreamingChatModel.builder()
//            .apiKey(getApiKey())
//            .modelName(chatModel.getModelName())
//            .temperature(chatModel.getTemperature())
//            .topP(chatModel.getTopP())
//            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
//            .build();
//    }

    @Override
    public String getApiKey() {
        return SettingsStateService.getInstance().getGroqKey().trim();
    }

    @Override
    public List<String> getModelNames() {
        return List.of(
            "gemma-7b-it",
            "llama3-8b-8192",
            "llama3-70b-8192",
            "llama2-70b-4096",
            "mixtral-8x7b-32768"
        );
    }
}
