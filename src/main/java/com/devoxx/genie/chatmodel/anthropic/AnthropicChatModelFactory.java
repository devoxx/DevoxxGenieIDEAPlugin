package com.devoxx.genie.chatmodel.anthropic;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

public class AnthropicChatModelFactory implements ChatModelFactory {

    private final String apiKey;
    private final String modelName;

    public AnthropicChatModelFactory(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @Override
    public ChatLanguageModel createChatModel(ChatModel chatModel) {
        return AnthropicChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .temperature(chatModel.temperature)
            .topP(chatModel.topP)
            .maxTokens(chatModel.maxTokens)
            .maxRetries(chatModel.maxRetries)
            .build();
    }

}
