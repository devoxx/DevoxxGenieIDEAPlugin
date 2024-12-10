package com.devoxx.genie.chatmodel.exo;

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

public class ExoChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return LocalAiChatModel.builder()
            .baseUrl(DevoxxGenieStateService.getInstance().getExoModelUrl())
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .maxRetries(chatModel.getMaxRetries())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    // TODO: Currently gives an error in regards to content-type
    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return LocalAiStreamingChatModel.builder()
            .baseUrl(DevoxxGenieStateService.getInstance().getExoModelUrl())
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    /**
     * Get the models for Exo
     *
     * @return List of model names
     */
    @Override
    public List<LanguageModel> getModels() {

        // 'llama-3-8b', 'llama-3.1-8b', 'llama-3.1-70b', 'llama-3.1-405b', 'llama-3-70b']

        LanguageModel model2 = LanguageModel.builder()
            .modelName("llama-3.1-405b")
            .displayName("Llama 3.1 405B")
            .apiKeyUsed(false)
            .provider(ModelProvider.EXO)
            .outputCost(0)
            .inputCost(0)
            .contextWindow(131_000)
            .build();

        LanguageModel model1 = LanguageModel.builder()
            .modelName("llama-3.1-8b")
            .displayName("Llama 3.1 8B")
            .apiKeyUsed(false)
            .provider(ModelProvider.EXO)
            .outputCost(0)
            .inputCost(0)
            .contextWindow(8_000)
            .build();

        LanguageModel model3 = LanguageModel.builder()
            .modelName("llama-3.1-70b")
            .displayName("Llama 3.1 70B")
            .apiKeyUsed(false)
            .provider(ModelProvider.EXO)
            .outputCost(0)
            .inputCost(0)
            .contextWindow(131_000)
            .build();

        LanguageModel model4 = LanguageModel.builder()
            .modelName("llama-3-8b")
            .displayName("Llama 3 8B")
            .apiKeyUsed(false)
            .provider(ModelProvider.EXO)
            .outputCost(0)
            .inputCost(0)
            .contextWindow(8_000)
            .build();

        // mistral-nemo
        LanguageModel model5 = LanguageModel.builder()
            .modelName("mistral-nemo")
            .displayName("Mistral Nemo")
            .apiKeyUsed(false)
            .provider(ModelProvider.EXO)
            .outputCost(0)
            .inputCost(0)
            .contextWindow(8_000)
            .build();

        // mistral-large
        LanguageModel model6 = LanguageModel.builder()
            .modelName("mistral-large")
            .displayName("Mistral Large")
            .apiKeyUsed(false)
            .provider(ModelProvider.EXO)
            .outputCost(0)
            .inputCost(0)
            .contextWindow(8_000)
            .build();

        // deepseek-coder-v2-lite
        LanguageModel model7 = LanguageModel.builder()
            .modelName("deepseek-coder-v2-lite")
            .displayName("Deepseek Coder V2 Lite")
            .apiKeyUsed(false)
            .provider(ModelProvider.EXO)
            .outputCost(0)
            .inputCost(0)
            .contextWindow(8_000)
            .build();

        // llava-1.5-7b-hf
        LanguageModel model8 = LanguageModel.builder()
            .modelName("llava-1.5-7b-hf")
            .displayName("Llava 1.5 7B HF")
            .apiKeyUsed(false)
            .provider(ModelProvider.EXO)
            .outputCost(0)
            .inputCost(0)
            .contextWindow(8_000)
            .build();

        List<LanguageModel> modelNames = new ArrayList<>();
        modelNames.add(model1);
        modelNames.add(model2);
        modelNames.add(model3);
        modelNames.add(model4);
        modelNames.add(model5);
        modelNames.add(model6);
        modelNames.add(model7);
        modelNames.add(model8);

        return modelNames;
    }
}

