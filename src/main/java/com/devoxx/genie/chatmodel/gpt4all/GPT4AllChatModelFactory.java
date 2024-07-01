package com.devoxx.genie.chatmodel.gpt4all;

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
import java.util.ArrayList;
import java.util.List;

public class GPT4AllChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return LocalAiChatModel.builder()
            .baseUrl(DevoxxGenieStateService.getInstance().getGpt4allModelUrl())
            .modelName("test-model")
            .maxRetries(chatModel.getMaxRetries())
            .maxTokens(chatModel.getMaxTokens())
            .temperature(chatModel.getTemperature())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .topP(chatModel.getTopP())
            .build();
    }

    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return LocalAiStreamingChatModel.builder()
            .baseUrl(DevoxxGenieStateService.getInstance().getGpt4allModelUrl())
            .modelName("test-model")
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    @Override
    public List<LanguageModel> getModels() {
        LanguageModel lmStudio = LanguageModel.builder()
            .provider(ModelProvider.GPT4All)
            .modelName("GPT4All")
            .inputCost(0)
            .outputCost(0)
            .contextWindow(8000)
            .build();

        List<LanguageModel> modelNames = new ArrayList<>();
        modelNames.add(lmStudio);
        return modelNames;
    }
}
