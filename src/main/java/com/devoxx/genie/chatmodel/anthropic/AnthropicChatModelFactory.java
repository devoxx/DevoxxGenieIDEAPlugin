package com.devoxx.genie.chatmodel.anthropic;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.List;

import static dev.langchain4j.model.anthropic.AnthropicChatModelName.*;
import static dev.langchain4j.model.anthropic.AnthropicChatModelName.CLAUDE_INSTANT_1_2;

public class AnthropicChatModelFactory implements ChatModelFactory {

    private String apiKey;
    private String modelName;

    public AnthropicChatModelFactory() {
    }

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

    @Override
    public List<String> getModelNames() {
        return List.of(
            CLAUDE_3_OPUS_20240229.toString(),
            CLAUDE_3_SONNET_20240229.toString(),
            CLAUDE_3_HAIKU_20240307.toString(),
            CLAUDE_2_1.toString(),
            CLAUDE_2.toString(),
            CLAUDE_INSTANT_1_2.toString()
        );
    }

}
