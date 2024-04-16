package com.devoxx.genie.chatmodel.groq;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;

public class GroqChatModelFactory implements ChatModelFactory {

    private final String apiKey;
    private final String modelName;

    public GroqChatModelFactory(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @Override
    public ChatLanguageModel createChatModel(ChatModel chatModel) {
        return OpenAiChatModel.builder()
            .baseUrl("https://api.groq.com/openai/v1")
            .apiKey(apiKey)
            .modelName(modelName)
            .maxRetries(chatModel.maxRetries)
            .maxTokens(chatModel.maxTokens)
            .temperature(chatModel.temperature)
            .timeout(Duration.ofSeconds(chatModel.timeout))
            .topP(chatModel.topP)
            .build();
    }
}
