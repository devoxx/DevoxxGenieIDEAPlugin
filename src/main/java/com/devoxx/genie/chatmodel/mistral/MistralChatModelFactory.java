package com.devoxx.genie.chatmodel.mistral;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;

import java.time.Duration;

public class MistralChatModelFactory implements ChatModelFactory {

    private final String apiKey;
    private final String modelName;

    public MistralChatModelFactory(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @Override
    public ChatLanguageModel createChatModel(ChatModel chatModel) {
        return MistralAiChatModel.builder()
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
