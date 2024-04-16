package com.devoxx.genie.chatmodel.openai;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;

public class OpenAIChatModelFactory implements ChatModelFactory {

    private final String apiKey;
    private final String modelName;

    public OpenAIChatModelFactory(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @Override
    public ChatLanguageModel createChatModel(ChatModel chatModel) {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .maxRetries(chatModel.maxRetries)
            .temperature(chatModel.temperature)
            .maxTokens(chatModel.maxTokens)
            .timeout(Duration.ofSeconds(chatModel.timeout))
            .topP(chatModel.topP)
            .build();
    }
}
