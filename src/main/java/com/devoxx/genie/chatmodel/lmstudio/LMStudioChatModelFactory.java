package com.devoxx.genie.chatmodel.lmstudio;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.localai.LocalAiChatModel;

import java.time.Duration;

public class LMStudioChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(ChatModel chatModel) {
        return LocalAiChatModel.builder()
            .baseUrl(getBaseUrlByType(ModelProvider.LMStudio))
            .modelName("LMStudio")
            .temperature(chatModel.temperature)
            .topP(chatModel.topP)
            .maxTokens(chatModel.maxTokens)
            .maxRetries(chatModel.maxRetries)
            .timeout(Duration.ofSeconds(chatModel.timeout))
            .build();
    }
}
