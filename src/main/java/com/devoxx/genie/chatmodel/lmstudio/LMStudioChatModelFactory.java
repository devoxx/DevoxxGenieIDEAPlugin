package com.devoxx.genie.chatmodel.lmstudio;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.SettingsState;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.localai.LocalAiChatModel;

import java.time.Duration;
import java.util.List;

public class LMStudioChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(ChatModel chatModel) {
        chatModel.setBaseUrl(SettingsState.getInstance().getLmstudioModelUrl());
        return LocalAiChatModel.builder()
            .baseUrl(getBaseUrlByType(ModelProvider.LMStudio))
            .modelName("LMStudio")
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .maxTokens(chatModel.getMaxTokens())
            .maxRetries(chatModel.getMaxRetries())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    @Override
    public List<String> getModelNames() {
        return List.of("");
    }
}
