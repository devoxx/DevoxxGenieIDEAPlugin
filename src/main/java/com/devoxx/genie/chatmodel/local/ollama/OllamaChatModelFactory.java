package com.devoxx.genie.chatmodel.local.ollama;

import com.devoxx.genie.chatmodel.local.LocalChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
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
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {

        var builder = OllamaChatModel.builder()
                .baseUrl(DevoxxGenieStateService.getInstance().getOllamaModelUrl())
                .modelName(customChatModel.getModelName())
                .temperature(customChatModel.getTemperature())
                .topP(customChatModel.getTopP())
                .maxRetries(customChatModel.getMaxRetries())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .listeners(getListener(customChatModel.getProject()));

        // Pass context window to Ollama if available (fixes issue #804)
        if (customChatModel.getContextWindow() != null) {
            builder.numCtx(customChatModel.getContextWindow());
        }

        return builder.build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        var builder = OllamaStreamingChatModel.builder()
                .baseUrl(DevoxxGenieStateService.getInstance().getOllamaModelUrl())
                .modelName(customChatModel.getModelName())
                .temperature(customChatModel.getTemperature())
                .topP(customChatModel.getTopP())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()));

        // Pass context window to Ollama if available (fixes issue #804)
        if (customChatModel.getContextWindow() != null) {
            builder.numCtx(customChatModel.getContextWindow());
        }

        return builder.build();
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
