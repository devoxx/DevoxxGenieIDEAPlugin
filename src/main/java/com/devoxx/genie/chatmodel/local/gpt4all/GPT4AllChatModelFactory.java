package com.devoxx.genie.chatmodel.local.gpt4all;

import com.devoxx.genie.chatmodel.LocalChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.gpt4all.Model;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class GPT4AllChatModelFactory extends LocalChatModelFactory {

    public GPT4AllChatModelFactory() {
        super(ModelProvider.GPT4All);
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
        return DevoxxGenieStateService.getInstance().getGpt4allModelUrl();
    }

    @Override
    protected Model[] fetchModels() throws IOException {
        return GPT4AllModelService.getInstance().getModels().toArray(new Model[0]);
    }

    @Override
    protected LanguageModel buildLanguageModel(Object model) {
        Model gpt4AllModel = (Model) model;
        return LanguageModel.builder()
                .provider(modelProvider)
                .modelName(gpt4AllModel.getId())
                .displayName(gpt4AllModel.getId())
                .inputCost(0)
                .outputCost(0)
                // .contextWindow(contextWindow)    // GPT4All does not provide context window :(
                .apiKeyUsed(false)
                .build();
    }
}
