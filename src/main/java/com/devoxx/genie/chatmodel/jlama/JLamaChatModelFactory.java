package com.devoxx.genie.chatmodel.jlama;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class JLamaChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return LocalAiChatModel.builder()
            .baseUrl(DevoxxGenieStateService.getInstance().getJlamaUrl())
            .modelName(chatModel.getModelName())
            .maxRetries(chatModel.getMaxRetries())
            .temperature(chatModel.getTemperature())
            .maxTokens(chatModel.getMaxTokens())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .topP(chatModel.getTopP())
            .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return LocalAiStreamingChatModel.builder()
            .baseUrl(DevoxxGenieStateService.getInstance().getJlamaUrl())
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    /**
     * Get the model names from the Jlama service.
     * @return List of model names
     */
    @Override
    public List<LanguageModel> getModels() {
        return Collections.emptyList();
    }
}
