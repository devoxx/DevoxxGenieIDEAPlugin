package com.devoxx.genie.chatmodel.cloud.anthropic;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.mcp.MCPListenerService;
import com.devoxx.genie.service.mcp.MCPService;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class AnthropicChatModelFactory implements ChatModelFactory {

    private static final ModelProvider MODEL_PROVIDER = ModelProvider.Anthropic;

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {

        return AnthropicChatModel.builder()
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .maxTokens(chatModel.getMaxTokens())
            .maxRetries(chatModel.getMaxRetries())
            .listeners(getListener())
            .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return AnthropicStreamingChatModel.builder()
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .maxTokens(chatModel.getMaxTokens())
            .listeners(getListener())
            .build();
    }

    @Override
    public List<LanguageModel> getModels() {
        return getModels(MODEL_PROVIDER);
    }
}
