package com.devoxx.genie.chatmodel.local.lmstudio;

import com.devoxx.genie.chatmodel.local.LocalChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.lmstudio.LMStudioModelEntryDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;

public class LMStudioChatModelFactory extends LocalChatModelFactory {

    public static final int DEFAULT_CONTEXT_LENGTH = 8000;

    public LMStudioChatModelFactory() {
        super(ModelProvider.LMStudio);
    }

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return LMStudioChatModel.builder()
                .baseUrl(getModelUrl())
                .modelName(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .topP(chatModel.getTopP())
                .maxTokens(chatModel.getMaxTokens())
                .maxRetries(chatModel.getMaxRetries())
                .timeout(Duration.ofSeconds(chatModel.getTimeout()))
                .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return createLocalAiStreamingChatModel(chatModel);
    }

    @Override
    protected String getModelUrl() {
        return DevoxxGenieStateService.getInstance().getLmstudioModelUrl();
    }

    @Override
    protected LMStudioModelEntryDTO[] fetchModels() throws IOException {
        return LMStudioModelService.getInstance().getModels();
    }

    @Override
    protected LanguageModel buildLanguageModel(Object model) {
        LMStudioModelEntryDTO lmStudioModel = (LMStudioModelEntryDTO) model;
        return LanguageModel.builder()
                .provider(modelProvider)
                .modelName(lmStudioModel.getId())
                .displayName(lmStudioModel.getId())
                .inputCost(0)
                .outputCost(0)
                .inputMaxTokens(lmStudioModel.getMax_context_length()!=null?lmStudioModel.getMax_context_length():DEFAULT_CONTEXT_LENGTH)
                .apiKeyUsed(false)
                .build();
    }
}
