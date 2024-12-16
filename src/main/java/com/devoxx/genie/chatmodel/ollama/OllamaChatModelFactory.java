package com.devoxx.genie.chatmodel.ollama;

import com.devoxx.genie.chatmodel.LocalChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.ollama.OllamaApiService;
import com.devoxx.genie.service.ollama.OllamaService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;

public class OllamaChatModelFactory extends LocalChatModelFactory {

    public OllamaChatModelFactory() {
        super(ModelProvider.Ollama);
    }

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return OllamaChatModel.builder()
                .baseUrl(DevoxxGenieStateService.getInstance().getOllamaModelUrl())
                .modelName(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .topP(chatModel.getTopP())
                .maxRetries(chatModel.getMaxRetries())
                .timeout(Duration.ofSeconds(chatModel.getTimeout()))
                .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return OllamaStreamingChatModel.builder()
                .baseUrl(DevoxxGenieStateService.getInstance().getOllamaModelUrl())
                .modelName(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .topP(chatModel.getTopP())
                .timeout(Duration.ofSeconds(chatModel.getTimeout()))
                .build();
    }

    @Override
    protected String getModelUrl() {
        return DevoxxGenieStateService.getInstance().getOllamaModelUrl();
    }

    @Override
    protected OllamaModelEntryDTO[] fetchModels() throws IOException {
        return OllamaService.getInstance().getModels();
    }

    @Override
    protected LanguageModel buildLanguageModel(Object model) throws IOException {
        OllamaModelEntryDTO ollamaModel = (OllamaModelEntryDTO) model;
        int contextWindow = OllamaApiService.getModelContext(ollamaModel.getName());
        return LanguageModel.builder()
                .provider(modelProvider)
                .modelName(ollamaModel.getName())
                .displayName(ollamaModel.getName())
                .inputCost(0)
                .outputCost(0)
                .contextWindow(contextWindow)
                .apiKeyUsed(false)
                .build();
    }
}
