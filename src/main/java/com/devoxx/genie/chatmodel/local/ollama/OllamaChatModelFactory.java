package com.devoxx.genie.chatmodel.local.ollama;

import com.devoxx.genie.chatmodel.ThinkingSupport;
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
                .listeners(getListener());

        applyThinkingSetting(builder);

        // Only send num_ctx when the user explicitly enabled request-time overriding.
        if (customChatModel.getContextWindowOverride() != null) {
            builder.numCtx(customChatModel.getContextWindowOverride());
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
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .listeners(getListener());

        applyThinkingSetting(builder);

        // Only send num_ctx when the user explicitly enabled request-time overriding.
        if (customChatModel.getContextWindowOverride() != null) {
            builder.numCtx(customChatModel.getContextWindowOverride());
        }

        return builder.build();
    }

    // Unlike the client-side-only returnThinking flag, think(true) is sent to the Ollama API,
    // so both are applied conditionally to keep the request unchanged when the setting is off.
    private void applyThinkingSetting(@NotNull OllamaChatModel.OllamaChatModelBuilder builder) {
        if (ThinkingSupport.isEnabled()) {
            builder.think(true).returnThinking(true);
        }
    }

    private void applyThinkingSetting(@NotNull OllamaStreamingChatModel.OllamaStreamingChatModelBuilder builder) {
        if (ThinkingSupport.isEnabled()) {
            builder.think(true).returnThinking(true);
        }
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
