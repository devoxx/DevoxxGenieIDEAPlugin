package com.devoxx.genie.chatmodel.local.ollama;

import com.devoxx.genie.chatmodel.local.LocalChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.mcp.MCPListenerService;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

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
                .listeners(getListener())
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
        return OllamaModelService.getInstance().getModels();
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
                .inputMaxTokens(contextWindow)
                .apiKeyUsed(false)
                .build();
    }
}
