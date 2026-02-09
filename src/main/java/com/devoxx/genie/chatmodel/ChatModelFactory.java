package com.devoxx.genie.chatmodel;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.models.LLMModelRegistryService;
import com.devoxx.genie.service.LLMProviderService;
import com.devoxx.genie.service.mcp.MCPListenerService;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;

import java.util.List;

public interface ChatModelFactory {

    String TEST_MODEL = "test-model";

    /**
     * Create a chat model with the given parameters.
     *
     * @param customChatModel the chat model
     * @return the chat model
     */
    ChatModel createChatModel(CustomChatModel customChatModel);

    /**
     * Create a streaming chat model with the given parameters.
     *
     * @param customChatModel the chat model
     * @return the streaming chat model
     */
    default StreamingChatModel createStreamingChatModel(CustomChatModel customChatModel) {
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
        // Attach MCPListenerService when MCP is enabled (for MCP tool logging)
        // or when agent mode is enabled (for intermediate response logging to Agent Logs)
        if (MCPService.isMCPEnabled() ||
                Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentModeEnabled())) {
            return List.of(new MCPListenerService());
        }
        return List.of();
    }
}
