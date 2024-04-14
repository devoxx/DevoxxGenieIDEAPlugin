package com.devoxx.genie.chatmodel;

import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.intellij.ide.util.PropertiesComponent;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.Objects;

import static com.devoxx.genie.ui.Settings.*;

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
            case GPT4All -> Objects.requireNonNullElse(
                PropertiesComponent.getInstance().getValue(GPT4ALL_MODEL_URL),
                Constant.GPT4ALL_MODEL_URL);
            case LMStudio -> Objects.requireNonNullElse(
                PropertiesComponent.getInstance().getValue(LMSTUDIO_MODEL_URL),
                Constant.LMSTUDIO_MODEL_URL);
            case Ollama -> Objects.requireNonNullElse(
                PropertiesComponent.getInstance().getValue(OLLAMA_MODEL_URL),
                Constant.OLLAMA_MODEL_URL);
        };
    }
}
