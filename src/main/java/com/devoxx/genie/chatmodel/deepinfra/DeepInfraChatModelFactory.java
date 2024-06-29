package com.devoxx.genie.chatmodel.deepinfra;

import com.devoxx.genie.chatmodel.AbstractChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class DeepInfraChatModelFactory extends AbstractChatModelFactory {

    public DeepInfraChatModelFactory() {
        super(ModelProvider.DeepInfra);
        LANGUAGE_MODELS.add(new LanguageModel("meta-llama/Meta-Llama-3-70B-Instruct", "Meta Llama 3 70B", 8_000, 0.56d, 0.77d));
        LANGUAGE_MODELS.add(new LanguageModel("meta-llama/Meta-Llama-3-8B-Instruct", "Meta Llama 3 8B Instruct",8_000, 0.064d, 0.064d));
        LANGUAGE_MODELS.add(new LanguageModel("mistralai/Mixtral-8x7B-Instruct-v0.1", "Mixtral 8x7B Instruct v0.1",32_000, 0.24d, 0.24d));
        LANGUAGE_MODELS.add(new LanguageModel("mistralai/Mixtral-8x22B-Instruct-v0.1", "Mixtral 8x22B Instruct v0.1", 64_000, 0.65d, 0.65d));
        LANGUAGE_MODELS.add(new LanguageModel("mistralai/Mistral-7B-Instruct-v0.3", "Mistral 7B Instruct v0.3", 32_000, 0.07d, 0.07d));
        LANGUAGE_MODELS.add(new LanguageModel("microsoft/WizardLM-2-8x22B", "Wizard LM 2 8x22B", 64_000, 0.65d, 0.65d));
        LANGUAGE_MODELS.add(new LanguageModel("microsoft/WizardLM-2-7B", "Wizard LM 2 7B", 32_000, 0.07d, 0.07d));
        LANGUAGE_MODELS.add(new LanguageModel("openchat/openchat_3.5", "OpenChat 3.5", 8_000, 0.07, 0.07d));
        LANGUAGE_MODELS.add(new LanguageModel("google/gemma-1.1-7b-it", "Gemma 1.1 7B it", 8_000, 0.07d, 0.07d));
        LANGUAGE_MODELS.add(new LanguageModel("Phind/Phind-CodeLlama-34B-v2", "Phind CodeLlama 34B v2", 4_000, 0.6d, 06d));
        LANGUAGE_MODELS.add(new LanguageModel("cognitivecomputations/dolphin-2.6-mixtral-8x7b", "Dolphin 2.6 Mixtral 8x7B",32_000, 0.24d, 0.24d));
        updateModelCosts();
    }

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return OpenAiChatModel.builder()
            .baseUrl("https://api.deepinfra.com/v1/openai")
            .apiKey(getApiKey())
            .modelName(chatModel.getModelName())
            .maxRetries(chatModel.getMaxRetries())
            .temperature(chatModel.getTemperature())
            .maxTokens(chatModel.getMaxTokens())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .topP(chatModel.getTopP())
            .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return OpenAiStreamingChatModel.builder()
            .baseUrl("https://api.deepinfra.com/v1/openai")
            .apiKey(getApiKey())
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    @Override
    public String getApiKey() {
        return DevoxxGenieStateService.getInstance().getDeepInfraKey().trim();
    }
}
