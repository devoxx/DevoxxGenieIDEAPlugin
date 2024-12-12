package com.devoxx.genie.chatmodel.groq;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class GroqChatModelFactory implements ChatModelFactory {

    private final ModelProvider MODEL_PROVIDER = ModelProvider.Groq;

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return OpenAiChatModel.builder()
            .baseUrl("https://api.groq.com/openai/v1")
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(chatModel.getModelName())
            .maxRetries(chatModel.getMaxRetries())
            .maxTokens(chatModel.getMaxTokens())
            .temperature(chatModel.getTemperature())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .topP(chatModel.getTopP())
            .build();
    }

//    Streaming gives error for Groq model provider
//    @Override
//    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
//        return OpenAiStreamingChatModel.builder()
//            .apiKey(getApiKey())
//            .modelName(chatModel.getModelName())
//            .temperature(chatModel.getTemperature())
//            .topP(chatModel.getTopP())
//            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
//            .build();
//    }

    @Override
    public List<LanguageModel> getModels() {
        return getModels(MODEL_PROVIDER);
    }
}
