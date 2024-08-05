package com.devoxx.genie.chatmodel.exo;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.DevoxxGenieSettingsServiceProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ExoChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return LocalAiChatModel.builder()
            .baseUrl(DevoxxGenieSettingsServiceProvider.getInstance().getExoModelUrl())
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .maxRetries(chatModel.getMaxRetries())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

// TODO: Currently gives an error in regards to content-type
//    @Override
//    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
//        return LocalAiStreamingChatModel.builder()
//            .baseUrl(DevoxxGenieStateService.getInstance().getExoModelUrl())
//            .modelName(chatModel.getModelName())
//            .temperature(chatModel.getTemperature())
//            .topP(chatModel.getTopP())
//            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
//            .build();
//    }

    /**
     * Get the models for Exo
     * @return List of model names
     */
    @Override
    public List<LanguageModel> getModels() {

        // 'llama-3-8b', 'llama-3.1-8b', 'llama-3.1-70b', 'llama-3.1-405b', 'llama-3-70b']

        LanguageModel model2 = LanguageModel.builder()
            .modelName("llama-3.1-405b")
            .displayName("Llama 3.1 405B")
            .apiKeyUsed(false)
            .provider(ModelProvider.Exo)
            .outputCost(0)
            .inputCost(0)
            .contextWindow(131_000)
            .build();

        LanguageModel model1 = LanguageModel.builder()
            .modelName("llama-3.1-8b")
            .displayName("Llama 3.1 8B")
            .apiKeyUsed(false)
            .provider(ModelProvider.Exo)
            .outputCost(0)
            .inputCost(0)
            .contextWindow(8_000)
            .build();

        LanguageModel model3 = LanguageModel.builder()
            .modelName("llama-3.1-70b")
            .displayName("Llama 3.1 70B")
            .apiKeyUsed(false)
            .provider(ModelProvider.Exo)
            .outputCost(0)
            .inputCost(0)
            .contextWindow(131_000)
            .build();

        LanguageModel model4 = LanguageModel.builder()
            .modelName("llama-3-8b")
            .displayName("Llama 3 8B")
            .apiKeyUsed(false)
            .provider(ModelProvider.Exo)
            .outputCost(0)
            .inputCost(0)
            .contextWindow(8_000)
            .build();

        List<LanguageModel> modelNames = new ArrayList<>();
        modelNames.add(model1);
        modelNames.add(model2);
        modelNames.add(model3);
        modelNames.add(model4);

        return modelNames;
    }
}

