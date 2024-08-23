package com.devoxx.genie.chatmodel;

import com.devoxx.genie.chatmodel.anthropic.AnthropicChatModelFactory;
import com.devoxx.genie.chatmodel.exo.ExoChatModelFactory;
import com.devoxx.genie.chatmodel.google.GoogleChatModelFactory;
import com.devoxx.genie.chatmodel.gpt4all.GPT4AllChatModelFactory;
import com.devoxx.genie.chatmodel.groq.GroqChatModelFactory;
import com.devoxx.genie.chatmodel.llama.LlamaChatModelFactory;
import com.devoxx.genie.chatmodel.lmstudio.LMStudioChatModelFactory;
import com.devoxx.genie.chatmodel.mistral.MistralChatModelFactory;
import com.devoxx.genie.chatmodel.ollama.OllamaChatModelFactory;
import com.devoxx.genie.chatmodel.openai.OpenAIChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.service.DevoxxGenieSettingsServiceProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.devoxx.genie.chatmodel.ChatModelFactory.TEST_MODEL;

@Setter
public class ChatModelProvider {

    private final Map<ModelProvider, ChatModelFactory> factories = new HashMap<>();
    private static final ModelProvider DEFAULT_PROVIDER = ModelProvider.OpenAI; // Choose an appropriate default

    public ChatModelProvider() {
        factories.put(ModelProvider.Ollama, new OllamaChatModelFactory());
        factories.put(ModelProvider.LMStudio, new LMStudioChatModelFactory());
        factories.put(ModelProvider.GPT4All, new GPT4AllChatModelFactory());
        factories.put(ModelProvider.OpenAI, new OpenAIChatModelFactory());
        factories.put(ModelProvider.Mistral, new MistralChatModelFactory());
        factories.put(ModelProvider.Anthropic, new AnthropicChatModelFactory());
        factories.put(ModelProvider.Groq, new GroqChatModelFactory());
        factories.put(ModelProvider.Google, new GoogleChatModelFactory());
        factories.put(ModelProvider.Exo, new ExoChatModelFactory());
        factories.put(ModelProvider.LLaMA, new LlamaChatModelFactory());

        // TODO Currently broken by latest Jan! version
        // factories.put(ModelProvider.Jan, new JanChatModelFactory());
    }

    public ChatLanguageModel getChatLanguageModel(@NotNull ChatMessageContext chatMessageContext) {
        ChatModel chatModel = initChatModel(chatMessageContext);
        return getFactory(chatMessageContext).createChatModel(chatModel);
    }

    public StreamingChatLanguageModel getStreamingChatLanguageModel(@NotNull ChatMessageContext chatMessageContext) {
        ChatModel chatModel = initChatModel(chatMessageContext);
        return getFactory(chatMessageContext).createStreamingChatModel(chatModel);
    }

    private @NotNull ChatModelFactory getFactory(@NotNull ChatMessageContext chatMessageContext) {
        ModelProvider provider = Optional.ofNullable(chatMessageContext.getLanguageModel())
            .map(LanguageModel::getProvider)
            .orElse(DEFAULT_PROVIDER);

        ChatModelFactory factory = factories.get(provider);
        if (factory == null) {
            throw new IllegalArgumentException("No factory for provider: " + provider);
        }
        return factory;
    }

    public @NotNull ChatModel initChatModel(@NotNull ChatMessageContext chatMessageContext) {
        ChatModel chatModel = new ChatModel();
        DevoxxGenieSettingsService stateService = DevoxxGenieSettingsServiceProvider.getInstance();
        setMaxOutputTokens(stateService, chatModel);

        chatModel.setTemperature(stateService.getTemperature());
        chatModel.setMaxRetries(stateService.getMaxRetries());
        chatModel.setTopP(stateService.getTopP());
        chatModel.setTimeout(stateService.getTimeout());

        LanguageModel languageModel = chatMessageContext.getLanguageModel();
        chatModel.setModelName(languageModel.getModelName() == null ? TEST_MODEL : languageModel.getModelName());

        setLocalBaseUrl(languageModel, chatModel, stateService);

        return chatModel;
    }

    private void setLocalBaseUrl(@NotNull LanguageModel languageModel,
                                 ChatModel chatModel,
                                 DevoxxGenieSettingsService stateService) {
        // Set base URL for local providers
        switch (languageModel.getProvider()) {
            case LMStudio:
                chatModel.setBaseUrl(stateService.getLmstudioModelUrl());
                break;
            case Ollama:
                chatModel.setBaseUrl(stateService.getOllamaModelUrl());
                break;
            case GPT4All:
                chatModel.setBaseUrl(stateService.getGpt4allModelUrl());
                break;
            case Exo:
                chatModel.setBaseUrl(stateService.getExoModelUrl());
                break;
            case LLaMA:
                chatModel.setBaseUrl(stateService.getLlamaCPPUrl());
                break;
            // Add other local providers as needed
        }
    }

    private static void setMaxOutputTokens(@NotNull DevoxxGenieSettingsService settingsState,
                                           @NotNull ChatModel chatModel) {
        Integer maxOutputTokens = settingsState.getMaxOutputTokens();
        chatModel.setMaxTokens(maxOutputTokens != null ? maxOutputTokens : Constant.MAX_OUTPUT_TOKENS);
    }
}
