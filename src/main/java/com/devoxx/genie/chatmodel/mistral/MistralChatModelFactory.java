package com.devoxx.genie.chatmodel.mistral;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;

import java.time.Duration;
import java.util.List;

import static dev.langchain4j.model.mistralai.MistralAiChatModelName.*;

public class MistralChatModelFactory implements ChatModelFactory {

    private String apiKey;
    private String modelName;

    public MistralChatModelFactory() {
    }

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

    @Override
    public List<String> getModelNames() {
        return List.of(
            OPEN_MISTRAL_7B.toString(),
            OPEN_MIXTRAL_8x7B.toString(),
            MISTRAL_SMALL_LATEST.toString(),
            MISTRAL_MEDIUM_LATEST.toString()
        );
    }
}
