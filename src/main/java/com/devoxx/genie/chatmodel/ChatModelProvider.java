package com.devoxx.genie.chatmodel;

import com.devoxx.genie.chatmodel.anthropic.AnthropicChatModelFactory;
import com.devoxx.genie.chatmodel.deepinfra.DeepInfraChatModelFactory;
import com.devoxx.genie.chatmodel.gpt4all.GPT4AllChatModelFactory;
import com.devoxx.genie.chatmodel.groq.GroqChatModelFactory;
import com.devoxx.genie.chatmodel.lmstudio.LMStudioChatModelFactory;
import com.devoxx.genie.chatmodel.mistral.MistralChatModelFactory;
import com.devoxx.genie.chatmodel.ollama.OllamaChatModelFactory;
import com.devoxx.genie.chatmodel.openai.OpenAIChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.OllamaService;
import com.devoxx.genie.ui.SettingsState;
import com.intellij.ide.util.PropertiesComponent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.Setter;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.devoxx.genie.ui.Settings.MODEL_PROVIDER;

@Setter
public class ChatModelProvider {

    private ModelProvider modelProvider = getModelProvider(ModelProvider.Ollama.name());
    private String modelName;
    private final OkHttpClient client;

    public ChatModelProvider() {
        this.client = new OkHttpClient();
    }

    protected ModelProvider getModelProvider(String defaultValue) {
        String value = PropertiesComponent.getInstance().getValue(MODEL_PROVIDER, defaultValue);
        return ModelProvider.valueOf(value);
    }

    /**
     * Get the chat language model for selected model provider.
     * @return the chat language model
     */
    public ChatLanguageModel getChatLanguageModel() {
        ChatModel chatModel = initChatModelSettings();
        SettingsState settings = SettingsState.getInstance();
        return switch (modelProvider) {
            case Ollama -> createOllamaModel(chatModel);
            case LMStudio -> createLmStudioModel(chatModel);
            case GPT4All -> createGPT4AllModel(chatModel);
            case OpenAI -> new OpenAIChatModelFactory(settings.getOpenAIKey(), modelName).createChatModel(chatModel);
            case Mistral -> new MistralChatModelFactory(settings.getMistralKey(), modelName).createChatModel(chatModel);
            case Anthropic -> new AnthropicChatModelFactory(settings.getAnthropicKey(), modelName).createChatModel(chatModel);
            case Groq -> new GroqChatModelFactory(settings.getGroqKey(), modelName).createChatModel(chatModel);
            case DeepInfra -> new DeepInfraChatModelFactory(settings.getDeepInfraKey(), modelName).createChatModel(chatModel);
        };
    }

    /**
     * Create GPT4All model.
     * @param chatModel the chat model
     * @return the chat language model
     */
    private static ChatLanguageModel createGPT4AllModel(ChatModel chatModel) {
        chatModel.setBaseUrl(SettingsState.getInstance().getGpt4allModelUrl());
        return new GPT4AllChatModelFactory().createChatModel(chatModel);
    }

    /**
     * Create LMStudio model.
     * @param chatModel the chat model
     * @return the chat language model
     */
    private static ChatLanguageModel createLmStudioModel(ChatModel chatModel) {
        chatModel.setBaseUrl(SettingsState.getInstance().getLmstudioModelUrl());
        return new LMStudioChatModelFactory().createChatModel(chatModel);
    }

    /**
     * Create Ollama model.
     * @param chatModel the chat model
     * @return the chat language model
     */
    private ChatLanguageModel createOllamaModel(ChatModel chatModel) {
        setLanguageModelName(chatModel);
        chatModel.setBaseUrl(SettingsState.getInstance().getOllamaModelUrl());
        return new OllamaChatModelFactory().createChatModel(chatModel);
    }

    /**
     * Initialize chat model settings by default or by user settings.
     * @return the chat model
     */
    private @NotNull ChatModel initChatModelSettings() {
        ChatModel chatModel = new ChatModel();
        chatModel.setTemperature(SettingsState.getInstance().getTemperature());
        chatModel.setMaxRetries(SettingsState.getInstance().getMaxRetries());
        chatModel.setTopP(SettingsState.getInstance().getTopP());
        chatModel.setTimeout(SettingsState.getInstance().getTimeout());
        return chatModel;
    }

    /**
     * Set the (default) language model name when none is selected.
     * @param chatModel the chat model
     */
    private void setLanguageModelName(ChatModel chatModel) {
        if (modelName == null) {
            try {
                OllamaModelEntryDTO[] models = new OllamaService(client).getModels();
                chatModel.setModelName(models[0].getName());
            } catch (IOException e) {
                System.err.println("Failed to get Ollama models : " + e.getMessage());
            }
        } else {
            chatModel.setModelName(modelName);
        }
    }
}
