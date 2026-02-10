package com.devoxx.genie.chatmodel.local.llamacpp;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class LlamaChatModelFactory implements ChatModelFactory {

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        return OpenAiChatModel.builder()
            .baseUrl(DevoxxGenieStateService.getInstance().getLlamaCPPUrl())
            .apiKey("na")
            .modelName(customChatModel.getModelName())
            .temperature(customChatModel.getTemperature())
            .topP(customChatModel.getTopP())
            .maxRetries(customChatModel.getMaxRetries())
            .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
            .listeners(getListener(customChatModel.getProject()))
            .build();
    }

    @Override
    public List<LanguageModel> getModels() {
        LanguageModel model = LanguageModel.builder()
            .provider(ModelProvider.LLaMA)
            .modelName(TEST_MODEL)
            .displayName(TEST_MODEL)
            .inputCost(0)
            .outputCost(0)
            .inputMaxTokens(8000)
            .apiKeyUsed(false)
            .build();

        List<LanguageModel> modelNames = new ArrayList<>();
        modelNames.add(model);
        return modelNames;
    }
}

