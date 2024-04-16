package com.devoxx.genie.chatmodel.deepinfra;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;

public class DeepInfraChatModelFactory implements ChatModelFactory {

    private final String apiKey;
    private final String modelName;

    public DeepInfraChatModelFactory(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @Override
    public ChatLanguageModel createChatModel(ChatModel chatModel) {
        return OpenAiChatModel.builder()
            .baseUrl("https://api.deepinfra.com/v1/openai")
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
