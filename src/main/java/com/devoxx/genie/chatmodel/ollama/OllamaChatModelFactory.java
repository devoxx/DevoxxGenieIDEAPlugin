package com.devoxx.genie.chatmodel.ollama;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.time.Duration;

public class OllamaChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(ChatModel chatModel) {
        return OllamaChatModel.builder()
            .baseUrl(getBaseUrlByType(ModelProvider.Ollama))
            .modelName(chatModel.name)
            .temperature(chatModel.temperature)
            .topP(chatModel.topP)
            .maxRetries(chatModel.maxRetries)
            .timeout(Duration.ofSeconds(chatModel.timeout))
            .build();
    }
}
