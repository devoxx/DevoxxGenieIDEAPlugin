package com.devoxx.genie.chatmodel;

import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.SettingsState;
import dev.langchain4j.model.chat.ChatLanguageModel;

public interface ChatModelFactory {

    /**
     * Create a chat model with the given parameters.
     * @param chatModel the chat model
     * @return the chat model
     */
    ChatLanguageModel createChatModel(ChatModel chatModel);

    /**
     * Get the base URL by the model type.
     * @param modelProvider the language model provider
     * @return the base URL
     */
    default String getBaseUrlByType(ModelProvider modelProvider) {

        return switch (modelProvider) {
            case GPT4All -> SettingsState.getInstance().getGpt4allModelUrl();
            case LMStudio -> SettingsState.getInstance().getLmstudioModelUrl();
            case Ollama -> SettingsState.getInstance().getOllamaModelUrl();
            default -> "na";
        };
    }
}
