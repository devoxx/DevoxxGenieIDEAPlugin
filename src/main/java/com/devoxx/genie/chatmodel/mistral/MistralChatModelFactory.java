package com.devoxx.genie.chatmodel.mistral;

import com.devoxx.genie.chatmodel.AbstractChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

import static dev.langchain4j.model.mistralai.MistralAiChatModelName.*;

public class MistralChatModelFactory extends AbstractChatModelFactory {

    public MistralChatModelFactory() {
        LANGUAGE_MODELS.add(new LanguageModel(OPEN_MISTRAL_7B.toString(), "Mistral 7B", 32_000));
        LANGUAGE_MODELS.add(new LanguageModel(OPEN_MIXTRAL_8x7B.toString(), "Mixtral 8x7B", 32_000));
        LANGUAGE_MODELS.add(new LanguageModel(MISTRAL_SMALL_LATEST.toString(), "Mistral Small", 32_000));
        LANGUAGE_MODELS.add(new LanguageModel(MISTRAL_MEDIUM_LATEST.toString(), "Mistral Medium", 32_000));
        LANGUAGE_MODELS.add(new LanguageModel(MISTRAL_LARGE_LATEST.toString(), "Mistral Large", 32_000));
    }

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return MistralAiChatModel.builder()
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
        return MistralAiStreamingChatModel.builder()
            .apiKey(getApiKey())
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    @Override
    public String getApiKey() {
        return DevoxxGenieStateService.getInstance().getMistralKey().trim();
    }
}
