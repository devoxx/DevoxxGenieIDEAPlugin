package com.devoxx.genie.chatmodel.local.rapidmlx;

import com.devoxx.genie.chatmodel.local.LocalChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.rapidmlx.RapidMLXModelEntryDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Rapid-MLX serves local MLX models on Apple Silicon through an OpenAI-compatible API.
 */
public class RapidMLXChatModelFactory extends LocalChatModelFactory {

    /** Used when the server does not report a positive context window. */
    public static final int DEFAULT_CONTEXT_LENGTH = 8000;

    public RapidMLXChatModelFactory() {
        super(ModelProvider.RapidMLX);
    }

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        return createOpenAiChatModel(customChatModel);
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        return createOpenAiStreamingChatModel(customChatModel);
    }

    @Override
    protected String getModelUrl() {
        return DevoxxGenieStateService.getInstance().getRapidMLXModelUrl();
    }

    @Override
    protected RapidMLXModelEntryDTO[] fetchModels() throws IOException {
        return RapidMLXModelService.getInstance().getModels().toArray(new RapidMLXModelEntryDTO[0]);
    }

    @Override
    protected LanguageModel buildLanguageModel(Object model) {
        RapidMLXModelEntryDTO rapidMLXModel = (RapidMLXModelEntryDTO) model;
        return LanguageModel.builder()
                .provider(modelProvider)
                .modelName(rapidMLXModel.getId())
                .displayName(rapidMLXModel.resolveDisplayName())
                .inputCost(0)
                .outputCost(0)
                .inputMaxTokens(rapidMLXModel.resolveContextLength(DEFAULT_CONTEXT_LENGTH))
                .apiKeyUsed(false)
                .build();
    }
}
