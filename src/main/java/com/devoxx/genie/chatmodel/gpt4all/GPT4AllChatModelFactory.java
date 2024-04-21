package com.devoxx.genie.chatmodel.gpt4all;


import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.localai.LocalAiChatModel;

import java.time.Duration;
import java.util.List;

public class GPT4AllChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(ChatModel chatModel) {
        return LocalAiChatModel.builder()
            .baseUrl(getBaseUrlByType(ModelProvider.GPT4All))
            .modelName("test-model")
            .maxRetries(chatModel.maxRetries)
            .maxTokens(chatModel.maxTokens)
            .temperature(chatModel.temperature)
            .timeout(Duration.ofSeconds(chatModel.timeout))
            .topP(chatModel.topP)
            .build();
    }

    @Override
    public List<String> getModelNames() {
        return List.of();
    }
}
