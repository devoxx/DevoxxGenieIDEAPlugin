package com.devoxx.genie.chatmodel;

import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.models.LLMModelRegistryService;
import com.devoxx.genie.service.LLMProviderService;
import com.devoxx.genie.service.mcp.MCPListenerService;
import com.devoxx.genie.service.mcp.MCPService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;

import java.util.List;

public interface ChatModelFactory {

    String TEST_MODEL = "test-model";

    /**
     * Create a chat model with the given parameters.
     *
     * @param chatModel the chat model
     * @return the chat model
     */
    ChatLanguageModel createChatModel(ChatModel chatModel);

    /**
     * Create a streaming chat model with the given parameters.
     *
     * @param chatModel the chat model
     * @return the streaming chat model
     */
    default StreamingChatLanguageModel createStreamingChatModel(ChatModel chatModel) {
        return null;
    }

    /**
     * Get available models for selected provider
     *
     * @return the list of models
     */
    default List<LanguageModel> getModels(ModelProvider provider) {
        return LLMModelRegistryService.getInstance().getModels()
            .stream()
            .filter(model -> model.getProvider().equals(provider))
            .toList();
    }

    /**
     * Get available models for selected provider
     *
     * @return the list of models
     */
    List<LanguageModel> getModels();

    default String getApiKey(ModelProvider modelProvider) {
        return LLMProviderService.getInstance().getApiKey(modelProvider).trim();
    }

    /**
     * Reset the list of local models
     */
    default void resetModels() {}

    default List<ChatModelListener> getListener() {
        if (MCPService.isMCPEnabled()) {
            return List.of(new MCPListenerService());
        }
        return List.of();
    }
}
