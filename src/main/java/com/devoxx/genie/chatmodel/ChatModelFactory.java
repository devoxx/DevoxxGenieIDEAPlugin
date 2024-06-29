package com.devoxx.genie.chatmodel;

import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

import java.util.List;

public interface ChatModelFactory {

    /**
     * Create a chat model with the given parameters.
     * @param chatModel the chat model
     * @return the chat model
     */
    ChatLanguageModel createChatModel(ChatModel chatModel);

    /**
     * Create a streaming chat model with the given parameters.
     * @param chatModel the chat model
     * @return the streaming chat model
     */
    default StreamingChatLanguageModel createStreamingChatModel(ChatModel chatModel) {
        return null;
    }

    /**
     * List the available model names.
     * @return the list of model names
     */
    default List<LanguageModel> getModelNames() {
        return List.of();
    }

    /**
     * Get the model provider API key.
     * @return the API key
     */
    default String getApiKey() {
        return "";
    }
}
