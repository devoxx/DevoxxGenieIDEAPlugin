package com.devoxx.genie.chatmodel.groq;

import com.devoxx.genie.chatmodel.AbstractChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class GroqChatModelFactory extends AbstractChatModelFactory {

    public GroqChatModelFactory() {
        LANGUAGE_MODELS.add(new LanguageModel("gemma-7b-it", "Gemma 7B it", 8_192));
        LANGUAGE_MODELS.add(new LanguageModel("llama3-8b-8192", "Llama 3 8B", 8_192));
        LANGUAGE_MODELS.add(new LanguageModel("llama3-70b-8192", "Llama 3 70B", 8_192));
        LANGUAGE_MODELS.add(new LanguageModel("llama2-70b-4096", "Llama 2 70B", 4_096));
        LANGUAGE_MODELS.add(new LanguageModel("mixtral-8x7b-32768", "Mixtral 8x7B", 32_768));
    }

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return OpenAiChatModel.builder()
            .baseUrl("https://api.groq.com/openai/v1")
            .apiKey(getApiKey())
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
    public String getApiKey() {
        return DevoxxGenieStateService.getInstance().getGroqKey().trim();
    }
}
