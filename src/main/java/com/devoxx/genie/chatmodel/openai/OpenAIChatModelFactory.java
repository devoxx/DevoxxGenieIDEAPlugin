package com.devoxx.genie.chatmodel.openai;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;

import java.time.Duration;
import java.util.List;

public class OpenAIChatModelFactory implements ChatModelFactory {

    private String apiKey;
    private String modelName;

    public OpenAIChatModelFactory() {}

    public OpenAIChatModelFactory(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @Override
    public ChatLanguageModel createChatModel(ChatModel chatModel) {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .maxRetries(chatModel.getMaxRetries())
            .temperature(chatModel.getTemperature())
            .maxTokens(chatModel.getMaxTokens())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .topP(chatModel.getTopP())
            .build();
    }

    @Override
    public List<String> getModelNames() {
        return List.of(
            "gpt-4o",
            OpenAiChatModelName.GPT_4.toString(),
            OpenAiChatModelName.GPT_4_32K.toString(),
            OpenAiChatModelName.GPT_4_TURBO_PREVIEW.toString(),
            OpenAiChatModelName.GPT_3_5_TURBO.toString(),
            OpenAiChatModelName.GPT_3_5_TURBO_16K.toString());
    }
}
