package com.devoxx.genie.chatmodel.local.jan;

import com.devoxx.genie.chatmodel.local.LocalChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.jan.Data;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class JanChatModelFactory extends LocalChatModelFactory {

    public JanChatModelFactory() {
        super(ModelProvider.Jan);
    }

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return createLocalAiChatModel(chatModel);
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return createLocalAiStreamingChatModel(chatModel);
    }

    @Override
    protected String getModelUrl() {
        return DevoxxGenieStateService.getInstance().getJanModelUrl();
    }

    @Override
    protected Data[] fetchModels() throws IOException {
        return JanModelService.getInstance().getModels().toArray(new Data[0]);
    }

    @Override
    protected LanguageModel buildLanguageModel(Object model) {
        Data janModel = (Data) model;
        return LanguageModel.builder()
                .provider(modelProvider)
                .modelName(janModel.getId())
                .displayName(janModel.getName())
                .inputCost(0)
                .outputCost(0)
                .inputMaxTokens(janModel.getCtxLen() == null ? 8_000 : janModel.getSettings().getCtxLen())
                .apiKeyUsed(false)
                .build();
    }
}
