package com.devoxx.genie;

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
import com.devoxx.genie.ui.util.CircularQueue;
import com.intellij.ide.util.PropertiesComponent;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.SystemMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.devoxx.genie.ui.Settings.*;

public class DevoxxGenieClient {

    private static final Logger log = LoggerFactory.getLogger(DevoxxGenieClient.class);

    public static final String YOU_ARE_A_SOFTWARE_DEVELOPER_WITH_EXPERT_KNOWLEDGE_IN =
        "You are a software developer with expert knowledge in ";
    public static final String PROGRAMMING_LANGUAGE = " programming language.";
    private ModelProvider modelProvider = getModelProvider(ModelProvider.Ollama.name());
    private String modelName;
    private CircularQueue<ChatMessage> chatMessages;

    private DevoxxGenieClient() {
        setChatMemorySize(SettingsState.getInstance().getMaxMemory());
    }

    private static final class InstanceHolder {
        private static final DevoxxGenieClient instance = new DevoxxGenieClient();
    }

    public static DevoxxGenieClient getInstance() {
        return InstanceHolder.instance;
    }

    protected ModelProvider getModelProvider(String defaultValue) {
        String value = PropertiesComponent.getInstance().getValue(MODEL_PROVIDER, defaultValue);
        return ModelProvider.valueOf(value);
    }

    public void setModelProvider(ModelProvider modelProvider) {
        this.modelProvider = modelProvider;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public void setChatMemorySize(int memorySize) {
        chatMessages = new CircularQueue<>(memorySize);
    }

    public int getChatMemorySize() {
        return chatMessages.size();
    }

    /**
     * Get the chat language model for selected model provider.
     * @return the chat language model
     */
    private ChatLanguageModel getChatLanguageModel() {
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
                OllamaModelEntryDTO[] models = new OllamaService().getModels();
                chatModel.setModelName(models[0].getName());
            } catch (IOException e) {
                log.error("Failed to get Ollama models", e);
            }
        } else {
            chatModel.setModelName(modelName);
        }
    }

    /**
     * Execute the user prompt
     * @param userPrompt the user prompt
     * @param selectedText the selected text
     * @return the prompt
     */
    public String executeGeniePrompt(String userPrompt,
                                     String language,
                                     String selectedText) {
        ChatLanguageModel chatLanguageModel = getChatLanguageModel();
        if (chatMessages.isEmpty()) {
            chatMessages.add(new SystemMessage(
                YOU_ARE_A_SOFTWARE_DEVELOPER_WITH_EXPERT_KNOWLEDGE_IN + language + PROGRAMMING_LANGUAGE +
                    "Always return the response in Markdown." +
                    "\n\nSelected code: " + selectedText));
        }

        chatMessages.add(new UserMessage(userPrompt));

        try {
            Response<AiMessage> generate = chatLanguageModel.generate(chatMessages.asList());
            String response = generate.content().text();
            chatMessages.add(new AiMessage(response));
            return response;
        } catch (Exception e) {
            return "Failed to execute Genie prompt!\n" + e.getMessage();
        }
    }

    /**
     * EXPERIMENTAL : Execute continue prompt
     * TODO : This is an experimental feature and may not work as expected.
     * @param selectedText the selected text
     * @return the prompt
     */
    public String executeGenieContinuePrompt(String selectedText) {
        ChatLanguageModel chatLanguageModel = getChatLanguageModel();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new dev.langchain4j.data.message.SystemMessage(
            YOU_ARE_A_SOFTWARE_DEVELOPER_WITH_EXPERT_KNOWLEDGE_IN + "JAVA" + PROGRAMMING_LANGUAGE +
                "\n\nSelected code: " + selectedText));
        messages.add(new UserMessage("Only return the code which finalises the code block."));
        Response<AiMessage> generate = chatLanguageModel.generate(messages);
        return generate.content().text();
    }
}
